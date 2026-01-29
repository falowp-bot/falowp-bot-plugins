@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi

/**
 * NapCatCoreApiExpand
 */
class NapCatCoreApiExpand

/**
 * 发送私聊戳一戳
 */
suspend fun NapCatBotApi.friendPoke(userId: Long, targetId: Any? = null) {
    apiRequestUnit("friend_poke", mapOf("user_id" to userId, "target_id" to targetId))
}

/**
 * 发送群聊戳一戳
 */
suspend fun NapCatBotApi.groupPoke(groupId: Long, userId: Long) {
    apiRequestUnit("group_poke", mapOf("group_id" to groupId, "user_id" to userId))
}

/**
 * 发送戳一戳
 */
suspend fun NapCatBotApi.sendPoke(groupId: Long? = null, userId: Long, targetId: String? = null) {
    apiRequestUnit("send_poke", mapOf("group_id" to groupId, "user_id" to userId, "target_id" to targetId))
}

/**
 * 设置群待办
 *
 * 将指定消息设置为群待办
 */
suspend fun NapCatBotApi.setGroupTodo(groupId: String, messageId: String? = null, messageSeq: String? = null) {
    apiRequestUnit("set_group_todo", mapOf("group_id" to groupId, "message_id" to messageId, "message_seq" to messageSeq))
}
