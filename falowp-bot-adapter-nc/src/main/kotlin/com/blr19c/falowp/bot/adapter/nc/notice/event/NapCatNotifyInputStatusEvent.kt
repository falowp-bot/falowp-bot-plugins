package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 输入状态更新
 */
data class NapCatNotifyInputStatusEvent(
    /**
     * 用户
     */
    val user: ReceiveMessage.User,
    /**
     * 来源
     */
    val source: ReceiveMessage.Source,
    /**
     * 类型
     */
    val type: String,
    /**
     * 状态文本 1-输入 2-取消输入
     */
    val statusText: String
) : Plugin.Listener.Event