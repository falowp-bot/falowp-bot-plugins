package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeAccount
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MemoryMessageHistory
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MessageHistory
import com.blr19c.falowp.bot.adapter.qqnative.core.history.NativeEventInbox
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessage
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.QQNativeNotice
import com.blr19c.falowp.bot.adapter.qqnative.core.transport.WorkerCall
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode

/**
 * 管理单个登录会话的 Kotlin 运行时；Electron 仅负责传递原生调用和回调
 */
internal class NativeRuntime(
    call: WorkerCall,
    private val account: JsonNode,
    onMessage: (QQNativeMessage) -> Unit,
    onNotice: (QQNativeNotice) -> Unit,
    history: MessageHistory = MemoryMessageHistory(),
    private val inbox: NativeEventInbox = NativeEventInbox(),
    startImmediately: Boolean = true
) : AutoCloseable {
    private val bridge = NativeBridge(call)
    private val contacts = NativeContacts(bridge)
    private val extensions = NativeExtensions(bridge, contacts, account, history)
    private val media = NativeMedia(bridge, account.path("paths"))
    private val delivery = NativeDelivery(bridge, account)
    private val messages = NativeMessages(bridge, account, contacts, media, delivery, history)
    private val events = NativeEvents(
        account, contacts, messages, history,
        { onMessage(it.message()) }, { data -> data.notice()?.let(onNotice) })

    @Volatile
    private var closed = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val incoming = Channel<NativeEventInbox.Entry>(Channel.UNLIMITED)
    private val log = LoggerFactory.getLogger(javaClass)
    private val processing: Job

    init {
        inbox.pending().forEach { check(incoming.trySend(it).isSuccess) }
        processing = scope.launch(start = CoroutineStart.LAZY) {
            for (entry in incoming) {
                val name = entry.event.s("name")
                val args = entry.event.list("arguments")
                try {
                    val successful = when (name) {
                        "onRecvMsg", "onMsgInfoListUpdate" -> args.firstOrNull()?.toList().orEmpty()
                            .fold(true) { successful, row ->
                                val received =
                                    try {
                                        events.receive(row, name == "onMsgInfoListUpdate")
                                        true
                                    } catch (error: Exception) {
                                        if (error is CancellationException) throw error
                                        log.error("QQ 原生消息处理失败，继续处理同批其他消息", error)
                                        false
                                    }
                                successful && received
                            }

                        "onRecvSysMsg" -> {
                            args.firstOrNull()?.let { data ->
                                events.system(if (data.isArray) data.toList().map { it.asInt().toByte() }
                                    .toByteArray() else data.binary())
                            }
                            true
                        }

                        "onGroupNotifiesUpdated" -> {
                            args.getOrNull(1)?.forEach { events.request("groupRequest", it) }
                            true
                        }

                        "onBuddyReqChange" -> {
                            args.firstOrNull()?.list("buddyReqs")?.forEach { events.request("friendRequest", it) }
                            true
                        }

                        else -> {
                            events.callback(
                                name,
                                args,
                                "callback/" + (entry.path?.fileName?.toString() ?: System.nanoTime().toString())
                            )
                            true
                        }
                    }
                    if (successful) inbox.acknowledge(entry)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    log.warn("QQ 原生事件解析失败：{}", name, error)
                }
            }
        }
        if (startImmediately) start()
    }

    fun start() {
        processing.start()
    }

    fun event(event: JsonNode) {
        val name = event.s("name")
        val args = event.list("arguments")
        // 先持久化完整回调，再完成发送回执或写入内存队列，防止进程退出时丢失原文
        val entry = if (name in listOf(
                "onRecvMsg",
                "onMsgInfoListUpdate",
                "onRecvSysMsg",
                "onGroupNotifiesUpdated",
                "onBuddyReqChange",
                "onInputStatusPush",
                "onMemberInfoChange"
            )
        ) inbox.append(event) else null
        extensions.callbacks.event(event)
        // 优先处理操作完成回调，避免被消息解码或媒体下载阻塞
        when (name) {
            "onMsgInfoListUpdate" -> delivery.update(args.firstOrNull()?.toList().orEmpty())
            "onRichMediaDownloadComplete" -> args.firstOrNull()?.let { media.complete(it) }
            "onGroupListUpdate" -> contacts.groupUpdate(
                args.firstOrNull()?.asInt() ?: -1,
                args.getOrNull(1)?.toList().orEmpty()
            )

            "onMemberInfoChange" -> if (args.size >= 3) contacts.memberUpdate(args[0].asString(), args[2])
        }
        if (entry != null) check(incoming.trySend(entry).isSuccess) { "QQ 消息队列已关闭，未处理回调保留在本地收件箱" }
    }

    /**
     * 绑定当前会话，账号切换或会话关闭后拒绝继续调用
     */
    fun account(nickname: String, checkActive: () -> Unit = {}): QQNativeAccount {
        val operations = NativeAccount(contacts, messages, delivery, extensions) {
            check(!closed) { "QQ 会话已关闭" }
            checkActive()
        }
        return QQNativeAccount(account.s("id"), operations, operations, nickname, operations::expand)
    }

    override fun close() {
        closed = true
        incoming.close()
        scope.cancel()
    }
}
