package com.blr19c.falowp.bot.adapter.qq.op.channel

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 频道发送消息
 */
data class OpChannelSendMessage(
    /**
     * 消息内容
     */
    val content: OpChannelMessageContent,

    /**
     * 图片地址
     */
    val image: String?,

    /**
     * 引用消息对象
     */
    @field:JsonProperty("message_reference")
    val messageReference: Reference?,

    /**
     * 要回复的消息id
     */
    @field:JsonProperty("msg_id")
    val msgId: String?,
) {
    data class Reference(
        /**
         * 需要引用回复的消息 id
         */
        @field:JsonProperty("message_id")
        val messageId: String,

        /**
         * 是否忽略获取引用消息详情错误，默认否
         */
        @field:JsonProperty("ignore_get_message_error")
        val ignore: Boolean = false
    )
}