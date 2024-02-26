package com.blr19c.falowp.bot.user.vo

import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.expand.ImageUrl
import java.math.BigDecimal

/**
 * 机器人用户信息
 */
data class BotUserVo(
    val id: Int,

    /**
     * 用户id
     */
    val userId: String,

    /**
     * 昵称
     */
    val nickname: String,

    /**
     * 头像url
     */
    val avatar: ImageUrl,

    /**
     * 权限
     */
    val auth: ApiAuth,

    /**
     * 好感度
     */
    val impression: BigDecimal,

    /**
     * 金币
     */
    val coins: BigDecimal,

    /**
     * 来源id
     */
    val sourceId: String,

    /**
     * 来源类型
     */
    val sourceType: String
)