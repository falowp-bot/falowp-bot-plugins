package com.blr19c.falowp.bot.adapter.qq.op

/**
 * 事件消息
 */
data class OpReceiveMessage<T>(
    /**
     * 消息类型
     */
    val op: OpCodeEnum,

    /**
     * 序列号
     */
    val s: Int,

    /**
     * 事件类型
     */
    val t: OpTypeEnum,

    /**
     * 事件id
     */
    val id: String,

    /**
     * 内容
     */
    val d: T
)