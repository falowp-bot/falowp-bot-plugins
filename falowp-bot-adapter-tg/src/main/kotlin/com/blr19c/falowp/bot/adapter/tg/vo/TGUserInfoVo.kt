package com.blr19c.falowp.bot.adapter.tg.vo

data class TGUserInfoVo(
    val id: Int,
    /**
     * 用户id
     */
    val userId: Long,
    /**
     * 用户名
     */
    val userName: String,
    /**
     * 来源id
     */
    val sourceId: Long,
    /**
     * 来源类型
     */
    val sourceType: String,
)