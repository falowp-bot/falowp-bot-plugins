package com.blr19c.falowp.bot.adapter.qq.op.qq

import com.blr19c.falowp.bot.adapter.qq.op.OpAttachment
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * QQ事件消息
 */
data class OpQQReceiveMessage(
    /**
     * 消息id
     */
    val id: String,

    /**
     * 消息的发送人信息
     */
    val author: User,

    /**
     * 消息内容
     */
    val content: String,

    /**
     * 附加资源
     */
    val attachments: List<OpAttachment>?,

    /**
     * 群聊时存在-群聊id
     */
    @field:JsonProperty("group_openid")
    val groupId: String?
) {

    data class User(

        @JsonAlias(value = ["member_openid", "user_openid"])
        val id: String,
    )
}
