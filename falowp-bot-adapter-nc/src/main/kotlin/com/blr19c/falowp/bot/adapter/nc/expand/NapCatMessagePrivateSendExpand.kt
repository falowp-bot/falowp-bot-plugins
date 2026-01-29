@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode

/**
 * NapCatMessagePrivateSendExpand
 */
class NapCatMessagePrivateSendExpand {
}

/**
 * 发送私聊图片
 *
 * 发送群消息
 * @param userId user_id
 * @param message message
 */
suspend fun NapCatBotApi.sendPrivateMsg(userId: Long, message: List<JsonNode>) {
    apiRequestUnit("send_private_msg", mapOf("user_id" to userId, "message" to message))
}
