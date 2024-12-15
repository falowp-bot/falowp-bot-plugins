package com.blr19c.falowp.bot.adapter.cq.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Event

/**
 * 申请添加好友事件
 */
data class RequestAddFriendEvent(
    /**
     * 申请人
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
     * 加好友标识
     */
    val flag: String
) : Event