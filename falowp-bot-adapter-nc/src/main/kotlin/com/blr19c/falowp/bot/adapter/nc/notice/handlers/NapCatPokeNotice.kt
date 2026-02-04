package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.listener.events.NudgeEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode

/**
 * 戳一戳
 */
object NapCatPokeNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-戳一戳:{}", originalMessage)
        val groupId = originalMessage.path("group_id").safeString()
        return if (groupId.isBlank()) privatePoke(originalMessage) else groupPoke(originalMessage)
    }

    private suspend fun privatePoke(originalMessage: JsonNode): NudgeEvent {
        val targetId = originalMessage.path("target_id").safeString()
        val userId = originalMessage.path("user_id").safeString()

        val source = ReceiveMessage.Source(userId, SourceTypeEnum.PRIVATE)
        val target = NapCatBotApiSupport.getFriendInfo(targetId) ?: ReceiveMessage.User.empty().copy(id = targetId)
        val sender = NapCatBotApiSupport.getFriendInfo(userId) ?: ReceiveMessage.User.empty().copy(id = userId)
        val action = parseAction(originalMessage, target)

        return NudgeEvent(source, target, sender, action)
    }

    private suspend fun groupPoke(originalMessage: JsonNode): NudgeEvent {
        val targetId = originalMessage.path("target_id").safeString()
        val userId = originalMessage.path("user_id").safeString()
        val groupId = originalMessage.path("group_id").safeString()

        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val target = NapCatBotApiSupport.getGroupMemberInfo(groupId, targetId)
        val sender = NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val action = parseAction(originalMessage, target)

        return NudgeEvent(source, target, sender, action)
    }

    private fun parseAction(originalMessage: JsonNode, target: ReceiveMessage.User): String {
        try {
            val rawArray = Json.convertValue<ArrayNode>(originalMessage.path("raw_info"))
            return rawArray
                .filter { it.path("type").safeString() == "nor" }
                .joinToString(target.nickname) { it.path("txt").safeString() }
        } catch (_: Exception) {
            return ""
        }
    }

}
