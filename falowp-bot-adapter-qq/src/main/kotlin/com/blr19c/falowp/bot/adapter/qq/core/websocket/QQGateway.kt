package com.blr19c.falowp.bot.adapter.qq.core.websocket

import com.blr19c.falowp.bot.adapter.qq.core.client.QQClient
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotSelf
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import tools.jackson.databind.JsonNode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 管理 QQ 网关连接和会话恢复
 */
internal class QQGateway(
    private val client: QQClient,
    private val onEvent: (JsonNode) -> Unit,
    private val onSelf: (BotSelf) -> Unit,
    private val http: HttpClient = webclient(),
    private val interactions: Boolean = true,
) : Log {
    private var sessionId: String? = null
    private val sequence = AtomicReference<Long?>(null)
    private var registered = false
    private var failures = 0
    private var closeCode: Int? = null

    /**
     * 持续接收事件，首次 READY 时完成注册
     */
    suspend fun run(onReady: () -> Unit) {
        while (currentCoroutineContext().isActive) {
            closeCode = null
            try {
                connect(onReady)
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                log().warn("QQ 网关握手超时，将重新连接")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log().warn("QQ 网关连接异常: {}", e.javaClass.simpleName)
            }
            if (!canReconnect()) return
            val backoff = (1000L shl minOf(failures++, 5)) + Random.nextLong(500)
            log().info("QQ 网关将在 {}ms 后重新连接", backoff)
            delay(backoff.milliseconds)
        }
    }

    /**
     * 建立连接并记录服务端关闭码
     */
    private suspend fun connect(onReady: () -> Unit) {
        val url = client.gateway()
        val authorization = client.authorization()
        http.webSocket(url) {
            try {
                maintainSession(this, authorization, onReady)
            } finally {
                closeCode = withTimeoutOrNull(1.seconds) { closeReason.await()?.code?.toInt() }
            }
        }
    }

    /**
     * 将心跳任务限制在当前连接内
     */
    private suspend fun maintainSession(
        session: DefaultClientWebSocketSession,
        authorization: String,
        onReady: () -> Unit
    ) {
        val interval = readHeartbeatInterval(session)
        authenticate(session, authorization)
        val awaitingAck = AtomicBoolean(false)
        val heartbeat = startHeartbeat(session, interval, awaitingAck)
        try {
            receiveEvents(session, awaitingAck, onReady)
        } finally {
            heartbeat.cancelAndJoin()
        }
    }

    /**
     * 从 Hello 帧读取平台指定的心跳周期
     */
    private suspend fun readHeartbeatInterval(session: WebSocketSession): Long {
        val hello = withTimeout(15.seconds) { session.incoming.receive() } as? Frame.Text
            ?: error("QQ 网关未返回 Hello")
        val payload = Json.readJsonNode(hello.readText())
        check(payload.path("op").asInt(-1) == 10) { "QQ 网关首帧不是 Hello" }
        return payload.path("d").path("heartbeat_interval").asLong(0)
            .also { check(it > 0) { "QQ 心跳周期无效" } }
    }

    /**
     * 有可恢复的会话时发送 Resume，否则发送 Identify
     */
    private suspend fun authenticate(session: WebSocketSession, authorization: String) {
        val resumable = sessionId != null && sequence.get() != null
        val data = if (resumable) {
            mapOf("token" to authorization, "session_id" to sessionId, "seq" to sequence.get())
        } else {
            mapOf(
                "token" to authorization,
                "intents" to ((1 shl 25) or if (interactions) (1 shl 26) else 0),
                "shard" to listOf(0, 1)
            )
        }
        session.sendJson(mapOf("op" to if (resumable) 6 else 2, "d" to data))
    }

    /**
     * 心跳未收到 ACK 时关闭连接，交给外层重连
     */
    private fun startHeartbeat(session: WebSocketSession, interval: Long, awaitingAck: AtomicBoolean): Job =
        session.launch {
            while (isActive) {
                delay(interval.milliseconds)
                if (awaitingAck.getAndSet(true)) {
                    session.close(CloseReason(CloseReason.Codes.NORMAL, "heartbeat timeout"))
                    break
                }
                session.sendJson(mapOf("op" to 1, "d" to sequence.get()))
            }
        }

    /**
     * 处理网关控制帧和业务事件
     */
    private suspend fun receiveEvents(session: WebSocketSession, awaitingAck: AtomicBoolean, onReady: () -> Unit) {
        for (frame in session.incoming) {
            if (frame !is Frame.Text) continue
            val event = Json.readJsonNode(frame.readText())
            when (event.path("op").asInt(-1)) {
                0 -> dispatch(event, onReady)
                1 -> session.sendJson(mapOf("op" to 1, "d" to sequence.get()))
                7, 9 -> {
                    if (event.path("op").asInt() == 9) clearSession()
                    session.close()
                    return
                }

                11 -> awaitingAck.set(false)
            }
        }
    }

    /**
     * 事件交付后更新恢复会话所需的序号
     */
    private fun dispatch(event: JsonNode, onReady: () -> Unit) {
        when (event.path("t").safeString()) {
            "READY" -> acceptReady(event.path("d"), onReady)
            "RESUMED" -> failures = 0
            else -> try {
                onEvent(event)
            } catch (_: IllegalArgumentException) {
                log().warn("忽略格式异常的 QQ 事件: {}", event.path("t").safeString())
            }
        }
        event.path("s").safeString().toLongOrNull()?.let { sequence.set(it) }
    }

    /**
     * 保存会话和机器人身份，注册回调只执行一次
     */
    private fun acceptReady(data: JsonNode, onReady: () -> Unit) {
        sessionId = data.path("session_id").safeString()
            .also { check(it.isNotBlank()) { "QQ READY 缺少 session_id" } }
        val user = data.path("user")
        val id = user.path("id").safeString()
        if (id.isNotBlank()) onSelf(BotSelf.Default(id, user.path("username").safeString()))
        failures = 0
        if (!registered) {
            onReady()
            registered = true
        }
    }

    /**
     * 权限或封禁错误停止重试，会话失效则重新鉴权
     */
    private fun canReconnect(): Boolean {
        if (closeCode in setOf(4001, 4002, 4010, 4011, 4012, 4013, 4014, 4914, 4915)) {
            log().error("QQ 网关拒绝连接，停止重试，请检查平台权限及配置。关闭码: {}", closeCode)
            return false
        }
        if (closeCode == 4006 || closeCode == 4007 || closeCode in 4900..4913) clearSession()
        return true
    }

    private fun clearSession() {
        sessionId = null
        sequence.set(null)
    }

    private suspend fun WebSocketSession.sendJson(payload: Any) = send(Json.toJsonString(payload))
}
