package com.blr19c.falowp.bot.adapter.qq.op.qq

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * QQ发送消息
 */
data class OpQQSendMessage(
    /**
     * 消息id
     */
    @field:JsonProperty("msg_seq")
    val id: Int,

    /**
     * 消息类型
     */
    @field:JsonProperty("msg_type")
    val messageType: OpQQMessageTypeEnum,

    /**
     * 消息内容
     */
    val content: String,

    /**
     * 富媒体
     */
    val media: JsonNode?,

    /**
     * 引用消息对象
     */
    @field:JsonProperty("message_reference")
    val messageReference: Reference?,

    /**
     * 要回复的消息id
     */
    @field:JsonProperty("msg_id")
    val msgId: String?
) {
    data class Reference(
        /**
         * 需要引用回复的消息 id
         */
        @field:JsonProperty("message_id")
        val messageId: String,
    )
}