package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.EmoteBusiness
import com.blr19c.falowp.bot.plugins.bili.api.data.EmotePackage
import com.blr19c.falowp.bot.plugins.bili.api.data.EmotePanel
import io.ktor.client.request.*

suspend fun BiliClient.getEmotePanel(
    business: EmoteBusiness = EmoteBusiness.dynamic,
    url: String = EMOTE_PANEL
): EmotePanel = json(url) {
    parameter("business", business)
}

suspend fun BiliClient.getEmotePackage(
    vararg id: Long,
    business: EmoteBusiness = EmoteBusiness.dynamic,
    url: String = EMOTE_PACKAGE
): EmotePackage = json(url) {
    parameter("ids", id.joinToString(","))
    parameter("business", business)
}