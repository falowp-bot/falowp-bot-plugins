package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliArticleList
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliArticleView
import io.ktor.client.request.*

suspend fun BiliClient.getArticleList(
    cid: Long,
    url: String = ARTICLE_LIST_INFO
): BiliArticleList = json(url) {
    parameter("id", cid)
}

suspend fun BiliClient.getArticleView(
    cid: Long,
    url: String = ARTICLE_VIEW_INFO
): BiliArticleView = json<BiliArticleView>(url) {
    parameter("id", cid)
}.copy(id = cid)