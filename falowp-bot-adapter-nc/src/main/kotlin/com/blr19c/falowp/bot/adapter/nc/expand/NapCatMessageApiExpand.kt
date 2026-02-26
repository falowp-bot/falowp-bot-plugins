@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * NapCatMessageApiExpand 消息API
 */
class NapCatMessageApiExpand {
    /**
     * Msg
     */
    data class Msg(
        /**
         * 发送时间
         */
        @field:JsonProperty("time")
        val time: Long,
        /**
         * 消息类型
         */
        @field:JsonProperty("message_type")
        val messageType: String,
        /**
         * 消息ID
         */
        @field:JsonProperty("message_id")
        val messageId: String,
        /**
         * 真实ID
         */
        @field:JsonProperty("real_id")
        val realId: Long,
        /**
         * 消息序号
         */
        @field:JsonProperty("message_seq")
        val messageSeq: Long,
        /**
         * sender
         */
        @field:JsonProperty("sender")
        val sender: String,
        /**
         * message
         */
        @field:JsonProperty("message")
        val message: String,
        /**
         * 原始消息内容
         */
        @field:JsonProperty("raw_message")
        val rawMessage: String,
        /**
         * 字体
         */
        @field:JsonProperty("font")
        val font: Long,
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: Long?,
        /**
         * 发送者QQ号
         */
        @field:JsonProperty("user_id")
        val userId: Long,
        /**
         * 表情回应列表
         */
        @field:JsonProperty("emoji_likes_list")
        val emojiLikesList: List<String>?
    )

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

    /**
     * 消息ID
     */
    data class MessageIdInfo(
        @field:JsonProperty("message_id")
        val messageId: String
    ) {

        fun saveHistory(vararg sendMessageChain: SendMessageChain, forward: Boolean = false) {
            val messageHistory = NapCatBotApiSupport.MessageHistory(this.messageId, forward)
            sendMessageChain.forEach {
                NapCatBotApiSupport.messageHistory.put(it.id, messageHistory)
            }
        }
    }
}


/**
 * 发送群聊消息
 *
 * @param groupId 群组ID
 * @param message 消息内容
 */
suspend fun NapCatBotApi.sendGroupMsg(
    groupId: String = this.receiveMessage.source.id,
    message: List<NapCatMessage.Message>
): NapCatMessageApiExpand.MessageIdInfo {
    val body = mapOf("group_id" to groupId, "message" to message)
    return apiRequest("send_group_msg", body)
}

/**
 * 发送私聊消息
 *
 * @param userId 用户ID
 * @param message 消息内容
 */
suspend fun NapCatBotApi.sendPrivateMsg(
    userId: String = this.receiveMessage.sender.id,
    message: List<NapCatMessage.Message>
): NapCatMessageApiExpand.MessageIdInfo {
    val body = mapOf("user_id" to userId, "message" to message)
    return apiRequest("send_private_msg", body)
}

/**
 * 发送戳一戳
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 */
suspend fun NapCatBotApi.sendPoke(groupId: String? = null, userId: String = this.receiveMessage.sender.id) {
    apiRequestUnit("send_poke", mapOf("group_id" to groupId, "user_id" to userId))
}

/**
 * 标记所有消息已读
 */
suspend fun NapCatBotApi.markAllAsRead() {
    apiRequestUnit("_mark_all_as_read")
}

/**
 * 撤回消息
 *
 * 撤回已发送的消息
 *
 * @param messageId 消息ID
 */
suspend fun NapCatBotApi.deleteMsg(messageId: String) {
    apiRequestUnit("delete_msg", mapOf("message_id" to messageId))
}

/**
 * 转发单条消息-私聊
 *
 * @param messageId 消息ID
 * @param userId 用户ID
 */
suspend fun NapCatBotApi.forwardFriendSingleMsg(messageId: String, userId: String = this.receiveMessage.sender.id) {
    apiRequestUnit(
        "forward_friend_single_msg",
        mapOf("message_id" to messageId, "user_id" to userId)
    )
}

/**
 * 转发单条消息-群聊
 *
 * @param messageId 消息ID
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.forwardGroupSingleMsg(messageId: String, groupId: String = this.receiveMessage.source.id) {
    apiRequestUnit(
        "forward_group_single_msg",
        mapOf("message_id" to messageId, "group_id" to groupId)
    )
}

/**
 * 获取消息
 *
 * @param messageId 消息ID
 */
