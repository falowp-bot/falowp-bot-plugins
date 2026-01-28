package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.listener.events.RequestAddFriendEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 好友添加
 */
object NapCatFriendAddNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-好友添加:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val comment = originalMessage.path("comment").safeString()
        val flag = originalMessage.path("flag").safeString()

        val source = ReceiveMessage.Source(userId, SourceTypeEnum.PRIVATE)

        return RequestAddFriendEvent(userId, source, comment, flag)
    }
}