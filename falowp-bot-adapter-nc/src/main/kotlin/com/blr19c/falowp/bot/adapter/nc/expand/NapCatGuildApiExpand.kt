@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode

/**
 * NapCatGuildApiExpand
 */
class NapCatGuildApiExpand {
}

/**
 * 获取频道列表
 *
 * 获取当前账号已加入的频道列表
 */
suspend fun NapCatBotApi.getGuildList(): JsonNode {
    return apiRequest("get_guild_list")
}

/**
 * 获取频道个人信息
 *
 * 获取当前账号在频道中的个人资料
 */
suspend fun NapCatBotApi.getGuildServiceProfile(): JsonNode {
    return apiRequest("get_guild_service_profile")
}
