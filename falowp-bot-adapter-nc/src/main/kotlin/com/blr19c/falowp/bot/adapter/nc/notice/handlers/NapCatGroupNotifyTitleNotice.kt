package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatGroupNotifyTitleEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 群成员头衔变更
 */
object NapCatGroupNotifyTitleNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群成员头衔变更:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()
        val title = originalMessage.path("title").safeString()

        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val user = NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val actor = NapCatBotApiSupport.getGroupOwner(groupId) ?: ReceiveMessage.User.empty()

        return NapCatGroupNotifyTitleEvent(source, actor, user, title)
    }
}