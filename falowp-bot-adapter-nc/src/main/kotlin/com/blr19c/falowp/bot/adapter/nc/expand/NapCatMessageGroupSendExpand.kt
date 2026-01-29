@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi

/**
 * NapCatMessageGroupSendExpand
 */
class NapCatMessageGroupSendExpand

/**
 * 发送群艾特
 *
 * 发送群消息
 */
suspend fun NapCatBotApi.sendGroupMsg(groupId: Long, message: List<MessageItem>) {
    apiRequestUnit("send_group_msg", mapOf("group_id" to groupId, "message" to message))
}
