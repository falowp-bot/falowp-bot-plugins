package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event

import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativeReaction
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群消息表情回应，原文不可查时仍保留消息序列号
 */
data class QQNativeMessageReactionEvent(
    override val source: ReceiveMessage.Source,
    override val actor: ReceiveMessage.User,
    val messageId: String,
    val sequence: String,
    val reactions: List<QQNativeReaction>,
    val added: Boolean,
    val message: ReceiveMessage? = null
) : Plugin.Listener.Event
