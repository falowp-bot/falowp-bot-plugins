package com.blr19c.falowp.bot.adapter.cq.message

import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.api.TextSendMessage

/**
 * 发送[CQ:json信息]
 */
fun SendMessage.Builder.cqJsonMessage(message: String, escape: Boolean = true): SendMessage.Builder {
    val json = "[CQ:json,data=${replaceEscapeCharacter(message, escape)}]"
    this.messageList.addLast(TextSendMessage(json))
    return this
}

/**
 * 发送[CQ:face表情]
 * [face表情代码](https://github.com/kyubotics/coolq-http-api/wiki/%E8%A1%A8%E6%83%85-CQ-%E7%A0%81-ID-%E8%A1%A8)
 */
fun SendMessage.Builder.cqFaceMessage(faceId: Int): SendMessage.Builder {
    this.messageList.addLast(TextSendMessage("[CQ:face,id=${faceId}]"))
    return this
}


private fun replaceEscapeCharacter(cqMessage: String, escape: Boolean): String {
    if (!escape) return cqMessage
    return cqMessage.replace(",", "&#44;")
        .replace("[", "&#91;")
        .replace("]", "&#93;")
}