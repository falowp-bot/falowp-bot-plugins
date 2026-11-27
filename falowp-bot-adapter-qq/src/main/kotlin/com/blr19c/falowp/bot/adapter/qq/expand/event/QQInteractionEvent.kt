package com.blr19c.falowp.bot.adapter.qq.expand.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 按钮、快捷菜单、反馈和授权等 QQ 交互事件
 */
data class QQInteractionEvent(
    /**
     * 交互发生的单聊或群聊
     */
    override val source: ReceiveMessage.Source,
    /**
     * 触发交互的用户，平台未提供身份时 ID 为空
     */
    override val actor: ReceiveMessage.User,
    /**
     * 交互响应使用的 d.id
     */
    val id: String,
    /**
     * 被动回复使用的外层事件 ID
     */
    val eventId: String,
    /**
     * 官方交互类型，如 11 按钮、12 菜单、18 用户授权、19 群授权
     */
    val type: Int,
    /**
     * 完整事件 d 对象，包含按钮数据、授权范围等字段
     */
    val data: JsonNode,
) : Plugin.Listener.Event
