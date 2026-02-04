package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.expand.getMsg
import com.blr19c.falowp.bot.adapter.nc.message.expand.toBotMessage
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.listener.events.WithdrawMessageEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 私聊消息撤回
 */
object NapCatFriendRecallNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-私聊消息撤回:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val messageId = originalMessage.path("message_id").safeString()

        val message = NapCatBotApiSupport.tempBot.getMsg(messageId).toBotMessage()
        val source = ReceiveMessage.Source(userId, SourceTypeEnum.PRIVATE)

        return WithdrawMessageEvent(source, message, message.sender)
    }

}
