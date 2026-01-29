@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi

/**
 * NapCatMessagePrivateSendExpand
 */
class NapCatMessagePrivateSendExpand

/**
 * 发送私聊图片
 *
 * 发送群消息
 */
suspend fun NapCatBotApi.sendPrivateMsg(userId: Long, message: List<Any>) {
    apiRequestUnit("send_private_msg", mapOf("user_id" to userId, "message" to message))
}
