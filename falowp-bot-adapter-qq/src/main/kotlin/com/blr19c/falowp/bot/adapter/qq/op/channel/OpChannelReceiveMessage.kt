package com.blr19c.falowp.bot.adapter.qq.op.channel

import com.blr19c.falowp.bot.adapter.qq.op.OpAttachment
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.time.LocalDateTime

/**
 * 频道事件消息
 */
data class OpChannelReceiveMessage(
    /**
     * 消息id
     */
    val id: String,

    /**
     * (子)频道消息序号
     */
    @field:JsonProperty("seq_in_channel")
    val seq: Int,

    /**
     * 频道id
     */
    @field:JsonProperty("guild_id")
    val guildId: String,

    /**
     * (子)频道id
     */
    @field:JsonProperty("channel_id")
    val channelId: String,

    /**
     * 消息内容
     */
    val content: OpChannelMessageContent,

    /**
     * 是否私信
     */
    @field:JsonProperty("direct_message")
    val directMessage: Boolean?,

    /**
     * 附加资源
     */
    val attachments: List<OpAttachment>?,

    /**
     * 消息的发送人信息
     */
    val author: User,

    /**
     * 消息的发送人的频道会员信息
     */
    val member: Member
) {

    data class User(
        /**
         * 头像
         */
        val avatar: URI,

        /**
         * 是否为机器人
         */
        val bot: Boolean,

        /**
         * 用户在频道中的id
         */
        val id: String,

        /**
         * 用户昵称
         */
        val username: String,
    )

    data class Member(
        /**
         * 加入频道时间
         */
        @field:JsonProperty("joined_at")
        val joinedAt: LocalDateTime,

        /**
         * 用户的昵称
         */
        val nick: String?,

        /**
         * 在频道的身份
         */
        val roles: List<String>?,
    )
}



