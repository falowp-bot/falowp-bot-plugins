package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.listener.events.GroupIncreaseEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 群聊成员增加
 */
object NapCatGroupIncreaseNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群聊成员增加:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()
        val operatorId = originalMessage.path("operator_id").safeString()
        //invite-管理员邀请入群 approve-管理员已同意入群
        val subType = originalMessage.path("sub_type").safeString()

        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val user = NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val actor = NapCatBotApiSupport.getGroupMemberInfo(groupId, operatorId)

        return GroupIncreaseEvent(source, actor, user, subType)
    }
}