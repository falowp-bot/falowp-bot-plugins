package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群禁言变更，成员为 0 时表示全员，duration 为秒，-1 表示全员禁言
 */
data class QQNativeGroupMuteEvent(
    override val source: ReceiveMessage.Source,
    override val actor: ReceiveMessage.User, val user: ReceiveMessage.User, val duration: Long
) : Plugin.Listener.Event
