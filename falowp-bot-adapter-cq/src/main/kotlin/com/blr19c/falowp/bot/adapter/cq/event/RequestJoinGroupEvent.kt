package com.blr19c.falowp.bot.adapter.cq.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Event

/**
 * 申请进群事件
 */
data class RequestJoinGroupEvent(
    /**
     * 进群人
     */
    val userId: String,
    /**
     * 来源
     */
    val source: ReceiveMessage.Source,
    /**
     * 验证信息
     */
    val comment: String,
    /**
     * 进群标识
     */
    val flag: String,
    /**
     * 进群类型
     */
    val subType: String,
) : Event