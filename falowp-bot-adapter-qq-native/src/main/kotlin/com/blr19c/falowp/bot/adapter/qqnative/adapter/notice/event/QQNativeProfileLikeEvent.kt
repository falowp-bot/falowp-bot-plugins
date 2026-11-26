package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 资料点赞，time 为服务端秒级时间戳
 */
data class QQNativeProfileLikeEvent(
    override val source: ReceiveMessage.Source,
    override val actor: ReceiveMessage.User, val count: Int, val time: Long
) : Plugin.Listener.Event
