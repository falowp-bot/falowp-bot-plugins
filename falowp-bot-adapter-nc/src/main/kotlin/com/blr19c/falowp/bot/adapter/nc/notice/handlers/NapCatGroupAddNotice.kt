package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.listener.events.RequestJoinGroupEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 加群请求
 */
object NapCatGroupAddNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-加群请求:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()
        val comment = originalMessage.path("comment").safeString()
        val flag = originalMessage.path("flag").safeString()
        val subType = originalMessage.path("sub_type").safeString()

        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)

        return RequestJoinGroupEvent(source, userId, comment, flag, subType)
    }

}