suspend fun NapCatBotApi.getMsg(messageId: String): NapCatMessage {
    return apiRequest("get_msg", mapOf("message_id" to messageId))
}

/**
 * 标记群聊已读
 *
 * @param messageId 消息ID
 */
suspend fun NapCatBotApi.markGroupMsgAsRead(messageId: String = this.receiveMessage.id) {
    apiRequestUnit("mark_group_msg_as_read", mapOf("message_id" to messageId))
}

/**
 * 标记消息已读 (Go-CQHTTP)
 *
 * @param userId 用户ID
 * @param groupId 群组ID
 * @param messageId 消息ID
 */
suspend fun NapCatBotApi.markMsgAsRead(userId: String? = null, groupId: String? = null, messageId: String? = null) {
    apiRequestUnit("mark_msg_as_read", mapOf("user_id" to userId, "group_id" to groupId, "message_id" to messageId))
}

/**
 * 标记私聊已读
 *
 * @param messageId 消息ID
 */
suspend fun NapCatBotApi.markPrivateMsgAsRead(messageId: String = this.receiveMessage.id) {
    apiRequestUnit("mark_private_msg_as_read", mapOf("message_id" to messageId))
}

/**
 * 分享群 (Ark)
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.arkShareGroup(groupId: String = this.receiveMessage.source.id): JsonNode {
    return apiRequest<JsonNode>("ArkShareGroup", mapOf("group_id" to groupId))
}

/**
 * 分享用户 (Ark)
 *
 * @param userId 用户ID
 */
suspend fun NapCatBotApi.arkSharePeer(userId: String = this.receiveMessage.sender.id): JsonNode {
    return apiRequest<JsonNode>("ArkSharePeer", mapOf("user_id" to userId))
        .path("arkMsg")
}

/**
 * 点击内联键盘按钮
 *
 * @param groupId 群组ID
 * @param botAppid 机器人AppID
 * @param buttonId 按钮ID
 * @param callbackData 回调数据
 * @param msgSeq 消息序列号
 */
suspend fun NapCatBotApi.clickInlineKeyboardButton(
    groupId: String = this.receiveMessage.source.id,
    botAppid: String,
    buttonId: String,
    callbackData: String,
    msgSeq: String
) {
    apiRequestUnit(
        "click_inline_keyboard_button",
        mapOf(
            "group_id" to groupId,
            "bot_appid" to botAppid,
            "button_id" to buttonId,
            "callback_data" to callbackData,
            "msg_seq" to msgSeq
        )
    )
}

/**
 * 获取表情点赞详情
 *
 * @param messageId 消息ID
 * @param emojiId 表情ID
 * @param emojiType 表情类型
 * @param count 获取数量
 * @param cookie 分页Cookie
 */
suspend fun NapCatBotApi.fetchEmojiLike(
    messageId: String,
    emojiId: String,
    emojiType: Long,
    count: Long,
    cookie: String
): NapCatMessageApiExpand.FetchEmojiLike {
    return apiRequest(
        "fetch_emoji_like",
        mapOf(
            "message_id" to messageId,
            "emojiId" to emojiId,
            "emojiType" to emojiType,
            "count" to count,
            "cookie" to cookie
        )
    )
}

/**
 * 获取消息表情点赞列表
 *
 * @param groupId 群组ID
 * @param messageId 消息ID
 * @param emojiId 表情ID
 * @param emojiType 表情类型
 * @param count 获取数量
 */
suspend fun NapCatBotApi.getEmojiLikes(
    groupId: String? = null,
    messageId: String,
    emojiId: String,
    emojiType: String? = null,
    count: Long
): NapCatMessageApiExpand.EmojiLikes {
    return apiRequest(
        "get_emoji_likes",
        mapOf(
            "group_id" to groupId,
            "message_id" to messageId,
            "emoji_id" to emojiId,
            "emoji_type" to emojiType,
            "count" to count
        )
    )
}

/**
 * 设置消息表情点赞
 *
 * @param messageId 消息ID
 * @param emojiId 表情ID
 * @param set 是否点赞
 */
suspend fun NapCatBotApi.setMsgEmojiLike(messageId: String, emojiId: String, set: Boolean? = null) {
    apiRequestUnit("set_msg_emoji_like", mapOf("message_id" to messageId, "emoji_id" to emojiId, "set" to set))
}