package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群文件上传，fileId 是可用于群文件管理的原生标识
 */
data class QQNativeGroupFileUploadEvent(
    override val source: ReceiveMessage.Source,
    override val actor: ReceiveMessage.User,
    val fileId: String,
    val name: String,
    val size: Long,
    val busId: Long,
    val messageId: String,
    val url: String? = null
) : Plugin.Listener.Event
