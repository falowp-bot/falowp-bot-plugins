@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi

/**
 * NapCatGuildApiExpand
 */
class NapCatGuildApiExpand

/**
 * get_guild_list
 */
suspend fun NapCatBotApi.getGuildList(): NapCatRawData {
    return apiRequest("get_guild_list")
}

/**
 * get_guild_service_profile
 */
suspend fun NapCatBotApi.getGuildServiceProfile(): NapCatRawData {
    return apiRequest("get_guild_service_profile")
}
