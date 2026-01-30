@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

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

    /**
     * Record
     */
    data class Record(
        /**
         * 本地路径
         */
        @field:JsonProperty("file")
        val file: String?,
        /**
         * 下载URL
         */
        @field:JsonProperty("url")
        val url: String?,
        /**
         * 文件大小
         */
        @field:JsonProperty("file_size")
        val fileSize: String?,
        /**
         * 文件名
         */
        @field:JsonProperty("file_name")
        val fileName: String?,
        /**
         * Base64编码
         */
        @field:JsonProperty("base64")
        val base64: String?
    )
}


/**
 * 获取AI角色列表
 *
 * @param groupId 群组ID
 * @param chatType 聊天类型
 */
suspend fun NapCatBotApi.getAiCharacters(
    groupId: String = this.receiveMessage.source.id,
    chatType: String = "1"
): List<NapCatAiExtraExpand.AiCharactersItemItem> {
    return apiRequest("get_ai_characters", mapOf("group_id" to groupId, "chat_type" to chatType))
}

/**
 * 获取 AI 语音(此方法可能会直接将语音发送)
 *
 * @param character AI 角色标识
 * @param groupId 群组ID
 * @param text 文本内容
 */
suspend fun NapCatBotApi.getAiRecord(
    character: NapCatAiExtraExpand.AiCharactersItemItemCharactersItem,
    groupId: String = this.receiveMessage.source.id,
    text: String
): URI {
    return apiRequest(
        "get_ai_record",
        mapOf("character" to character.characterId, "group_id" to groupId, "text" to text)
    )
}


/**
 * 发送群 AI 语音
 *
 * @param character AI 角色标识
 * @param groupId 群组ID
 * @param text 文本内容
 */
suspend fun NapCatBotApi.sendGroupAiRecord(
    character: NapCatAiExtraExpand.AiCharactersItemItemCharactersItem,
    groupId: String = this.receiveMessage.source.id,
    text: String
) {
    apiRequestUnit(
        "send_group_ai_record",
        mapOf("character" to character.characterId, "group_id" to groupId, "text" to text)
    )
}


/**
 * 获取语音
 *
 * 获取指定语音文件的信息，并支持格式转换
 *
 * @param file 文件路径
 * @param fileId 文件ID
 * @param outFormat 输出格式
 */
suspend fun NapCatBotApi.getRecord(
    file: String? = null,
    fileId: String? = null,
    outFormat: String = "mp3"
): NapCatAiExtraExpand.Record {
    return apiRequest("get_record", mapOf("file" to file, "file_id" to fileId, "out_format" to outFormat))
}