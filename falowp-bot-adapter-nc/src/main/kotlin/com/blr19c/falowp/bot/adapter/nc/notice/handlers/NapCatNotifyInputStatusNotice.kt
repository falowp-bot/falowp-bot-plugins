package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatNotifyInputStatusEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 输入状态更新
 */
object NapCatNotifyInputStatusNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-输入状态更新:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val statusText = originalMessage.path("status_text").safeString()
        val eventType = originalMessage.path("event_type").safeString()

        val user = NapCatBotApiSupport.getFriendInfo(userId) ?: ReceiveMessage.User.empty().copy(id = userId)
        val source = ReceiveMessage.Source(userId, SourceTypeEnum.PRIVATE)

        return NapCatNotifyInputStatusEvent(user, source, eventType, statusText)
    }
}