package com.blr19c.falowp.bot.bili.plugins.bili.api.api

import com.blr19c.falowp.bot.bili.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.bili.plugins.bili.api.data.BiliSearchResult
import com.blr19c.falowp.bot.bili.plugins.bili.api.data.BiliVideoAiSummary
import com.blr19c.falowp.bot.bili.plugins.bili.api.data.BiliVideoInfo
import io.ktor.client.request.*

suspend fun BiliClient.getVideoInfo(
    aid: Long,
    url: String = VIDEO_INFO
): BiliVideoInfo = json(url) {
    parameter("aid", aid)
}

suspend fun BiliClient.getVideoInfo(
    bvid: String,
    url: String = VIDEO_INFO
): BiliVideoInfo = json(url) {
    parameter("bvid", bvid)
}

suspend fun BiliClient.getVideos(
    uid: Long,
    keyword: String = "",
    pageSize: Int = 30,
    pageNum: Int = 1,
    url: String = VIDEO_USER
): BiliSearchResult = json(url) {
    parameter("mid", uid)
    parameter("keyword", keyword)
    parameter("order", "pubdate")
    parameter("jsonp", "jsonp")
    parameter("ps", pageSize)
    parameter("pn", pageNum)
    parameter("tid", 0)
}

/**
 * 获取视频的ai总结
 */
suspend fun BiliClient.getVideoAiSummary(
    bvid: String,
    url: String = VIDEO_AI_SUMMARY
): BiliVideoAiSummary {
    val videoInfo = this.getVideoInfo(bvid)
    return json(url) {
        parameter("bvid", bvid)
        parameter("cid", videoInfo.cid)
        parameter("up_mid", videoInfo.mid)
        parameter("web_location", 333.788)
    }
}