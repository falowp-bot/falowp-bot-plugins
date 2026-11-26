package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 私聊输入状态
 */
data class QQNativeInputStatusEvent(
    override val source: ReceiveMessage.Source,
    override val actor: ReceiveMessage.User, val status: Int, val text: String
) : Plugin.Listener.Event
