package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.expand.getMsg
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatFaceEmoji
import com.blr19c.falowp.bot.adapter.nc.message.expand.toBotMessage
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatGroupMsgEmojiLikeEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 群聊表情回应
 */
object NapCatGroupMsgEmojiLikeNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群聊表情回应:{}", originalMessage)

        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()
        val messageId = originalMessage.path("message_id").safeString()
        val add = originalMessage.path("is_add").asBoolean()
        val likes = originalMessage.withArray("likes").map {
            NapCatGroupMsgEmojiLikeEvent.Emoji(
                NapCatFaceEmoji.fromValue(it.path("emoji_id").safeString(), "").toNapCatEmoji(),
                it.path("count").asInt()
            )
        }

        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val user = NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val message = NapCatBotApiSupport.tempBot.getMsg(messageId).toBotMessage()

        return NapCatGroupMsgEmojiLikeEvent(source, user, message, likes, add)
    }
}