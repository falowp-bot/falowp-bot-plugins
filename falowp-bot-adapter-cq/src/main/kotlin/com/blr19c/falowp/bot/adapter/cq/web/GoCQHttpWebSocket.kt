package com.blr19c.falowp.bot.adapter.cq.web

import com.blr19c.falowp.bot.adapter.cq.api.GoCQHttpBotApi
import com.blr19c.falowp.bot.adapter.cq.api.GoCQHttpEchoMessage
import com.blr19c.falowp.bot.adapter.cq.api.GoCQHttpMessage
import com.blr19c.falowp.bot.adapter.cq.api.GoCqHttpBotApiSupport
import com.blr19c.falowp.bot.adapter.cq.expand.avatar
import com.blr19c.falowp.bot.adapter.cq.expand.getMsg
import com.blr19c.falowp.bot.adapter.cq.expand.markMsgAsRead
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.BotSelf
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.listener.events.*
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.google.common.base.Strings
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class GoCQHttpWebSocket(onload: () -> Unit) : Log {

    fun configure() {
        embeddedServer(CIO, port = adapterConfigProperty("cq.port").toInt()) {
            config()
        }.start(wait = false)
    }

    class GoCqHttpWebSocketSession(session: WebSocketSession) : WebSocketSession by session

    private val webSocketSession = AtomicReference<GoCqHttpWebSocketSession>()
    private val echoCache = ConcurrentHashMap<Any, Channel<GoCQHttpEchoMessage>>()
    private val onload by lazy { onload() }
    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Suppress("UNUSED")
    fun webSocketSession(): GoCqHttpWebSocketSession {
        return webSocketSession.get()
    }

    private fun Application.config() {
        install(WebSockets)
        routing { webSocket("/ws") { initWebsocket() } }
    }

    private suspend fun DefaultWebSocketServerSession.initWebsocket() {
        webSocketSession.set(GoCqHttpWebSocketSession(this))
        onload
        for (frame in incoming) {
            executor.launch {
                try {
                    websocketFrame(frame)
                } catch (e: Throwable) {
                    log().error("GoCQHttp适配器处理消息失败", e)
                }
            }
        }
    }

    private suspend fun websocketFrame(frame: Frame) {
        if (frame !is Frame.Text) return
        val jsonNode = Json.readJsonNode(frame.readText())
        val postType = jsonNode.findPath("post_type").safeString()
        //心跳
        if (postType.isBlank() || postType == "meta_event") {
            return
        }
        //消息
        if (jsonNode.findPath("post_type").safeString().isNotBlank()) {
            processMessages(Json.readObj(frame.readText()))
        }
        //回执
        if (jsonNode.findPath("echo").safeString().isNotBlank()) {
            processEcho(Json.readObj(frame.readText()))
        }
    }

    private suspend fun processEcho(message: GoCQHttpEchoMessage) {
        log().info("GoCQHttp适配器接收到echo回执消息:{}", message)
        val channel = echoCache.remove(message.echo) ?: return
        channel.send(message)
        channel.close()
    }

    private suspend fun processMessages(goCQHttpMessage: GoCQHttpMessage) {
        log().info("GoCQHttp适配器接收到消息:{}", goCQHttpMessage)
        if (goCQHttpMessage.userId.isNullOrBlank()) {
            log().info("GoCQHttp适配器接收到消息没有userId不处理")
            return
        }
        //设置已读
        goCQHttpMessage.messageId?.let { GoCqHttpBotApiSupport.tempBot.markMsgAsRead(it) }
        if (preprocessingNoticeTypeEvents(goCQHttpMessage)) return
        if (preprocessingPostTypeEvents(goCQHttpMessage)) return
        PluginManagement.message(parseMessage(goCQHttpMessage), GoCQHttpBotApi::class)
    }

    private fun preprocessingPostTypeEvents(goCQHttpMessage: GoCQHttpMessage): Boolean {
        when (goCQHttpMessage.postType ?: return false) {
            "request" -> {
                when (goCQHttpMessage.requestType ?: return false) {
                    "group" -> {
                        parseEventBotApi(goCQHttpMessage).publishEvent(
                            RequestJoinGroupEvent(
                                parseSource(goCQHttpMessage),
                                ReceiveMessage.User.empty().copy(id = goCQHttpMessage.userId!!),
                                goCQHttpMessage.comment!!,
                                goCQHttpMessage.flag!!,
                                goCQHttpMessage.subType!!
                            )
                        )
                        return true
                    }

                    "friend" -> {
                        parseEventBotApi(goCQHttpMessage).publishEvent(
                            RequestAddFriendEvent(
                                parseSource(goCQHttpMessage),
                                ReceiveMessage.User.empty().copy(id = goCQHttpMessage.userId!!),
                                goCQHttpMessage.comment!!,
                                goCQHttpMessage.flag!!,
                            )
                        )
                        return true
                    }
                }
            }
        }
        return false
    }

    private suspend fun preprocessingNoticeTypeEvents(goCQHttpMessage: GoCQHttpMessage): Boolean {

        when (goCQHttpMessage.noticeType ?: return false) {
            "group_recall", "friend_recall" -> {
                val sender = parseSender(goCQHttpMessage)
                val cqMessage = goCQHttpMessage.messageId?.let { GoCqHttpBotApiSupport.tempBot.getMsg(it) }
                val message = cqMessage?.let { parseMessage(it) } ?: ReceiveMessage.empty()
                val source = ReceiveMessage.Source(goCQHttpMessage.groupId!!, SourceTypeEnum.GROUP)
                parseEventBotApi(goCQHttpMessage).publishEvent(WithdrawMessageEvent(source, sender, message))
                return true
            }

            "group_increase" -> {
                val sender = parseSender(goCQHttpMessage)
                val source = parseSource(goCQHttpMessage)
                parseEventBotApi(goCQHttpMessage).publishEvent(GroupIncreaseEvent(source, sender, sender, ""))
                return true
            }

            "group_decrease" -> {
                val sender = parseSender(goCQHttpMessage)
                val source = parseSource(goCQHttpMessage)
                parseEventBotApi(goCQHttpMessage).publishEvent(GroupDecreaseEvent(source, sender, sender, ""))
                return true
            }

            else -> return false
        }
    }

    private fun parseEventBotApi(goCQHttpMessage: GoCQHttpMessage): BotApi {
        val sender = parseSender(goCQHttpMessage)
        val source = parseSource(goCQHttpMessage)
        val self = BotSelf.Default(goCQHttpMessage.selfId!!)
        val message = ReceiveMessage.empty().copy(sender = sender, source = source, self = self)
        return GoCQHttpBotApi(message, this::class)
    }

    private fun parseMessage(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage {
        val content = parseMessageContent(goCQHttpMessage)
        val sender = parseSender(goCQHttpMessage)
        val source = parseSource(goCQHttpMessage)
        val self = BotSelf.Default(goCQHttpMessage.selfId!!)
        val messageId = goCQHttpMessage.messageId ?: UUID.randomUUID().toString()
        val messageType = goCQHttpMessage.toMessageType()
        val adapter = ReceiveMessage.Adapter("CQ", goCQHttpMessage)
        return ReceiveMessage(messageId, messageType, content, sender, source, self, adapter)
    }

    private fun parseSource(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage.Source {
        val userId = goCQHttpMessage.userId!!
        return ReceiveMessage.Source(goCQHttpMessage.groupId ?: userId, sourceTypeEnum(goCQHttpMessage))
    }

    private fun parseSender(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage.User {
        val userId = goCQHttpMessage.userId!!
        return ReceiveMessage.User(
            userId,
            Strings.emptyToNull(goCQHttpMessage.sender?.card) ?: goCQHttpMessage.sender?.nickname ?: "",
            GoCqHttpBotApiSupport.apiAuth(userId, goCQHttpMessage.sender?.role),
            GoCqHttpBotApiSupport.tempBot.avatar(userId)
        )
    }

    private fun parseMessageContent(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage.Content {
        return ReceiveMessage.Content(
            goCQHttpMessage.content,
            goCQHttpMessage.voice.orElse(null),
            goCQHttpMessage.atList,
            goCQHttpMessage.imageList,
            emptyList(),
            goCQHttpMessage.video.orElse(null),
            goCQHttpMessage.shareList,
            emptyList()
        ) {
            val cqMessage = goCQHttpMessage.message ?: return@Content null
            //处理引用
            val referenceRegex = Regex("\\[CQ:reply,id=(\\d+)]")
            val referenceSingle = referenceRegex.findAll(cqMessage).map { it.groupValues[1] }.singleOrNull()
            referenceSingle(referenceSingle, goCQHttpMessage)
        }
    }

    private suspend fun referenceSingle(referenceSingle: String?, goCQHttpMessage: GoCQHttpMessage): ReceiveMessage? {
        referenceSingle ?: return null
        val originalMessage = GoCqHttpBotApiSupport.tempBot.getMsg(referenceSingle)
        val finalMessage = originalMessage.copy(
            selfId = goCQHttpMessage.selfId,
            userId = originalMessage.sender?.userId
        )
        return parseMessage(finalMessage)
    }


    private fun sourceTypeEnum(goCQHttpMessage: GoCQHttpMessage): SourceTypeEnum {
        return if (goCQHttpMessage.messageType == "group" || !goCQHttpMessage.groupId.isNullOrBlank())
            SourceTypeEnum.GROUP
        else SourceTypeEnum.PRIVATE
    }
}