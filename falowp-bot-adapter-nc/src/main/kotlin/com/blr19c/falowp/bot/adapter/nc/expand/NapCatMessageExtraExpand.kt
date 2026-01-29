@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * ArkSharePeerData
 */
data class ArkSharePeerData(
    @field:JsonProperty("errCode")
    val errCode: Long,
    @field:JsonProperty("errMsg")
    val errMsg: String,
    @field:JsonProperty("arkJson")
    val arkJson: String
)

/**
 * FetchEmojiLikeDataEmojiLikesListItem
 */
data class FetchEmojiLikeDataEmojiLikesListItem(
    @field:JsonProperty("tinyId")
    val tinyId: String,
    @field:JsonProperty("nickName")
    val nickName: String,
    @field:JsonProperty("headUrl")
    val headUrl: String
)

/**
 * FetchEmojiLikeData
 */
data class FetchEmojiLikeData(
    @field:JsonProperty("result")
    val result: Long,
    @field:JsonProperty("errMsg")
    val errMsg: String,
    @field:JsonProperty("emojiLikesList")
    val emojiLikesList: List<FetchEmojiLikeDataEmojiLikesListItem>,
    @field:JsonProperty("cookie")
    val cookie: String,
    @field:JsonProperty("isLastPage")
    val isLastPage: Boolean,
    @field:JsonProperty("isFirstPage")
    val isFirstPage: Boolean
)

/**
 * GetEmojiLikesDataEmojiLikeListItem
 */
data class GetEmojiLikesDataEmojiLikeListItem(
    @field:JsonProperty("user_id")
    val userId: String,
    @field:JsonProperty("nick_name")
    val nickName: String
)

/**
 * GetEmojiLikesData
 */
data class GetEmojiLikesData(
    @field:JsonProperty("emoji_like_list")
    val emojiLikeList: List<GetEmojiLikesDataEmojiLikeListItem>
)

/**
 * NapCatMessageExtraExpand
 */
class NapCatMessageExtraExpand

/**
 * 获取推荐群聊卡片
 */
suspend fun NapCatBotApi.ArkShareGroup(groupId: String): String {
    return apiRequest("ArkShareGroup", mapOf("group_id" to groupId))
}

/**
 * 获取推荐好友/群聊卡片
 */
suspend fun NapCatBotApi.ArkSharePeer(groupId: Long? = null, userId: Long? = null, phoneNumber: String? = null): ArkSharePeerData {
    return apiRequest("ArkSharePeer", mapOf("group_id" to groupId, "user_id" to userId, "phoneNumber" to phoneNumber))
}

/**
 * 点击按钮
 */
suspend fun NapCatBotApi.clickInlineKeyboardButton(groupId: Long, botAppid: String, buttonId: String, callbackData: String, msgSeq: String) {
    apiRequestUnit("click_inline_keyboard_button", mapOf("group_id" to groupId, "bot_appid" to botAppid, "button_id" to buttonId, "callback_data" to callbackData, "msg_seq" to msgSeq))
}

/**
 * 获取贴表情详情
 */
suspend fun NapCatBotApi.fetchEmojiLike(messageId: Long, emojiId: String, emojiType: String, count: Long? = null): FetchEmojiLikeData {
    return apiRequest("fetch_emoji_like", mapOf("message_id" to messageId, "emojiId" to emojiId, "emojiType" to emojiType, "count" to count))
}

/**
 * 获取消息表情点赞列表
 */
suspend fun NapCatBotApi.getEmojiLikes(groupId: String? = null, messageId: String, emojiId: String, emojiType: String? = null, count: Long): GetEmojiLikesData {
    return apiRequest("get_emoji_likes", mapOf("group_id" to groupId, "message_id" to messageId, "emoji_id" to emojiId, "emoji_type" to emojiType, "count" to count))
}

/**
 * 分享用户 (Ark)
 *
 * 获取用户推荐的 Ark 内容
 */
suspend fun NapCatBotApi.sendArkShare(userId: String? = null, groupId: String? = null, phoneNumber: String) {
    apiRequestUnit("send_ark_share", mapOf("user_id" to userId, "group_id" to groupId, "phone_number" to phoneNumber))
}

/**
 * 分享群 (Ark)
 *
 * 获取群分享的 Ark 内容
 */
suspend fun NapCatBotApi.sendGroupArkShare(groupId: String) {
    apiRequestUnit("send_group_ark_share", mapOf("group_id" to groupId))
}

/**
 * 贴表情
 */
suspend fun NapCatBotApi.setMsgEmojiLike(messageId: Long, emojiId: Long, set: Boolean) {
    apiRequestUnit("set_msg_emoji_like", mapOf("message_id" to messageId, "emoji_id" to emojiId, "set" to set))
}
