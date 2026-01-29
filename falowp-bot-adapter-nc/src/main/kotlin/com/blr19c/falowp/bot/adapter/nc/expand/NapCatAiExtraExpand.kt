@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatAiExtraExpand AI API
 */
class NapCatAiExtraExpand {
    /**
     * AiCharactersItemItemCharactersItem
     */
    data class AiCharactersItemItemCharactersItem(
        /**
         * 角色ID
         */
        @field:JsonProperty("character_id")
        val characterId: String,
        /**
         * 角色名称
         */
        @field:JsonProperty("character_name")
        val characterName: String,
        /**
         * 预览URL
         */
        @field:JsonProperty("preview_url")
        val previewUrl: String
    )

    /**
     * AiCharactersItemItem
     */
    data class AiCharactersItemItem(
        /**
         * 角色类型
         */
        @field:JsonProperty("type")
        val type: String,
        /**
         * 角色列表
         */
        @field:JsonProperty("characters")
        val characters: List<AiCharactersItemItemCharactersItem>
    )
}

/**
 * 获取 AI 语音
 *
 * 通过 AI 语音引擎获取指定文本的语音 URL
 *
 * @param character AI 角色标识
 * @param groupId 群组ID
 * @param text 文本内容
 */
suspend fun NapCatBotApi.getAiRecord(character: String, groupId: String, text: String): String {
    return apiRequest("get_ai_record", mapOf("character" to character, "group_id" to groupId, "text" to text))
}

/**
 * 获取AI角色列表
 *
 * 获取群聊中的AI角色列表
 *
 * @param groupId 群组ID
 * @param chatType 聊天类型
 */
suspend fun NapCatBotApi.getAiCharacters(
    groupId: String,
    chatType: Long
): List<NapCatAiExtraExpand.AiCharactersItemItem> {
    return apiRequest("get_ai_characters", mapOf("group_id" to groupId, "chat_type" to chatType))
}

/**
 * 发送群 AI 语音
 *
 * 发送 AI 生成的语音到指定群聊
 *
 * @param character AI 角色标识
 * @param groupId 群组ID
 * @param text 文本内容
 */
suspend fun NapCatBotApi.sendGroupAiRecord(character: String, groupId: String, text: String) {
    apiRequestUnit("send_group_ai_record", mapOf("character" to character, "group_id" to groupId, "text" to text))
}
