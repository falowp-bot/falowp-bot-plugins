package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群聊表情回应
 */
data class NapCatGroupMsgEmojiLikeEvent(
    /**
     * 群组
     */
    val source: ReceiveMessage.Source,
    /**
     * 用户
     */
    val user: ReceiveMessage.User,
    /**
     * 消息
     */
    val message: ReceiveMessage,
    /**
     * 贴表情信息
     */
    val likes: List<Emoji>,
    /**
     * 添加/取消 true-添加 false-取消
     */
    val add: Boolean
) : Plugin.Listener.Event {


    data class Emoji(
        /**
         * 表情
         */
        val emoji: ReceiveMessage.Emoji,
        /**
         * 此表情数量
         */
        val count: Int
    )
}