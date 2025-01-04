package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliLiveInfo
import com.blr19c.falowp.bot.system.json.Json
import io.ktor.client.request.*

suspend fun BiliClient.getLiveInfo(
    roomId: Long,
    url: String = ROOM_INFO
): BiliLiveInfo = Json.readObj(get(url) {
    parameter("room_id", roomId)
})