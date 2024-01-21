package com.blr19c.falowp.bot.bili.plugins.bili.api.api

import com.blr19c.falowp.bot.bili.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.bili.plugins.bili.api.data.GarbSuit
import io.ktor.client.request.*

suspend fun BiliClient.getGarbSuit(
    itemId: Long,
    url: String = SUIT_ITEMS
): GarbSuit = json(url) {
    parameter("item_id", itemId)
    parameter("part", "suit")
}