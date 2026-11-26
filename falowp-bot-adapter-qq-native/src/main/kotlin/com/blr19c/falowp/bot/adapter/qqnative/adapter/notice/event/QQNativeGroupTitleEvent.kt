package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群成员头衔变更
 */
data class QQNativeGroupTitleEvent(
    override val source: ReceiveMessage.Source,
    override val actor: ReceiveMessage.User, val user: ReceiveMessage.User, val title: String
) : Plugin.Listener.Event
