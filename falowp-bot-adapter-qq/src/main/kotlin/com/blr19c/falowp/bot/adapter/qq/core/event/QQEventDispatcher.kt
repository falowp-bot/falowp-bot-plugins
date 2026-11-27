package com.blr19c.falowp.bot.adapter.qq.core.event

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.api.QQBotApiSupport
import com.blr19c.falowp.bot.adapter.qq.expand.event.QQInteractionEvent
import com.blr19c.falowp.bot.adapter.qq.expand.event.QQRelationEvent
import com.blr19c.falowp.bot.adapter.qq.core.state.QQState
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.event.EventManager
import tools.jackson.databind.JsonNode

/**
 * 将 QQ 通知发布到框架的事件订阅机制
 */
internal object QQEventDispatcher {
    private val received = QQState()
    private val relations = setOf(
        "FRIEND_ADD", "FRIEND_DEL", "C2C_MSG_REJECT", "C2C_MSG_RECEIVE",
        "GROUP_ADD_ROBOT", "GROUP_DEL_ROBOT", "GROUP_MSG_REJECT", "GROUP_MSG_RECEIVE"
    )

    fun dispatch(
        raw: JsonNode, publish: (QQBotApi, Plugin.Listener.Event) -> Unit = { bot, event ->
            EventManager.publishEvent(bot, event)
        }
    ): Boolean {
        val type = raw.path("t").safeString()
        if (type != "INTERACTION_CREATE" && type !in relations) return false
        val data = raw.path("d")
        if (data.path("scene").safeString() == "guild" || data.path("chat_type").asInt(-1) == 0) return true
        val source = source(data)
        if (source.id.isBlank()) return true
        val event = convert(raw, source)
        val eventId = raw.path("id").safeString().ifBlank { data.path("id").safeString() }
        val key = "$type:${source.id}:$eventId"
        if (eventId.isNotBlank() && received.next(key) != 1) return true
        val context = ReceiveMessage.empty().copy(
            source = source, sender = event.actor,
            self = QQBotApiSupport.botSelf, adapter = ReceiveMessage.Adapter("QQ", raw)
        )
        try {
            publish(QQBotApi(context, QQBotApi::class), event)
        } catch (e: Exception) {
            if (eventId.isNotBlank()) received.forget(key)
            throw e
        }
        return true
    }

    private fun convert(raw: JsonNode, source: ReceiveMessage.Source): Plugin.Listener.Event {
        val data = raw.path("d")
        val actor = ReceiveMessage.User.empty().copy(
            id = data.path("group_member_openid").safeString()
                .ifBlank { data.path("op_member_openid").safeString() }.ifBlank { userId(data) })
        val eventId = raw.path("id").safeString()
        val type = raw.path("t").safeString()
        return if (type == "INTERACTION_CREATE") QQInteractionEvent(
            source, actor, data.path("id").safeString(), eventId,
            data.path("type").asInt(data.path("data").path("type").asInt()), data
        ) else QQRelationEvent(source, actor, type, eventId, data)
    }

    private fun source(data: JsonNode): ReceiveMessage.Source {
        val group = data.path("group_openid").safeString()
        return if (group.isNotBlank()) ReceiveMessage.Source(group, SourceTypeEnum.GROUP)
        else ReceiveMessage.Source(userId(data), SourceTypeEnum.PRIVATE)
    }

    private fun userId(data: JsonNode): String = data.path("user_openid").safeString()
        .ifBlank { data.path("openid").safeString() }
}
