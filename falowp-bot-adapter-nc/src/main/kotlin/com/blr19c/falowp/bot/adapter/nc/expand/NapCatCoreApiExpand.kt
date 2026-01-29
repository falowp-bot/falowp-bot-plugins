@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode

/**
 * NapCatCoreApiExpand
 */
class NapCatCoreApiExpand {
}

/**
 * 发送戳一戳
 *
 * 在群聊或私聊中发送戳一戳动作
 */
suspend fun NapCatBotApi.friendPoke(groupId: String? = null, userId: String, targetId: String? = null) {
    apiRequestUnit("friend_poke", mapOf("group_id" to groupId, "user_id" to userId, "target_id" to targetId))
}

/**
 * 发送戳一戳
 *
 * 在群聊或私聊中发送戳一戳动作
 */
suspend fun NapCatBotApi.groupPoke(groupId: String? = null, userId: String, targetId: String? = null) {
    apiRequestUnit("group_poke", mapOf("group_id" to groupId, "user_id" to userId, "target_id" to targetId))
}

/**
 * 发送戳一戳
 *
 * 在群聊或私聊中发送戳一戳动作
 */
suspend fun NapCatBotApi.sendPoke(groupId: String? = null, userId: String, targetId: String? = null) {
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
