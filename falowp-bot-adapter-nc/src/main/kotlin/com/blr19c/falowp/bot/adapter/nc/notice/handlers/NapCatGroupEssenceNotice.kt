package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.expand.getEssenceMsgList
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatPostType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatSubType
import com.blr19c.falowp.bot.adapter.nc.message.expand.toBotMessage
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatGroupEssenceEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 群聊设精
 */
object NapCatGroupEssenceNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群聊设精:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()
        val operatorId = originalMessage.path("operator_id").safeString()
        val subType = originalMessage.path("sub_type").safeString()
        val selfId = originalMessage.path("self_id").safeString()

        val essenceMsg = NapCatBotApiSupport.tempBot.getEssenceMsgList(groupId)[0]
        val actor = NapCatBotApiSupport.getGroupMemberInfo(groupId, operatorId)
        val user = if (userId == "0") actor else NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val napCatMessage = NapCatMessage.empty().copy(
            messageId = essenceMsg.messageId,
            postType = NapCatPostType.MESSAGE,
            messageType = NapCatMessageType.GROUP,
            subType = NapCatSubType.GROUP,
            selfId = selfId,
            userId = user.id,
            groupId = groupId,
            time = essenceMsg.operatorTime,
            messageSeq = essenceMsg.messageId,
            realId = essenceMsg.messageId,
            realSeq = essenceMsg.messageId,
            sender = NapCatMessage.Sender(user.id, user.nickname, user.nickname),
            message = essenceMsg.content
        )
        val message = napCatMessage.toBotMessage()

        return NapCatGroupEssenceEvent(source, actor, user, message, subType)
    }
}