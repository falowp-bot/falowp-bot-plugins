package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliUserInfo
import com.blr19c.falowp.bot.system.json.Json

/**
 * 获取用户空间里的基本资料和直播间状态
 */
suspend fun BiliClient.getUserInfo(
    uid: Long,
    url: String = SPACE_INFO
): BiliUserInfo = Json.readObj(wbiGet(url) {
    put("mid", uid.toString())
    put("token", "")
    put("platform", "web")
})
