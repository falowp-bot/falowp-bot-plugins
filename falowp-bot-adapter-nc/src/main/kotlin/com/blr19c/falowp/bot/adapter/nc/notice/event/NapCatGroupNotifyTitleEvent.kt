package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群成员头衔变更
 */
data class NapCatGroupNotifyTitleEvent(
    /**
     * 群组
     */
    override val source: ReceiveMessage.Source,
    /**
     * 用户
     */
    val user: ReceiveMessage.User,
    /**
     * 群头衔
     */
    val title: String
) : Plugin.Listener.Event