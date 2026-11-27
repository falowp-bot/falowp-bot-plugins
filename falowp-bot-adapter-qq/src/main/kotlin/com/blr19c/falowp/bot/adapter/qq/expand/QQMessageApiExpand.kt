@file:Suppress("unused")

package com.blr19c.falowp.bot.adapter.qq.expand

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQReply
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQKeyboard
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import io.ktor.http.*
import tools.jackson.databind.JsonNode

/**
 * 发送原生 Markdown；可携带内嵌键盘，不混入文本 content 字段
 * @param content 非空的 Markdown 正文
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @param keyboard 内嵌键盘，null 表示不附带键盘
 * @param reference 是否引用当前消息，缺少引用索引时按普通消息发送
 * @param forceVerifyImageResource 是否校验 Markdown 图片转存结果，默认关闭
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.sendMarkdown(
    content: String,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
    keyboard: QQKeyboard? = null,
    reference: Boolean = false,
    forceVerifyImageResource: Boolean = false,
    reply: QQReply = QQReply.Auto,
): JsonNode {
    require(content.isNotBlank()) { "Markdown 内容不能为空" }
    val body = mutableMapOf(
        "msg_type" to 2, "markdown" to mapOf(
            "content" to content, "force_verify_image_resource" to forceVerifyImageResource
        )
    )
    keyboard?.let { body["keyboard"] = it.payload() }
    return sendPayload(body, sourceId, sourceType, reference, reply)
}

/**
 * 撤回机器人发送的消息，平台负责校验撤回时限及权限
 * @param messageId 需要撤回的机器人消息 ID
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.recallMessage(
    messageId: String,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
): JsonNode {
    require(messageId.isNotBlank()) { "消息 ID 不能为空" }
    return apiRequest(
        "${targetPath(sourceId, sourceType)}/messages/${messageId.encodeURLPathPart()}",
        method = HttpMethod.Delete
    )
}

/**
 * 发送文本，可显式指定主动消息或互动召回
 * @param content 非空文本正文
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.sendText(
    content: String,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
    reply: QQReply = QQReply.Auto,
): JsonNode {
    require(content.isNotBlank()) { "文本不能为空" }
    return sendPayload(mapOf("msg_type" to 0, "content" to content), sourceId, sourceType, false, reply)
}

/**
 * 展示单聊输入状态，持续时间为 1 至 60 秒
 * @param seconds 输入状态持续秒数，范围 1 至 60，默认 30
 * @param sourceId 目标用户 OpenID，群聊上下文中必须显式指定
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.sendTyping(
    seconds: Int = 30,
    sourceId: String = receiveMessage.source.id,
    reply: QQReply = QQReply.Auto,
): JsonNode {
    require(seconds in 1..60) { "输入状态持续时间必须为 1 至 60 秒" }
    require(receiveMessage.private() || sourceId != receiveMessage.source.id) { "输入状态仅支持单聊，请指定用户 OpenID" }
    return sendPayload(
        mapOf(
            "msg_type" to 6, "input_notify" to mapOf(
                "input_type" to 1, "input_second" to seconds
            )
        ), sourceId, SourceTypeEnum.PRIVATE, false, reply
    )
}

/**
 * 发送图文卡片
 * @param card 图文卡片内容
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.sendCard(
    card: com.blr19c.falowp.bot.adapter.qq.expand.model.QQCard,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
    reply: QQReply = QQReply.Auto,
): JsonNode = sendPayload(mapOf("msg_type" to 8, "card" to card.payload()), sourceId, sourceType, false, reply)
