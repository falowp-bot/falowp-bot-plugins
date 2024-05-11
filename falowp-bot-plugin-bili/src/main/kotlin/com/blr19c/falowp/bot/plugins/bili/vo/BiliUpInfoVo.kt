package com.blr19c.falowp.bot.plugins.bili.vo

/**
 * b站up主信息
 */
data class BiliUpInfoVo(
    val id: Int,

    /**
     * b站id
     */
    val mid: String,

    /**
     * 直播id
     */
    val roomId: String,

    /**
     * up名称
     */
    val name: String,

    /**
     * 直播状态
     */
    val liveStatus: Boolean,
)