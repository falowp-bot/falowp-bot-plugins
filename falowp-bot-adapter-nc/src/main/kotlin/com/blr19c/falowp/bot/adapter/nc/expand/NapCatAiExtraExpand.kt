@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi

/**
 * NapCatAiExtraExpand
 */
class NapCatAiExtraExpand

/**
 * 获取AI语音
 */
suspend fun NapCatBotApi.getAiRecord(groupId: Long, character: String, text: String): String {
    return apiRequest("get_ai_record", mapOf("group_id" to groupId, "character" to character, "text" to text))
}

/**
 * 发送群AI语音
 */
suspend fun NapCatBotApi.sendGroupAiRecord(groupId: Long, character: String, text: String) {
    apiRequestUnit("send_group_ai_record", mapOf("group_id" to groupId, "character" to character, "text" to text))
}
