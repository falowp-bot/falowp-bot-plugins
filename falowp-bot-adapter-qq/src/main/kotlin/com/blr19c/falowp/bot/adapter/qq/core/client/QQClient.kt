package com.blr19c.falowp.bot.adapter.qq.core.client

import com.blr19c.falowp.bot.adapter.qq.core.config.QQConfig
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tools.jackson.databind.JsonNode

/**
 * 共享 REST 客户端，统一管理凭证和请求错误
 */
@Suppress("UastIncorrectHttpHeaderInspection") // QQ 平台自定义请求头
internal class QQClient(
    private val config: QQConfig,
    private val http: HttpClient = webclient(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var token = ""
    private var refreshAt = 0L

    /**
     * 获取 Authorization 请求头，必要时在互斥锁内刷新凭证
     */
    suspend fun authorization(): String = mutex.withLock {
        if (token.isBlank() || clock() >= refreshAt) {
            val startedAt = clock()
            val response = http.post(config.tokenUrl) {
                contentType(ContentType.Application.Json)
                setBody(Json.toJsonString(mapOf("appId" to config.appId, "clientSecret" to config.secret)))
            }
            val data = checked(response)
            val value = data.path("access_token").safeString()
            val seconds = data.path("expires_in").safeString().toLongOrNull() ?: 0
            check(value.isNotBlank() && seconds > 0) { "QQ access_token 响应缺少有效凭证或有效期" }
            token = value
            refreshAt = startedAt + (seconds - minOf(60, seconds / 2)).coerceAtLeast(1) * 1000
        }
        "QQBot $token"
    }

    /**
     * 调用 REST 接口并检查 HTTP 状态与业务错误码
     * @param body 待序列化的 JSON 请求体
     */
    suspend fun request(path: String, body: Any? = null, method: HttpMethod = HttpMethod.Post): JsonNode {
        return requestUrl(config.apiUrl + path, body, method)
    }

    /**
     * 查询 WebSocket 接入点
     */
    suspend fun gateway(): String = requestUrl(config.gatewayUrl, null, HttpMethod.Get)
        .path("url").safeString().also { check(it.startsWith("wss://")) { "QQ 网关地址无效" } }

    /**
     * 向预签名地址上传分片，不携带机器人凭证
     */
    suspend fun uploadPart(url: String, bytes: ByteArray) {
        val uri = java.net.URI(url)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) { "分片地址必须为 HTTPS" }
        val response = http.put(url) {
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }
        check(response.status.value in 200..299) { "QQ 分片上传失败，HTTP ${response.status.value}" }
    }

    private suspend fun requestUrl(url: String, body: Any?, method: HttpMethod): JsonNode {
        val auth = authorization()
        val response = http.request(url) {
            this.method = method
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, auth)
            body?.let { setBody(Json.toJsonString(it)) }
        }
        // 不自动重试发送请求，避免请求已成功但响应丢失时重复发送
        if (response.status == HttpStatusCode.Unauthorized) {
            mutex.withLock { if (auth == "QQBot $token") refreshAt = 0 }
        }
        return checked(response)
    }

    private suspend fun checked(response: HttpResponse): JsonNode {
        val text = response.bodyAsText()
        val data = runCatching { Json.readJsonNode(text.ifBlank { "{}" }) }.getOrElse {
            throw QQApiException(response.status.value, null, response.headers["X-Tps-trace-ID"], "响应不是 JSON")
        }
        val code = listOf("err_code", "code").mapNotNull {
            data.path(it).safeString().toLongOrNull()
        }.firstOrNull { it != 0L }
        if (response.status.value !in 200..299 || code != null) {
            val trace = data.path("trace_id").safeString().ifBlank { response.headers["X-Tps-trace-ID"] }
            throw QQApiException(
                response.status.value, code, trace,
                data.path("message").safeString().replace(config.secret, "[redacted]")
                    .let { if (token.isBlank()) it else it.replace(token, "[redacted]") })
        }
        return data
    }
}
