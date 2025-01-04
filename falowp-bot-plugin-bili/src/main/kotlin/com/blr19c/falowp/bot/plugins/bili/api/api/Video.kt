package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliVideoAiSummary
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliVideoInfo
import com.blr19c.falowp.bot.system.json.Json
import io.ktor.client.request.*

suspend fun BiliClient.getVideoInfo(
    bvid: String,
    url: String = VIDEO_INFO
): BiliVideoInfo = Json.readObj(get(url) {
    parameter("bvid", bvid)
})

/**
 * 获取视频的ai总结
 */
suspend fun BiliClient.getVideoAiSummary(
    bvid: String,
    url: String = VIDEO_AI_SUMMARY
): BiliVideoAiSummary {
    val videoInfo = this.getVideoInfo(bvid)
    return Json.readObj(wbiGet(url) {
        put("bvid", bvid)
        put("cid", videoInfo.cid)
        put("up_mid", videoInfo.owner.mid)
        put("web_location", 333.788.toString())
    })
}