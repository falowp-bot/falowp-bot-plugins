package com.blr19c.falowp.bot.signin.vo

import java.time.LocalDateTime

/**
 * 签到记录
 */
data class SigninRecordVo(
    val id: Int,

    /**
     * 用户id
     */
    val userId: String,

    /**
     * 签到时间
     */
    val signinDate: String,

    /**
     * 创建时间
     */
    val createTime: LocalDateTime
)