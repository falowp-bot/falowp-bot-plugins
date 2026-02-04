package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群聊设精
 */
data class NapCatGroupEssenceEvent(
    /**
     * 群组
     */
    override val source: ReceiveMessage.Source,
    /**
     * 操作人
     */
    override val actor: ReceiveMessage.User,
    /**
     * 用户
     */
    val user: ReceiveMessage.User,
    /**
     * 消息
     */
    val message: ReceiveMessage,
    /**
     * 变动类型 add-设置为精华消息
     */
    val type: String
) : Plugin.Listener.Event