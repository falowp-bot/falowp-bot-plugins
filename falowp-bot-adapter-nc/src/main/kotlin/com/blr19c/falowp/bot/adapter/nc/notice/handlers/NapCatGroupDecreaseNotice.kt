package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.listener.events.GroupDecreaseEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 群聊成员减少
 */
object NapCatGroupDecreaseNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群聊成员减少:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()
        //leave-主动退群 kick-成员被踢 kick_me-登录号被踢
        val subType = originalMessage.path("sub_type").safeString()
        val operatorId = originalMessage.path("operator_id").safeString()

        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val user = ReceiveMessage.User.empty().copy(id = userId)
        val actor = if (operatorId == "0") user else NapCatBotApiSupport.getGroupMemberInfo(groupId, operatorId)

        return GroupDecreaseEvent(source, actor, user, subType)
    }
}
