package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 点赞
 */
data class NapCatNotifyProfileLikeNoticeEvent(
    /**
     * 来源
     */
    override val source: ReceiveMessage.Source,
    /**
     * 点赞用户
     */
    val operator: ReceiveMessage.User,
    /**
     * 点赞数量
     */
    val count: Int
) : Plugin.Listener.Event