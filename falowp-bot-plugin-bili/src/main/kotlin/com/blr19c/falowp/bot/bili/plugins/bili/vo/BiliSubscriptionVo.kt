package com.blr19c.falowp.bot.bili.plugins.bili.vo

/**
 * b站订阅
 */
data class BiliSubscriptionVo(
    val id: Int,
    /**
     * b站id
     */
    val mid: String,

    /**
     * 来源id
     */
    val sourceId: String,

    /**
     * 来源类型
     */
    val sourceType: String
)