package com.blr19c.falowp.bot.adapter.qqnative.core.session

/**
 * 登录会话的当前阶段
 */
enum class QQNativeSessionPhase { PREPARING, WAITING_QR, SCAN, CONNECTING, ONLINE, ERROR, RETRYING }

/**
 * 提供给管理界面的登录状态快照
 */
data class QQNativeStatus(
    val phase: QQNativeSessionPhase,
    val message: String,
    val qr: String? = null,
    val accountId: String? = null,
    val nickname: String? = null
)
