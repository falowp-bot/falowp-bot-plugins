package com.blr19c.falowp.bot.plugins.signin.vo

import java.time.LocalDate

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
    val signinDate: LocalDate,
)
