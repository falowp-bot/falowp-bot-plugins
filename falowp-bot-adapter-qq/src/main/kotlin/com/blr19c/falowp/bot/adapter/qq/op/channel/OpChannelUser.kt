package com.blr19c.falowp.bot.adapter.qq.op.channel

import java.net.URI

/**
 * 频道用户信息
 */
data class OpChannelUser(
    /**
     * 用户在频道中的id
     */
    val id: String,
    /**
     * 头像
     */
    val avatar: URI,
    /**
     * 用户名
     */
    val username: String,

    /**
     * 是否为机器人
     */
    val bot: Boolean,

    /**
     * 角色
     */
    val roles: List<String> = emptyList(),

    /**
     * 昵称
     */
    val nick: String = ""
)