package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.BiliLiveClient
import com.blr19c.falowp.bot.plugins.bili.api.DatabaseCookiesStorage
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliLiveStreamMessage
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliRoomInfo
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

/**
 * 批量查询用户的直播间状态
 */
suspend fun BiliClient.batchRoomInfo(
    uidList: List<String>,
    url: String = BATCH_ROOM_INFO
): List<BiliRoomInfo> {
    val response = postJson(url) {
        @Suppress("SpellCheckingInspection")
        setBody(mapOf("uids" to uidList))
    }
    val data = checkResponse(response, "获取房间信息失败")
    return Json.readObj<Map<String, BiliRoomInfo>>(data).values.toList()
}


/**
 * 连接直播信息流并持续接收消息
 *
 * 连接或解析异常会直接抛给调用方
 */
@Suppress("unused")
suspend fun BiliClient.liveDMInfo(
    roomId: String,
    onMessage: suspend (BiliLiveStreamMessage) -> Unit,
) {
    val dmInfo = wbiGet(LIVE_DM_INFO) {
        put("id", roomId)
    }
    val token = dmInfo.path("token").safeString()
    val hostInfo = dmInfo.path("host_list").asArray()[0]
    val host = hostInfo.path("host").safeString()
    val wssPort = hostInfo.path("wss_port").asInt()
    val uid = DatabaseCookiesStorage.uid() ?: throw IllegalStateException("未找到uid，请先完成B站登录")
    BiliLiveClient(getClient(), roomId.toLong(), uid.toLong(), token, host, wssPort).connect {
        onMessage(it)
    }
}

@Suppress("SpellCheckingInspection", "unused")
/**
 * 向直播间发送一条普通弹幕
 */
suspend fun BiliClient.sendLiveMessage(
    roomId: String,
    message: String,
    color: Int = 16_777_215,
    fontSize: Int = 25,
    mode: Int = 1,
    url: String = LIVE_MESSAGE_SEND,
) {
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
            append("roomid", roomId)
            append("csrf", csrf)
            append("csrf_token", csrf)
        }))
    }
    checkResponse(response, "发送直播间消息失败")
}
