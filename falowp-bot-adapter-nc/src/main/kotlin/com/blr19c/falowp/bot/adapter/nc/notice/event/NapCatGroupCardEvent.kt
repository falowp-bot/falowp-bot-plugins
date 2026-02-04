package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群成员名片更新
 */
data class NapCatGroupCardEvent(
    /**
     * 群组
     */
    override val source: ReceiveMessage.Source,
    /**
     * 用户
     */
    override val actor: ReceiveMessage.User,
    /**
     * 新名片
     */
    val cardNew: String,
    /**
     * 旧名片
     */
    val cardOld: String
) : Plugin.Listener.Event