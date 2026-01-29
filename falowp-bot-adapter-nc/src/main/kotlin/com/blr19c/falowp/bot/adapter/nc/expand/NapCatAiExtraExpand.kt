@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi

/**
 * NapCatAiExtraExpand
 */
class NapCatAiExtraExpand {
}

/**
 * 获取 AI 语音
 *
 * 通过 AI 语音引擎获取指定文本的语音 URL
 */
suspend fun NapCatBotApi.getAiRecord(character: String, groupId: String, text: String): String {
    return apiRequest("get_ai_record", mapOf("character" to character, "group_id" to groupId, "text" to text))
}

/**
 * 发送群 AI 语音
 *
 * 发送 AI 生成的语音到指定群聊
 */
suspend fun NapCatBotApi.sendGroupAiRecord(character: String, groupId: String, text: String) {
    apiRequestUnit("send_group_ai_record", mapOf("character" to character, "group_id" to groupId, "text" to text))
}
