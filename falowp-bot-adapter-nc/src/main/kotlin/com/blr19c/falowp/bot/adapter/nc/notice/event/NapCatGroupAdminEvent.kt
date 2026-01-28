package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群聊管理员变动
 */
data class NapCatGroupAdminEvent(
    /**
     * 群组
     */
    val source: ReceiveMessage.Source,
    /**
     * 用户
     */
    val user: ReceiveMessage.User,
    /**
     * 变动类型 set-设置为管理员 unset-取消管理员
     */
    val type: String
) : Plugin.Listener.Event