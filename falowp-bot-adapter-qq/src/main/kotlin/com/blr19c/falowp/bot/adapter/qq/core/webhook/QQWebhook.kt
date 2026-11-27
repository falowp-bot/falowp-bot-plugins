package com.blr19c.falowp.bot.adapter.qq.core.webhook

import com.blr19c.falowp.bot.adapter.qq.core.config.QQConfig
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.web.WebServer
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tools.jackson.databind.JsonNode

/**
 * HTTP 接入层，仅负责验证、验签、ACK 和事件交付
 */
@Suppress("UastIncorrectHttpHeaderInspection") // QQ 平台自定义请求头
internal class QQWebhook(
    private val config: QQConfig,
    private val onEvent: (JsonNode) -> Unit,
) {
    private val signature = QQSignature(config.secret)

    /**
     * 向框架 WebServer 注册 QQ 回调路由
     */
    fun register() {
        WebServer.registerRoute { mount(this) }
    }

    /**
     * 挂载回调路由，验证身份后交付事件并返回 ACK
     */
    internal fun mount(route: Route) {
        with(route) {
            post(config.webhookPath) { handleCallback(call) }
        }
    }

    /**
     * 验证身份和签名后交付事件
     */
    private suspend fun handleCallback(call: ApplicationCall) {
        if (call.request.headers["X-Bot-Appid"] != config.appId) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }
        val body = call.receive<ByteArray>()
        val event = runCatching { Json.readJsonNode(body) }.getOrNull()
        if (event == null) {
            call.respond(HttpStatusCode.BadRequest)
            return
        }
        if (event.path("op").asInt(-1) == 13) {
            respondChallenge(call, event.path("d"))
            return
        }
        if (!verifySignature(call, body)) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }
        if (event.path("op").asInt(-1) == 0) onEvent(event)
        call.respondText("{\"op\":12}", ContentType.Application.Json)
    }

    /**
     * 首次地址验证不带签名头，返回 challenge 的签名
     */
    private suspend fun respondChallenge(call: ApplicationCall, data: JsonNode) {
        val token = data.path("plain_token").safeString()
        val timestamp = data.path("event_ts").safeString()
        if (token.isBlank() || timestamp.isBlank()) {
            call.respond(HttpStatusCode.BadRequest)
            return
        }
        val response = mapOf(
            "plain_token" to token,
            "signature" to signature.sign(timestamp, token.toByteArray(Charsets.UTF_8))
        )
        call.respondText(Json.toJsonString(response), ContentType.Application.Json)
    }

    private fun verifySignature(call: ApplicationCall, body: ByteArray): Boolean = signature.verify(
        call.request.headers["X-Signature-Timestamp"],
        call.request.headers["X-Signature-Ed25519"],
        body,
    )
}
