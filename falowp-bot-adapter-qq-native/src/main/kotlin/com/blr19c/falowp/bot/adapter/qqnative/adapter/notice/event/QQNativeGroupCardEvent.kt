package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群名片变更
 */
data class QQNativeGroupCardEvent(
    override val source: ReceiveMessage.Source,
    override val actor: ReceiveMessage.User, val user: ReceiveMessage.User, val oldCard: String, val newCard: String
) : Plugin.Listener.Event
