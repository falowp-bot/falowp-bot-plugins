package com.blr19c.falowp.bot.adapter.qq.core.config

/**
 * 本地群消息过滤模式，不替代 QQ 开放平台的事件订阅和接收权限
 */
internal enum class QQGroupMessageMode {
    /**
     * 接收普通群消息和 @机器人消息
     */
    ALL,

    /**
     * 仅接收 @机器人消息，包含全量事件中的显式 @
     */
    AT,
}
