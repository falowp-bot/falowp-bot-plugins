package com.blr19c.falowp.bot.adapter.qq.expand

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.core.message.QQStreamSession
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQReply
import com.blr19c.falowp.bot.system.api.SourceTypeEnum

/**
 * 创建单聊流式回复，首次 append 或 replace 时发送首片
 * @param sourceId 目标用户 OpenID，群聊上下文中必须显式指定
 * @param markdown 是否使用 Markdown 格式，false 表示纯文本
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 流式会话，调用 append 或 replace 开始发送
 */
fun QQBotApi.stream(
    sourceId: String = receiveMessage.source.id,
    markdown: Boolean = true,
    reply: QQReply = QQReply.Auto,
): QQStreamSession {
    require(receiveMessage.private() || sourceId != receiveMessage.source.id) { "流式回复仅支持单聊，请指定用户 OpenID" }
    val path = "${targetPath(sourceId, SourceTypeEnum.PRIVATE)}/stream_messages"
    return QQStreamSession(this, path, replyFields(sourceId, SourceTypeEnum.PRIVATE, reply), markdown)
}
