@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliDynamicInfo
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliSpaceDynamicInfo
import com.blr19c.falowp.bot.system.json.Json
import io.ktor.client.request.*

/**
 * 按动态 ID 获取旧版动态详情
 */
suspend fun BiliClient.getDynamicInfo(
    dynamicId: Long,
    url: String = DYNAMIC_INFO
): BiliDynamicInfo = Json.readObj(get(url) {
    parameter("dynamic_id", dynamicId)
})

/**
 * 获取用户空间最新一页动态
 */
suspend fun BiliClient.spaceDynamicInfo(
    uid: Long,
    url: String = SPACE_DYNAMIC_INFO
): BiliSpaceDynamicInfo = Json.readObj(get(url) {
    parameter("offset", "")
    parameter("host_mid", uid)
    parameter("timezone_offset", -480)
    parameter("platform", "web")
    parameter("features", "itemOpusStyle,listOnlyfans")
})