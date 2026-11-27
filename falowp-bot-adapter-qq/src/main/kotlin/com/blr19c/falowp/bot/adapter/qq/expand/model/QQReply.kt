package com.blr19c.falowp.bot.adapter.qq.expand.model

/**
 * 消息发送使用的回复模式
 */
sealed interface QQReply {
    /**
     * 沿用当前消息或事件，没有可用上下文时主动发送
     */
    data object Auto : QQReply

    /**
     * 主动发送，不携带消息或事件回复 ID
     */
    data object Active : QQReply

    /**
     * 发送互动召回消息，使用平台的召回额度
     */
    data object Wakeup : QQReply

    /**
     * 回复指定消息
     */
    data class Message(
        /**
         * 入站消息的 d.id，用于 msg_id
         */
        val id: String,
    ) : QQReply

    /**
     * 回复指定事件
     */
    data class Event(
        /**
         * 外层事件 ID，用于 event_id，不是交互响应使用的 d.id
         */
        val id: String,
    ) : QQReply
}
