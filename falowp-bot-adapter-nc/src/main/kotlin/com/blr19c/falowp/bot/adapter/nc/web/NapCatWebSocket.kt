package com.blr19c.falowp.bot.adapter.nc.web

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.expand.NapCatWsEcho
import com.blr19c.falowp.bot.adapter.nc.expand.markGroupMsgAsRead
import com.blr19c.falowp.bot.adapter.nc.expand.markPrivateMsgAsRead
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatPostType
import com.blr19c.falowp.bot.adapter.nc.message.expand.toBotMessage
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import tools.jackson.databind.JsonNode
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * NapCat WebSocket连接
 */
object NapCatWebSocket : Log {


    @Volatile
    private var webSocketSession: WebSocketSession? = null

    private val executor = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val echoWaiters = ConcurrentHashMap<String, CompletableDeferred<JsonNode>>()

    /**
     * 加载配置
     */
    fun configure(onload: () -> Unit) = executor.launch {
        while (isActive) {
            try {
                napCatWsClient().ws(adapterConfigProperty("nc.wsAddress")) {
                    try {
                        log().info("NapCat-WebSocket-连接成功")
                        initWebsocket(onload)
                        for (frame in incoming) {
                            launch { execWebsocketFrame(frame) }
                        }
                    } finally {
                        val reason = closeReason.await()
                        log().warn("NapCat-WebSocket-断开连接,closeReason=$reason")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log().info("NapCat-WebSocket-断开连接", e)
            }
            val wsRetryInterval = adapterConfigProperty("nc.wsRetryInterval") { "5000" }
            log().info("等待${wsRetryInterval}ms后重试")
            delay(wsRetryInterval.toLong().milliseconds)
        }
    }

    /**
     * 获取连接
     */
    fun webSocketSession(): WebSocketSession {
        return webSocketSession ?: throw IllegalStateException("NapCat-WebSocket-无可用webSocketSession")
    }

    /**
     * 请求并等待结果
     */
    suspend fun sendAndWaitEcho(napCatWsEcho: NapCatWsEcho, timeout: Duration): JsonNode {
        val deferred = CompletableDeferred<JsonNode>()
        echoWaiters[napCatWsEcho.echo] = deferred
        try {
            webSocketSession().sendLargeText(Json.toJsonString(napCatWsEcho))
            return withTimeout(timeout) {
                deferred.await()
            }
        } finally {
            echoWaiters.remove(napCatWsEcho.echo)
        }
    }

    /**
     * 初始化 WebSocket 服务
     */
    private fun WebSocketSession.initWebsocket(onload: () -> Unit) {
        webSocketSession = this
        onload()
    }

    /**
     * 处理 WebSocket 信息
     */
    private suspend fun execWebsocketFrame(frame: Frame) {
        try {
            val originalMessage = when (frame) {
                is Frame.Text -> Json.readJsonNode(frame.readText())
                is Frame.Binary -> Json.readJsonNode(frame.readBytes())
                else -> return
            }
            if (processEcho(originalMessage)) return
            if (processEvent(originalMessage)) return
            if (processMessages(originalMessage)) return
        } catch (e: Exception) {
            log().error("NapCat-WebSocket-处理消息失败", e)
        }
    }

    /**
     * 处理echo
     */
    private fun processEcho(originalMessage: JsonNode): Boolean {
        val echo = originalMessage.path("echo").safeString()
        if (echo.isBlank()) return false
        val deferred = echoWaiters.remove(echo) ?: return true
        log().debug("NapCat-WebSocket-接收到回执:{}", originalMessage)
        deferred.complete(originalMessage)
        return true
    }

    /**
     * 处理事件
     */
    private suspend fun processEvent(originalMessage: JsonNode): Boolean {
        val postType = NapCatPostType.fromValue(originalMessage.path("post_type").safeString())
        if (NapCatPostType.NOTICE == postType) {
            NapCatNotice.processNotice(originalMessage)
            return true
        }
        if (NapCatPostType.REQUEST == postType) {
            NapCatNotice.processEvent(originalMessage)
            return true
        }
        return false
    }

    /**
     * 处理消息
     */
    private suspend fun processMessages(originalMessage: JsonNode): Boolean {
        val postType = NapCatPostType.fromValue(originalMessage.path("post_type").safeString())
        if (postType.negligible()) return false
        log().info("NapCat-WebSocket-接收到消息:{}", originalMessage)
        val receiveMessage = Json.convertValue<NapCatMessage>(originalMessage).toBotMessage()
        if (receiveMessage.private()) {
            NapCatBotApiSupport.tempBot.markPrivateMsgAsRead(receiveMessage.id)
        }
        if (receiveMessage.group()) {
            NapCatBotApiSupport.tempBot.markGroupMsgAsRead(receiveMessage.id)
        }
        PluginManagement.message(receiveMessage, NapCatBotApi::class)
        return true
    }

    private suspend fun WebSocketSession.sendLargeText(text: String, chunkSize: Int = 32_000) {
        var i = 0
        log().warn("send json length=${text.length}")

        while (i < text.length) {
            val end = minOf(i + chunkSize, text.length)
            val content = Frame.Text(
                fin = end == text.length,
                data = text.substring(i, end).toByteArray()
            )
            this.send(content)
            i = end
        }
    }

    fun napCatWsClient(): HttpClient {
        return webclient().config {
            install(WebSockets) {
                pingInterval = 3.seconds
                maxFrameSize = 16L * 1024 * 1024
            }
        }
    }
}
