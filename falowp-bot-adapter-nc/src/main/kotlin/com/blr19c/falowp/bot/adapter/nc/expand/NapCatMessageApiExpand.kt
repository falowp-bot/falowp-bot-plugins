@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatMessageApiExpand
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
        val messageId: Long,
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

}


/**
 * 发送群聊消息
 *
 * @param groupId 群聊ID
 * @param message 消息内容
 */
suspend fun NapCatBotApi.sendGroupMsg(
    groupId: String = this.receiveMessage.source.id, message: List<NapCatMessage.Message>
): Long {
    val body = mapOf("group_id" to groupId, "message" to message)
    return apiRequest<Long>("send_group_msg", body)
}

/**
 * 发送私聊消息
 *
 * @param userId 用户ID
 * @param message 消息内容
 */
suspend fun NapCatBotApi.sendPrivateMsg(
    userId: String = this.receiveMessage.sender.id, message: List<NapCatMessage.Message>
): Long {
    val body = mapOf("user_id" to userId, "message" to message)
    return apiRequest<Long>("send_private_msg", body)
}

/**
 * 发送戳一戳
 *
 * 在群聊或私聊中发送戳一戳动作
 */
suspend fun NapCatBotApi.sendPoke(groupId: String? = null, userId: String) {
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
 */
suspend fun NapCatBotApi.deleteMsg(messageId: Long) {
    apiRequestUnit("delete_msg", mapOf("message_id" to messageId))
}

/**
 * 转发单条消息-私聊
 */
suspend fun NapCatBotApi.forwardFriendSingleMsg(messageId: Long, userId: String) {
    apiRequestUnit(
        "forward_friend_single_msg",
        mapOf("message_id" to messageId, "user_id" to userId)
    )
}

/**
 * 转发单条消息-群聊
 */
suspend fun NapCatBotApi.forwardGroupSingleMsg(messageId: Long, groupId: String) {
    apiRequestUnit(
        "forward_group_single_msg",
        mapOf("message_id" to messageId, "group_id" to groupId)
    )
}

/**
 * 获取消息
 *
 * 根据消息 ID 获取消息详细信息
 */
suspend fun NapCatBotApi.getMsg(messageId: String): NapCatMessage {
    return apiRequest("get_msg", mapOf("message_id" to messageId))
}

/**
 * 标记群聊已读
 *
 * 标记指定渠道的消息为已读
 */
suspend fun NapCatBotApi.markGroupMsgAsRead(
    userId: String? = null,
    groupId: String? = null,
    messageId: String? = null
) {
    apiRequestUnit(
        "mark_group_msg_as_read",
        mapOf("user_id" to userId, "group_id" to groupId, "message_id" to messageId)
    )
}

/**
 * 标记消息已读 (Go-CQHTTP)
 *
 * 标记指定渠道的消息为已读
 */
suspend fun NapCatBotApi.markMsgAsRead(userId: String? = null, groupId: String? = null, messageId: String? = null) {
    apiRequestUnit("mark_msg_as_read", mapOf("user_id" to userId, "group_id" to groupId, "message_id" to messageId))
}

/**
 * 标记私聊已读
 *
 * 标记指定渠道的消息为已读
 */
suspend fun NapCatBotApi.markPrivateMsgAsRead(
    userId: String? = null,
    groupId: String? = null,
    messageId: String? = null
) {
    apiRequestUnit(
        "mark_private_msg_as_read",
        mapOf("user_id" to userId, "group_id" to groupId, "message_id" to messageId)
    )
}

/**
 * 设置在线状态
 *
 * 
## 状态列表

### 在线

 */
suspend fun NapCatBotApi.setOnlineStatus() {
    apiRequestUnit("set_online_status")
}
