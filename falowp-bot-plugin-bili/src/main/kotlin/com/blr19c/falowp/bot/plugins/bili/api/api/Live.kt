package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.DatabaseCookiesStorage
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliLiveInfo
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliLiveSendMessageResult
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

suspend fun BiliClient.getLiveInfo(
    roomId: Long,
    url: String = ROOM_INFO
): BiliLiveInfo = Json.readObj(get(url) {
    parameter("room_id", roomId)
})

suspend fun BiliClient.sendLiveMessage(
    roomId: Long,
    message: String,
    color: Int = 16_777_215,
    fontSize: Int = 25,
    mode: Int = 1,
    url: String = LIVE_MESSAGE_SEND,
): BiliLiveSendMessageResult {
    val msg = message.trim()
    require(msg.isNotBlank()) { "直播间消息不能为空" }

    val csrf = DatabaseCookiesStorage.csrfToken()
        ?: throw IllegalStateException("未找到bili_jct，请先完成B站登录")
    val response = postJson(url) {
        headers {
            remove(HttpHeaders.Origin)
            append(HttpHeaders.Origin, LIVE_ORIGIN)
            remove(HttpHeaders.Referrer)
            append(HttpHeaders.Referrer, "$LIVE_ORIGIN/$roomId")
        }
        setBody(FormDataContent(Parameters.build {
            append("bubble", "0")
            append("msg", msg)
            append("color", color.toString())
            append("mode", mode.toString())
            append("fontsize", fontSize.toString())
            append("rnd", (System.currentTimeMillis() / 1000).toString())
            append("roomid", roomId.toString())
            append("csrf", csrf)
            append("csrf_token", csrf)
        }))
    }

    val code = response["code"].asInt()
    val responseMessage = response["message"].safeString()
    if (code != 0) {
        throw IllegalStateException("发送直播间消息失败[$code]:${responseMessage.ifBlank { "未知错误" }}")
    }
    return BiliLiveSendMessageResult(
        code = code,
        message = responseMessage,
        data = response["data"],
    )
}
