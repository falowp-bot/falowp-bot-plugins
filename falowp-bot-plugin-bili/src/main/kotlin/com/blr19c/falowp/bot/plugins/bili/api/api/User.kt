package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliUserInfo
import io.ktor.client.request.*

suspend fun BiliClient.getUserInfo(
    uid: Long,
    url: String = SPACE_INFO
): BiliUserInfo = json(url) {
    parameter("mid", uid)
    parameter("token", "")
    parameter("platform", "web")
    parameter("web_location", 1550101)
}