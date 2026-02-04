package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatGroupAdminEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 群聊管理员变动
 */
object NapCatGroupAdminNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群聊管理员变动:{}", originalMessage)

        val groupId = originalMessage.path("group_id").safeString()
        val userId = originalMessage.path("user_id").safeString()
        val subType = originalMessage.path("sub_type").safeString()

        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val user = NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val actor = NapCatBotApiSupport.getGroupOwner(groupId) ?: ReceiveMessage.User.empty()

        return NapCatGroupAdminEvent(source, actor, user, subType)
    }
}