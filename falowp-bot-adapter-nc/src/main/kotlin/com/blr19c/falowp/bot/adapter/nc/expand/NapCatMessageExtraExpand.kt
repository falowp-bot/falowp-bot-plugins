@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatMessageExtraExpand
 */
class NapCatMessageExtraExpand {
    /**
     * FetchEmojiLikeEmojiLikesListItem
     */
    data class FetchEmojiLikeEmojiLikesListItem(
        /**
         * TinyID
         */
        @field:JsonProperty("tinyId")
        val tinyId: String,
        /**
         * 昵称
         */
        @field:JsonProperty("nickName")
        val nickName: String,
        /**
         * 头像URL
         */
        @field:JsonProperty("headUrl")
        val headUrl: String
    )

    /**
     * FetchEmojiLike
     */
    data class FetchEmojiLike(
        /**
         * 表情回应列表
         */
        @field:JsonProperty("emojiLikesList")
        val emojiLikesList: List<FetchEmojiLikeEmojiLikesListItem>,
        /**
         * 分页Cookie
         */
        @field:JsonProperty("cookie")
        val cookie: String,
        /**
         * 是否最后一页
         */
        @field:JsonProperty("isLastPage")
        val isLastPage: Boolean,
        /**
         * 是否第一页
         */
        @field:JsonProperty("isFirstPage")
        val isFirstPage: Boolean,
        /**
         * 结果状态码
         */
        @field:JsonProperty("result")
        val result: Long,
        /**
         * 错 误信息
         */
        @field:JsonProperty("errMsg")
        val errMsg: String
    )

    /**
     * EmojiLikesEmojiLikeListItem
     */
    data class EmojiLikesEmojiLikeListItem(
        /**
         * 点击者QQ号
         */
        @field:JsonProperty("user_id")
        val userId: String,
        /**
         * 昵称?
         */
        @field:JsonProperty("nick_name")
        val nickName: String
    )

    /**
     * EmojiLikes
     */
    data class EmojiLikes(
        /**
         * 表情回应列表
         */
        @field:JsonProperty("emoji_like_list")
        val emojiLikeList: List<EmojiLikesEmojiLikeListItem>
    )

}

/**
 * 分享群 (Ark)
 *
 * 获取群分享的 Ark 内容
 * @param groupId 群号
 */
suspend fun NapCatBotApi.ArkShareGroup(groupId: String) {
    apiRequestUnit("ArkShareGroup", mapOf("group_id" to groupId))
}

/**
 * 分享用户 (Ark)
 *
 * 获取用户推荐的 Ark 内容
 * @param userId QQ号
 * @param groupId 群号
 * @param phoneNumber 手机号
 */
suspend fun NapCatBotApi.ArkSharePeer(userId: String? = null, groupId: String? = null, phoneNumber: String) {
    apiRequestUnit("ArkSharePeer", mapOf("user_id" to userId, "group_id" to groupId, "phone_number" to phoneNumber))
}

/**
 * 点击内联键盘按钮
 * @param groupId 群号
 * @param botAppid 机器人AppID
 * @param buttonId 按钮ID
 * @param callbackData 回调数据
 * @param msgSeq 消息序列号
 */
suspend fun NapCatBotApi.clickInlineKeyboardButton(groupId: String, botAppid: String, buttonId: String, callbackData: String, msgSeq: String) {
    apiRequestUnit("click_inline_keyboard_button", mapOf("group_id" to groupId, "bot_appid" to botAppid, "button_id" to buttonId, "callback_data" to callbackData, "msg_seq" to msgSeq))
}

/**
 * 获取表情点赞详情
 * @param messageId 消息ID
 * @param emojiId 表情ID
 * @param emojiType 表情类型
 * @param count 获取数量
 * @param cookie 分页Cookie
 */
suspend fun NapCatBotApi.fetchEmojiLike(messageId: Long, emojiId: Long, emojiType: Long, count: Long, cookie: String): NapCatMessageExtraExpand.FetchEmojiLike {
    return apiRequest("fetch_emoji_like", mapOf("message_id" to messageId, "emojiId" to emojiId, "emojiType" to emojiType, "count" to count, "cookie" to cookie))
}

/**
 * 获取消息表情点赞列表
 * @param groupId 群号，短ID可不传
 * @param messageId 消息ID，可以传递长ID或短ID
 * @param emojiId 表情ID
 * @param emojiType 表情类型
 * @param count 数量，0代表全部
 */
suspend fun NapCatBotApi.getEmojiLikes(groupId: String? = null, messageId: String, emojiId: String, emojiType: String? = null, count: Long): NapCatMessageExtraExpand.EmojiLikes {
    return apiRequest("get_emoji_likes", mapOf("group_id" to groupId, "message_id" to messageId, "emoji_id" to emojiId, "emoji_type" to emojiType, "count" to count))
}

/**
 * 分享用户 (Ark)
 *
 * 获取用户推荐的 Ark 内容
 * @param userId QQ号
 * @param groupId 群号
 * @param phoneNumber 手机号
 */
suspend fun NapCatBotApi.sendArkShare(userId: String? = null, groupId: String? = null, phoneNumber: String) {
    apiRequestUnit("send_ark_share", mapOf("user_id" to userId, "group_id" to groupId, "phone_number" to phoneNumber))
}

/**
 * 分享群 (Ark)
 *
 * 获取群分享的 Ark 内容
 * @param groupId 群号
 */
suspend fun NapCatBotApi.sendGroupArkShare(groupId: String) {
    apiRequestUnit("send_group_ark_share", mapOf("group_id" to groupId))
}

/**
 * 设置消息表情点赞
 * @param messageId 消息ID
 * @param emojiId 表情ID
 * @param set 是否设置
 */
suspend fun NapCatBotApi.setMsgEmojiLike(messageId: Long, emojiId: Long, set: Boolean? = null) {
    apiRequestUnit("set_msg_emoji_like", mapOf("message_id" to messageId, "emoji_id" to emojiId, "set" to set))
}
