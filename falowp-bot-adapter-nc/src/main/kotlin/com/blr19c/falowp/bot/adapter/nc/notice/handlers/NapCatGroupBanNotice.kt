package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatGroupBanEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode
import kotlin.time.Duration.Companion.seconds

/**
 * 群聊禁言
 */
object NapCatGroupBanNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群聊禁言:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()
        val subType = originalMessage.path("sub_type").safeString()
        val operatorId = originalMessage.path("operator_id").safeString()
        val duration = originalMessage.path("duration").safeString()

        val user = NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val actor = NapCatBotApiSupport.getGroupMemberInfo(groupId, operatorId)
        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val durationTime = duration.toLong().seconds

        return NapCatGroupBanEvent(source, actor, user, durationTime, subType)
    }
}