package com.blr19c.falowp.bot.adapter.cq.web

import com.blr19c.falowp.bot.adapter.cq.api.GoCQHttpBotApi
import com.blr19c.falowp.bot.adapter.cq.api.GoCQHttpEchoMessage
import com.blr19c.falowp.bot.adapter.cq.api.GoCQHttpMessage
import com.blr19c.falowp.bot.adapter.cq.api.GoCqHttpBotApiSupport
import com.blr19c.falowp.bot.adapter.cq.expand.cqAvatar
import com.blr19c.falowp.bot.adapter.cq.expand.cqGetMsg
import com.blr19c.falowp.bot.adapter.cq.expand.cqMarkMsgAsRead
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.listener.events.*
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.google.common.base.Strings
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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
        embeddedServer(Netty, port = adapterConfigProperty("cq.port").toInt()) {
            config()
        }.start(wait = false)
    }

    class GoCqHttpWebSocketSession(session: WebSocketSession) : WebSocketSession by session

    private val webSocketSession = AtomicReference<GoCqHttpWebSocketSession>()
    private val echoCache = ConcurrentHashMap<Any, Channel<GoCQHttpEchoMessage>>()
    private val onload by lazy { onload() }
    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
        val postType = jsonNode.findPath("post_type").asText()
        //心跳
        if (postType.isNullOrBlank() || postType == "meta_event") {
            return
        }
        //消息
        if (jsonNode.findPath("post_type").asText().isNotBlank()) {
            processMessages(Json.readObj(frame.readText()))
        }
        //回执
        if (jsonNode.findPath("echo").asText().isNotBlank()) {
            processEcho(Json.readObj(frame.readText()));
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
        goCQHttpMessage.messageId?.let { GoCqHttpBotApiSupport.tempBot.cqMarkMsgAsRead(it) }
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
                                goCQHttpMessage.userId!!,
                                parseSource(goCQHttpMessage),
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
                                goCQHttpMessage.userId!!,
                                parseSource(goCQHttpMessage),
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
                val cqMessage = goCQHttpMessage.messageId?.let { GoCqHttpBotApiSupport.tempBot.cqGetMsg(it) }
                val message = cqMessage?.let { parseMessage(it) } ?: ReceiveMessage.empty()
                parseEventBotApi(goCQHttpMessage).publishEvent(WithdrawMessageEvent(message, sender))
                return true
            }

            "group_increase" -> {
                val sender = parseSender(goCQHttpMessage)
                val source = parseSource(goCQHttpMessage)
                parseEventBotApi(goCQHttpMessage).publishEvent(GroupIncreaseEvent(sender, source))
                return true
            }

            "group_decrease" -> {
                val sender = parseSender(goCQHttpMessage)
                val source = parseSource(goCQHttpMessage)
                parseEventBotApi(goCQHttpMessage).publishEvent(GroupDecreaseEvent(sender, source))
                return true
            }

            else -> return false
        }
    }

    private fun parseEventBotApi(goCQHttpMessage: GoCQHttpMessage): BotApi {
        val sender = parseSender(goCQHttpMessage)
        val source = parseSource(goCQHttpMessage)
        val self = ReceiveMessage.Self(goCQHttpMessage.selfId!!)
        val message = ReceiveMessage.empty().copy(sender = sender, source = source, self = self)
        return GoCQHttpBotApi(message, this::class)
    }

    private fun parseMessage(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage {
        val content = parseMessageContent(goCQHttpMessage)
        val sender = parseSender(goCQHttpMessage)
        val source = parseSource(goCQHttpMessage)
        val self = ReceiveMessage.Self(goCQHttpMessage.selfId!!)
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
            GoCqHttpBotApiSupport.tempBot.cqAvatar(userId)
        )
    }

    private fun parseMessageContent(goCQHttpMessage: GoCQHttpMessage): ReceiveMessage.Content {
        return ReceiveMessage.Content(
            goCQHttpMessage.content,
            goCQHttpMessage.voice.orElse(null),
            goCQHttpMessage.atList,
            goCQHttpMessage.imageList,
            goCQHttpMessage.video.orElse(null),
            goCQHttpMessage.shareList
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
        val originalMessage = GoCqHttpBotApiSupport.tempBot.cqGetMsg(referenceSingle)
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