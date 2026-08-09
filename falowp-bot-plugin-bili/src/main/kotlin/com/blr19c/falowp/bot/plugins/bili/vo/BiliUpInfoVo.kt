package com.blr19c.falowp.bot.plugins.bili.vo

/**
 * 订阅中的 UP 主信息
 */
data class BiliUpInfoVo(
    val id: Int,

    /**
     * UP 主 UID
     */
    val mid: String,

    /**
     * 直播间 ID
     */
    val roomId: String,

    /**
     * UP 主昵称
     */
    val name: String,

    /**
     * 上次检查时是否开播
     */
    val liveStatus: Boolean,
)
