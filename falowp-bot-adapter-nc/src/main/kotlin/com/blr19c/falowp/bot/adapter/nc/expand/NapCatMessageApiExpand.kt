@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi

/**
 * NapCatMessageApiExpand
 */
class NapCatMessageApiExpand

/**
 * _设置所有消息已读
 */
suspend fun NapCatBotApi.markAllAsRead() {
    apiRequestUnit("_mark_all_as_read")
}

/**
 * 撤回消息
 */
suspend fun NapCatBotApi.deleteMsg(messageId: Long) {
    apiRequestUnit("delete_msg", mapOf("message_id" to messageId))
}

/**
 * 消息转发到私聊
 */
suspend fun NapCatBotApi.forwardFriendSingleMsg(userId: Long, messageId: Long) {
    apiRequestUnit("forward_friend_single_msg", mapOf("user_id" to userId, "message_id" to messageId))
}

/**
 * 消息转发到群
 */
suspend fun NapCatBotApi.forwardGroupSingleMsg(groupId: Long, messageId: Long) {
    apiRequestUnit("forward_group_single_msg", mapOf("group_id" to groupId, "message_id" to messageId))
}

/**
 * 获取消息详情
 */
suspend fun NapCatBotApi.getMsg(messageId: Long): NapCatRawData {
    return apiRequest("get_msg", mapOf("message_id" to messageId))
}

/**
 * 设置群聊已读
 */
suspend fun NapCatBotApi.markGroupMsgAsRead(groupId: Long) {
    apiRequestUnit("mark_group_msg_as_read", mapOf("group_id" to groupId))
}

/**
 * 设置消息已读
 */
suspend fun NapCatBotApi.markMsgAsRead(groupId: Long? = null, userId: Long? = null) {
    apiRequestUnit("mark_msg_as_read", mapOf("group_id" to groupId, "user_id" to userId))
}

/**
 * 设置私聊已读
 */
suspend fun NapCatBotApi.markPrivateMsgAsRead(userId: Long) {
    apiRequestUnit("mark_private_msg_as_read", mapOf("user_id" to userId))
}

/**
 * send_msg
 */
suspend fun NapCatBotApi.sendMsg(messageType: String, groupId: Long, userId: Long, message: List<Any>) {
    apiRequestUnit("send_msg", mapOf("message_type" to messageType, "group_id" to groupId, "user_id" to userId, "message" to message))
}

/**
 * 设置输入状态
 *
 * ## 状态列表

### 对方正在说话...

 */
suspend fun NapCatBotApi.setInputStatus() {
    apiRequestUnit("set_input_status")
}

/**
 * 设置在线状态
 *
 * ## 状态列表

### 在线
 */
suspend fun NapCatBotApi.setOnlineStatus() {
    apiRequestUnit("set_online_status")
}
