package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 输入状态更新
 */
data class NapCatNotifyInputStatusEvent(
    /**
     * 来源
     */
    override val source: ReceiveMessage.Source,
    /**
     * 用户
     */
    override val actor: ReceiveMessage.User,
    /**
     * 类型
     */
    val type: String,
    /**
     * 状态文本 1-输入 2-取消输入
     */
    val statusText: String
) : Plugin.Listener.Event