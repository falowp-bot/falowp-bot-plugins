package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliDynamicInfo
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliDynamicList
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliSpaceDynamicInfo
import io.ktor.client.request.*

suspend fun BiliClient.getSpaceHistory(
    uid: Long,
    url: String = DYNAMIC_HISTORY
): BiliDynamicList = json(url) {
    parameter("visitor_uid", uid)
    parameter("host_uid", uid)
    parameter("offset_dynamic_id", 0)
    parameter("need_top", 0)
}

suspend fun BiliClient.getDynamicInfo(
    dynamicId: Long,
    url: String = DYNAMIC_INFO
): BiliDynamicInfo = json(url) {
    parameter("dynamic_id", dynamicId)
}

suspend fun BiliClient.spaceDynamicInfo(
    uid: Long,
    url: String = SPACE_DYNAMIC_INFO
): BiliSpaceDynamicInfo = json(url) {
    parameter("offset", "")
    parameter("host_mid", uid)
    parameter("timezone_offset", -480)
    parameter("platform", "web")
    parameter("features", "itemOpusStyle,listOnlyfans")
}