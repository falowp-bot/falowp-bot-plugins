package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliDynamicInfo
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliSpaceDynamicInfo
import com.blr19c.falowp.bot.system.json.Json
import io.ktor.client.request.*

suspend fun BiliClient.getDynamicInfo(
    dynamicId: Long,
    url: String = DYNAMIC_INFO
): BiliDynamicInfo = Json.readObj(get(url) {
    parameter("dynamic_id", dynamicId)
}, BiliDynamicInfo::class)

suspend fun BiliClient.spaceDynamicInfo(
    uid: Long,
    url: String = SPACE_DYNAMIC_INFO
): BiliSpaceDynamicInfo = Json.readObj(get(url) {
    parameter("offset", "")
    parameter("host_mid", uid)
    parameter("timezone_offset", -480)
    parameter("platform", "web")
    parameter("features", "itemOpusStyle,listOnlyfans")
}, BiliSpaceDynamicInfo::class)