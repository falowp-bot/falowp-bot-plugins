@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatExtraApiExpand
 */
class NapCatExtraApiExpand {
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
     * Clientkey
     */
    data class Clientkey(
        /**
         * 客户端Key
         */
        @field:JsonProperty("clientkey")
        val clientkey: String?
    )

}

/**
 * 图片 OCR 识别 (内部)
 *
 * 识别图片中的文字内容(仅Windows端支持)
 * @param image 图片路径、URL或Base64
 */
suspend fun NapCatBotApi..ocrImage(image: String) {
    apiRequestUnit(".ocr_image", mapOf("image" to image))
}

/**
 * 创建收藏
 * @param rawData 原始数据
 * @param brief 简要描述
 */
suspend fun NapCatBotApi.createCollection(rawData: String, brief: String) {
    apiRequestUnit("create_collection", mapOf("rawData" to rawData, "brief" to brief))
}

/**
 * 获取AI角色列表
 *
 * 获取群聊中的AI角色列表
 * @param groupId 群号
 * @param chatType 聊天类型
 */
suspend fun NapCatBotApi.getAiCharacters(groupId: String, chatType: Long): NapCatExtraApiExpand.List<AiCharactersItemItem> {
    return apiRequest("get_ai_characters", mapOf("group_id" to groupId, "chat_type" to chatType))
}

/**
 * 获取ClientKey
 *
 * 获取当前登录帐号的ClientKey
 */
suspend fun NapCatBotApi.getClientkey(): NapCatExtraApiExpand.Clientkey {
    return apiRequest("get_clientkey")
}

/**
 * 图片 OCR 识别
 *
 * 识别图片中的文字内容(仅Windows端支持)
 * @param image 图片路径、URL或Base64
 */
suspend fun NapCatBotApi.ocrImage(image: String) {
    apiRequestUnit("ocr_image", mapOf("image" to image))
}

/**
 * 批量踢出群成员
 *
 * 从指定群聊中批量踢出多个成员
 * @param groupId 群号
 * @param userId QQ号列表
 * @param rejectAddRequest 是否拒绝加群请求
 */
suspend fun NapCatBotApi.setGroupKickMembers(groupId: String, userId: List<String>, rejectAddRequest: Boolean? = null) {
    apiRequestUnit("set_group_kick_members", mapOf("group_id" to groupId, "user_id" to userId, "reject_add_request" to rejectAddRequest))
}

/**
 * 设置专属头衔
 *
 * 设置群聊中指定成员的专属头衔
 * @param groupId 群号
 * @param userId QQ号
 * @param specialTitle 专属头衔
 */
suspend fun NapCatBotApi.setGroupSpecialTitle(groupId: String, userId: String, specialTitle: String) {
    apiRequestUnit("set_group_special_title", mapOf("group_id" to groupId, "user_id" to userId, "special_title" to specialTitle))
}

/**
 * 设置QQ头像
 *
 * 修改当前账号的QQ头像
 * @param file 图片路径、URL或Base64
 */
suspend fun NapCatBotApi.setQqAvatar(file: String) {
    apiRequestUnit("set_qq_avatar", mapOf("file" to file))
}

/**
 * 设置个性签名
 *
 * 修改当前登录帐号的个性签名
 * @param longNick 签名内容
 */
suspend fun NapCatBotApi.setSelfLongnick(longNick: String) {
    apiRequestUnit("set_self_longnick", mapOf("longNick" to longNick))
}

/**
 * 英文单词翻译
 *
 * 将英文单词列表翻译为中文
 * @param words 待翻译单词列表
 */
suspend fun NapCatBotApi.translateEn2zh(words: List<String>) {
    apiRequestUnit("translate_en2zh", mapOf("words" to words))
}
