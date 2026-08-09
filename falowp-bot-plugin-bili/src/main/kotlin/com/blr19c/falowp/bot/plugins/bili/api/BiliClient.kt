package com.blr19c.falowp.bot.plugins.bili.api

import com.blr19c.falowp.bot.plugins.bili.api.WBI.wbiParams
import com.blr19c.falowp.bot.plugins.bili.api.api.ORIGIN
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.web.bodyAsJsonNode
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import tools.jackson.databind.JsonNode

@Suppress("UNUSED")
/**
 * 访问 B 站接口用的客户端
 */
class BiliClient : Log {

    /**
     * 登录 Cookie 会落到数据库里
     */
    private val storage = DatabaseCookiesStorage

    private val client = longTimeoutWebclient().config {
        defaultRequest {
            header(HttpHeaders.Origin, ORIGIN)
            header(HttpHeaders.Referrer, ORIGIN)
            header(HttpHeaders.AcceptLanguage, "en,zh-CN;q=0.9,zh;q=0.8")
        }
        install(HttpCookies) {
            storage = this@BiliClient.storage
        }
        expectSuccess = true
    }

    /**
     * 发起 GET 请求并取出 data
     */
    suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit = {}): JsonNode {
        return client.get(url) { block() }.bodyAsJsonNode()["data"]
    }

    /**
     * 发起 GET 请求并返回原始文本
     */
    suspend fun getText(url: String, block: HttpRequestBuilder.() -> Unit = {}): String {
        return client.get(url) { block() }.bodyAsText()
    }

    /**
     * 发起 POST 请求并取出 data
     */
    suspend fun post(url: String, block: HttpRequestBuilder.() -> Unit = {}): JsonNode {
        return client.post(url) { block() }.bodyAsJsonNode()["data"]
    }

    /**
     * 发起 POST 请求并保留完整响应
     */
    suspend fun postJson(url: String, block: HttpRequestBuilder.() -> Unit = {}): JsonNode {
        return client.post(url) { block() }.bodyAsJsonNode()
    }

    /**
     * 签好 WBI 参数再发起 GET 请求
     */
    suspend fun wbiGet(url: String, block: MutableMap<String, String>.() -> Unit = {}): JsonNode {
        val build = mutableMapOf<String, String>()
        build.block()
        val params = wbiParams(build)
        return client.get(url) {
            params.forEach { (k, v) -> parameter(k, v) }
        }.bodyAsJsonNode()["data"]
    }

    /**
     * 检查接口状态并取出 data
     */
    fun checkResponse(response: JsonNode, message: String): JsonNode {
        val code = response["code"].asInt()
        val responseMessage = response["message"].safeString()
        if (code != 0) {
            throw IllegalStateException("$message[$code]:${responseMessage.ifBlank { "未知错误" }}")
        }
        return response.path("data")
    }

    /**
     * 需要 WebSocket 等能力时使用底层客户端
     */
    fun getClient() = client
}
