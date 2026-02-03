@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode

/**
 * NapCatGuildApiExpand 频道API
 */
class NapCatGuildApiExpand

/**
 * 获取频道列表
 */
suspend fun NapCatBotApi.getGuildList(): JsonNode {
    return apiRequest("get_guild_list")
}

/**
 * 获取频道个人信息
 */
suspend fun NapCatBotApi.getGuildServiceProfile(): JsonNode {
    return apiRequest("get_guild_service_profile")
}
