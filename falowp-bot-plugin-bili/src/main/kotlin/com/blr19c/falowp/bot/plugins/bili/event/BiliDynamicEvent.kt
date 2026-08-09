package com.blr19c.falowp.bot.plugins.bili.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * UP 主发布新动态时触发
 */
data class BiliDynamicEvent(
    val mid: String,
    val name: String,
    val dynamicId: String,
    val dynamicType: String,
    override val source: ReceiveMessage.Source = ReceiveMessage.Source.system(),
    override val actor: ReceiveMessage.User = ReceiveMessage.User.empty(),
) : Plugin.Listener.Event
