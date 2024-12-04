package com.blr19c.falowp.bot.plugins.bili.message

import com.blr19c.falowp.bot.plugins.bili.vo.BiliUpInfoVo
import com.blr19c.falowp.bot.system.api.SendMessage

/**
 * b站推送消息
 */
data class BiliMessage(
    val uid: String,
    val url: String,
    val itemId: String
) : SendMessage


fun SendMessage.Builder.biliMessage(biliUpInfoVo: BiliUpInfoVo, url: String, itemId: String): SendMessage.Builder {
    this.messageList.add(BiliMessage(biliUpInfoVo.mid, url, itemId))
    return this
}