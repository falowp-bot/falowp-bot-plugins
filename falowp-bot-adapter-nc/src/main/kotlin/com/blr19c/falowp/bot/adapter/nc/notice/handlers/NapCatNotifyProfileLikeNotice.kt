package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatNotifyProfileLikeNoticeEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 点赞
 */
object NapCatNotifyProfileLikeNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-点赞:{}", originalMessage)

        val operatorId = originalMessage.path("operator_id").safeString()
        val nickname = originalMessage.path("operator_nick").safeString()
        val count = originalMessage.path("times").intValue()

        val operator = NapCatBotApiSupport.getFriendInfo(operatorId) ?: ReceiveMessage.User.empty()
            .copy(id = operatorId, nickname = nickname)
        val source = ReceiveMessage.Source(operatorId, SourceTypeEnum.PRIVATE)

        return NapCatNotifyProfileLikeNoticeEvent(source, operator, count)
    }
}