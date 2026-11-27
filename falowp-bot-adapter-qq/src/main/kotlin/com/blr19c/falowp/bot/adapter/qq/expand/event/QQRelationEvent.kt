package com.blr19c.falowp.bot.adapter.qq.expand.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 好友、群关系和主动消息开关变化
 */
data class QQRelationEvent(
    /**
     * 关系或消息开关发生变化的单聊或群聊
     */
    override val source: ReceiveMessage.Source,
    /**
     * 触发变化的用户，平台未提供身份时 ID 为空
     */
    override val actor: ReceiveMessage.User,
    /**
     * 官方事件名，如 FRIEND_ADD、GROUP_DEL_ROBOT、C2C_MSG_REJECT
     */
    val type: String,
    /**
     * 外层事件 ID，仅部分事件支持被动回复
     */
    val eventId: String,
    /**
     * 完整事件 d 对象
     */
    val data: JsonNode,
) : Plugin.Listener.Event
