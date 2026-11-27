package com.blr19c.falowp.bot.adapter.qq.expand

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQMedia
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQReply
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQMediaType
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import java.util.Base64
import tools.jackson.databind.JsonNode

/**
 * URL 上传：平台下载并转存资源，不立即发送消息
 * @param url 平台可访问的 HTTP(S) 资源地址
 * @param type 文件类型，决定平台的展示方式
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @return 上传结果，包含文件令牌、目标和有效期
 */
suspend fun QQBotApi.uploadMedia(
    url: String,
    type: QQMediaType = QQMediaType.IMAGE,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
): QQMedia {
    val uri = java.net.URI(url)
    require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) { "QQ 富媒体需要 HTTP(S) URL" }
    return uploadMediaPayload(mapOf("url" to url), type, sourceId, sourceType)
}

/**
 * 直传小文件，不依赖公网 URL
 * @param data 非空且小于 5 MiB 的原始文件字节
 * @param fileName 包含扩展名的文件名
 * @param type 文件类型，决定平台的展示方式
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @return 上传结果，包含文件令牌、目标和有效期
 */
suspend fun QQBotApi.uploadMedia(
    data: ByteArray,
    fileName: String,
    type: QQMediaType = QQMediaType.IMAGE,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
): QQMedia {
    require(data.isNotEmpty() && data.size < 5 * 1024 * 1024) { "QQ 二进制直传要求文件非空且小于 5 MiB" }
    require(fileName.isNotBlank()) { "QQ 二进制直传必须提供文件名" }
    return uploadMediaPayload(
        mapOf("file_data" to Base64.getEncoder().encodeToString(data), "file_name" to fileName),
        type, sourceId, sourceType
    )
}

internal suspend fun QQBotApi.uploadMediaPayload(
    fields: Map<String, Any>, type: QQMediaType, sourceId: String, sourceType: SourceTypeEnum,
): QQMedia {
    val startedAt = System.currentTimeMillis()
    val result = apiRequest(
        "${targetPath(sourceId, sourceType)}/files",
        fields + mapOf("file_type" to type.code, "srv_send_msg" to false)
    )
    val info = result.path("file_info").safeString()
    check(info.isNotBlank()) { "QQ 上传响应缺少 file_info" }
    val ttl = result.path("ttl").safeString().toLongOrNull()
    check(ttl != null && ttl >= 0) { "QQ 上传响应缺少有效 ttl" }
    return QQMedia(
        info, result.path("file_uuid").safeString(), sourceId, sourceType,
        if (ttl == 0L) null else Math.addExact(startedAt, Math.multiplyExact(ttl, 1000L)), result
    )
}

/**
 * 发送已上传的资源，可附带文本
 * @param media 已上传的资源，必须未过期且绑定同一目标
 * @param content 随资源发送的文本，默认不附带文本
 * @param sourceId 目标 OpenID，默认使用资源绑定的目标
 * @param sourceType 目标会话类型，必须与资源绑定的类型一致
 * @param reference 是否引用当前消息，缺少引用索引时按普通消息发送
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.sendMedia(
    media: QQMedia,
    content: String = "",
    sourceId: String = media.sourceId,
    sourceType: SourceTypeEnum = media.sourceType,
    reference: Boolean = false,
    reply: QQReply = QQReply.Auto,
): JsonNode {
    require(media.sourceId == sourceId && media.sourceType == sourceType) { "QQ 富媒体不能跨目标复用，请重新上传" }
    require(media.fileInfo.isNotBlank()) { "file_info 不能为空" }
    require(media.expiresAt == null || System.currentTimeMillis() < media.expiresAt) { "QQ 富媒体已过期，请重新上传" }
    val body = mutableMapOf("msg_type" to 7, "media" to mapOf("file_info" to media.fileInfo))
    if (content.isNotEmpty()) body["content"] = content
    return sendPayload(body, sourceId, sourceType, reference, reply)
}

/**
 * 上传并发送 URL 资源
 * @param url 平台可访问的 HTTP(S) 资源地址
 * @param type 文件类型，决定平台的展示方式
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @param content 随资源发送的文本，默认不附带文本
 * @param reference 是否引用当前消息，缺少引用索引时按普通消息发送
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.sendMedia(
    url: String,
    type: QQMediaType = QQMediaType.IMAGE,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
    content: String = "",
    reference: Boolean = false,
    reply: QQReply = QQReply.Auto,
): JsonNode = sendMedia(uploadMedia(url, type, sourceId, sourceType), content, reference = reference, reply = reply)

/**
 * 上传并发送原始文件字节
 * @param data 非空且小于 5 MiB 的原始文件字节
 * @param fileName 包含扩展名的文件名
 * @param type 文件类型，决定平台的展示方式
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @param content 随资源发送的文本，默认不附带文本
 * @param reference 是否引用当前消息，缺少引用索引时按普通消息发送
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.sendMedia(
    data: ByteArray,
    fileName: String,
    type: QQMediaType = QQMediaType.IMAGE,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
    content: String = "",
    reference: Boolean = false,
    reply: QQReply = QQReply.Auto,
): JsonNode =
    sendMedia(uploadMedia(data, fileName, type, sourceId, sourceType), content, reference = reference, reply = reply)
