package com.blr19c.falowp.bot.adapter.qqnative.core.contact

/**
 * 区分群聊与私聊，避免相同数字标识对应不同会话
 */
sealed interface QQNativeConversation {
    val id: String

    data class Group(override val id: String) : QQNativeConversation
    data class Private(override val id: String) : QQNativeConversation
}

/**
 * 查询当前账号的群列表及会话可用性
 */
interface QQNativeContacts {
    /**
     * 获取当前账号加入的群
     */
    suspend fun groups(): List<QQNativeConversation.Group>

    /**
     * 判断群或好友是否属于当前账号
     */
    suspend fun contains(id: String): Boolean
}
