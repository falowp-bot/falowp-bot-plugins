package com.blr19c.falowp.bot.adapter.cq.message

import com.blr19c.falowp.bot.system.api.SendMessage

data class CqJsonMessage(val message: String, val escape: Boolean) : SendMessage

data class CqFaceMessage(val faceId: Int) : SendMessage

data class CqMusicMessage(val type: String, val id: String) : SendMessage

data class CqCustomMusicMessage(
    val url: String,
    val audio: String,
    val title: String,
    val subtype: String,
    val image: String,
    val content: String,
) : SendMessage


/**
 * 发送[CQ:json信息]
 */
fun SendMessage.Builder.cqJsonMessage(message: String, escape: Boolean = true): SendMessage.Builder {
    this.messageList.addLast(CqJsonMessage(message, escape))
    return this
}

/**
 * 发送[CQ:face表情]
 * [face表情代码](https://github.com/kyubotics/coolq-http-api/wiki/%E8%A1%A8%E6%83%85-CQ-%E7%A0%81-ID-%E8%A1%A8)
 */
fun SendMessage.Builder.cqFaceMessage(faceId: Int): SendMessage.Builder {
    this.messageList.addLast(CqFaceMessage(faceId))
    return this
}

/**
 * 发送音乐分享
 *
 * @param type 类型 可选的值qq,163
 */
fun SendMessage.Builder.cqMusicMessage(type: String, id: String): SendMessage.Builder {
    this.messageList.addLast(CqMusicMessage(type, id))
    return this
}

/**
 * 发送自定义音乐分享
 *
 * @param url 点击后跳转地址
 * @param audio 音乐
 * @param title 标题
 * @param subtype 用于伪装的类型
 * @param image 封面
 * @param content 内容
 */
fun SendMessage.Builder.cqCustomMusicMessage(
    url: String,
    audio: String,
    title: String,
    subtype: String = "163",
    image: String = "",
    content: String = ""
): SendMessage.Builder {
    this.messageList.addLast(CqCustomMusicMessage(url, audio, title, subtype, image, content))
    return this
}