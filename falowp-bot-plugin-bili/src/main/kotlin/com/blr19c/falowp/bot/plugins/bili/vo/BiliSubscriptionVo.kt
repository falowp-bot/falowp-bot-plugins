package com.blr19c.falowp.bot.plugins.bili.vo

/**
 * 一条 B 站订阅
 */
data class BiliSubscriptionVo(
    val id: Int,
    /**
     * UP 主 UID
     */
    val mid: String,

    /**
     * 接收推送的好友或群 ID
     */
    val sourceId: String,

    /**
     * 接收推送的会话类型
     */
    val sourceType: String
)
