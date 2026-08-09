package com.blr19c.falowp.bot.plugins.bili.event

import com.blr19c.falowp.bot.plugins.bili.api.data.BiliRoomInfo
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * UP 主下播时触发
 */
data class BiliLiveEndEvent(
    val roomInfo: BiliRoomInfo,
    override val source: ReceiveMessage.Source = ReceiveMessage.Source.system(),
    override val actor: ReceiveMessage.User = ReceiveMessage.User.empty(),
) : Plugin.Listener.Event
