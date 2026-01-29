@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GetAiCharactersDataItemCharactersItem
 */
data class GetAiCharactersDataItemCharactersItem(
    @field:JsonProperty("character_id")
    val characterId: String,
    @field:JsonProperty("character_name")
    val characterName: String,
    @field:JsonProperty("preview_url")
    val previewUrl: String
)

/**
 * GetAiCharactersDataItem
 */
data class GetAiCharactersDataItem(
    @field:JsonProperty("type")
    val type: String,
    @field:JsonProperty("characters")
    val characters: List<GetAiCharactersDataItemCharactersItem>
)

/**
 * GetClientkeyData
 */
data class GetClientkeyData(
    @field:JsonProperty("clientkey")
    val clientkey: String
)

/**
 * NapCatExtraApiExpand
 */
class NapCatExtraApiExpand

/**
 * .OCR 图片识别
 *
 * 仅 Windows 可用
 */
suspend fun NapCatBotApi..ocrImage(image: String) {
    apiRequestUnit(".ocr_image", mapOf("image" to image))
}

/**
 * 创建收藏
 */
suspend fun NapCatBotApi.createCollection(rawData: String, brief: String) {
    apiRequestUnit("create_collection", mapOf("rawData" to rawData, "brief" to brief))
}

/**
 * 获取AI语音人物
 */
suspend fun NapCatBotApi.getAiCharacters(groupId: Long, chatType: Long? = null): List<GetAiCharactersDataItem> {
    return apiRequest("get_ai_characters", mapOf("group_id" to groupId, "chat_type" to chatType))
}

/**
 * 获取clientkey
 */
suspend fun NapCatBotApi.getClientkey(): GetClientkeyData {
    return apiRequest("get_clientkey")
}

/**
 * OCR 图片识别
 *
 * 仅 Windows 可用
 */
suspend fun NapCatBotApi.ocrImage(image: String) {
    apiRequestUnit("ocr_image", mapOf("image" to image))
}

/**
 * 批量踢出群成员
 */
suspend fun NapCatBotApi.setGroupKickMembers(groupId: Long, userId: List<Long>, rejectAddRequest: Boolean? = null) {
    apiRequestUnit("set_group_kick_members", mapOf("group_id" to groupId, "user_id" to userId, "reject_add_request" to rejectAddRequest))
}

/**
 * 设置群头衔
 */
suspend fun NapCatBotApi.setGroupSpecialTitle(groupId: Long, userId: Long, specialTitle: String? = null) {
    apiRequestUnit("set_group_special_title", mapOf("group_id" to groupId, "user_id" to userId, "special_title" to specialTitle))
}

/**
 * 设置头像
 */
suspend fun NapCatBotApi.setQqAvatar(file: String) {
    apiRequestUnit("set_qq_avatar", mapOf("file" to file))
}

/**
 * 设置个性签名
 */
suspend fun NapCatBotApi.setSelfLongnick(longNick: String) {
    apiRequestUnit("set_self_longnick", mapOf("longNick" to longNick))
}

/**
 * 英译中
 */
suspend fun NapCatBotApi.translateEn2zh(words: List<String>) {
    apiRequestUnit("translate_en2zh", mapOf("words" to words))
}
