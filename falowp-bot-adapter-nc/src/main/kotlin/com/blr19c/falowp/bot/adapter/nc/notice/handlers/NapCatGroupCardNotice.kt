package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatGroupCardEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 群成员名片更新
 */
object NapCatGroupCardNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群成员名片更新:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()
        val cardNew = originalMessage.path("card_new").safeString()
        val cardOld = originalMessage.path("card_old").safeString()

        val user = NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)

        return NapCatGroupCardEvent(source, user, cardNew, cardOld)
    }
}