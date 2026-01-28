package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin
import kotlin.time.Duration

/**
 * 群聊禁言
 */
data class NapCatGroupBanEvent(
    /**
     * 群组
     */
    val source: ReceiveMessage.Source,
    /**
     * 用户
     */
    val user: ReceiveMessage.User,
    /**
     * 操作人
     */
    val operator: ReceiveMessage.User,
    /**
     * 持续时间
     */
    val duration: Duration,
    /**
     * 变动类型 ban-禁言 lift_ban-取消禁言
     */
    val type: String
) : Plugin.Listener.Event