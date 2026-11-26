package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群精华消息变更，原文不可查时仍保留消息序列号
 */
data class QQNativeGroupEssenceEvent(
    override val source: ReceiveMessage.Source,
    override val actor: ReceiveMessage.User,
    val messageId: String,
    val sequence: String,
    val sender: ReceiveMessage.User,
    val added: Boolean,
    val message: ReceiveMessage? = null
) : Plugin.Listener.Event
