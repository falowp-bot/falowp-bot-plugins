package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.web.NapCatWebSocket
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.util.*
import kotlin.time.Duration.Companion.minutes

/**
 * NapCat 扩展API
 */
@Suppress("unused")
class NapCatExpandApi

data class NapCatApiResult<T>(
    /**
     * 状态
     */
    @field:JsonProperty("status")
    val status: String,
    /**
     * 状态码
     */
    @field:JsonProperty("retcode")
    val retCode: Int,
    /**
     * 错误消息
     */
    @field:JsonProperty("msg")
    val msg: String?,
    /**
     * 对错误的详细解释
     */
    @field:JsonProperty("wording")
    val wording: String?,
    /**
     * 数据
     */
    @field:JsonProperty("data")
    val data: T?,
    /**
     * 回执
     */
    @field:JsonProperty("echo")
    val echo: Any?,
) {
    /**
     * 是否成功
     */
    fun success(): Boolean {
        return status == "ok" || status == "async"
    }
}

/**
 * NapCat WS
 */
data class NapCatWsEcho(
    /**
     * 动作
     */
    @field:JsonProperty("action")
    val action: String,
    /**
     * 参数
     */
    @field:JsonProperty("params")
    val params: Any?,
    /**
     * echo
     */
    @field:JsonProperty("echo")
    val echo: String,
)

private suspend inline fun <reified T : Any> NapCatBotApi.apiRequestResult(
    type: String,
    body: Any? = null
): NapCatApiResult<T> {
    return if (adapterConfigProperty("nc.useHttp") { false.toString() }.toBoolean()) {
        val baseUrl = adapterConfigProperty("nc.httpAddress")
        val resBytes = longTimeoutWebclient().post("$baseUrl/$type") { setBody(body) }.bodyAsBytes()
        Json.readObj<NapCatApiResult<T>>(resBytes)
    } else {
        val echo = UUID.randomUUID().toString()
        val node = NapCatWebSocket.sendAndWaitEcho(NapCatWsEcho(type, body, echo), 1.minutes)
        Json.convertValue<NapCatApiResult<T>>(node)
    }
}

internal suspend inline fun <reified T : Any> NapCatBotApi.apiRequest(type: String, body: Any? = null): T {
    val result = apiRequestResult<T>(type, body)
    if (result.success()) return result.data ?: throw IllegalStateException("NapCat-API-请求失败-无返回结果")
    throw IllegalStateException("NapCat-API-请求失败:${result.msg ?: ""}${result.wording ?: ""}")
}

internal suspend fun NapCatBotApi.apiRequestUnit(type: String, body: Any? = null) {
    val result = apiRequestResult<Unit>(type, body)
    if (result.success()) return
    throw IllegalStateException("NapCat-API-请求失败:${result.msg ?: ""}${result.wording ?: ""}")
}


