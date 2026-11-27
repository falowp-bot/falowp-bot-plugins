@file:Suppress("unused")

package com.blr19c.falowp.bot.adapter.qq.expand

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.api.QQBotApiSupport
import com.blr19c.falowp.bot.adapter.qq.core.media.QQChunkUploader
import com.blr19c.falowp.bot.adapter.qq.expand.model.*
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import tools.jackson.databind.JsonNode
import java.nio.file.Path

/**
 * 分片上传本地文件，不依赖公网存储
 * @param file 本地文件路径，非空且不超过 200 MiB，上传期间不可修改
 * @param type 文件类型，决定平台的展示方式
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @return 上传结果，包含文件令牌、目标和有效期
 */
suspend fun QQBotApi.uploadMedia(
    file: Path,
    type: QQMediaType = QQMediaType.FILE,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
): QQMedia {
    val target = targetPath(sourceId, sourceType)
    val id = QQChunkUploader(QQBotApiSupport.client).upload(file, target, type.code)
    return uploadMediaPayload(mapOf("upload_id" to id), type, sourceId, sourceType)
}

/**
 * 上传并发送本地文件
 * @param file 本地文件路径，非空且不超过 200 MiB，上传期间不可修改
 * @param type 文件类型，决定平台的展示方式
 * @param sourceId 目标用户或群 OpenID，默认使用当前会话
 * @param sourceType 目标会话类型，仅支持 PRIVATE 和 GROUP
 * @param content 随资源发送的文本，默认不附带文本
 * @param reference 是否引用当前消息，缺少引用索引时按普通消息发送
 * @param reply 回复模式，默认沿用当前消息或事件
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.sendMedia(
    file: Path,
    type: QQMediaType = QQMediaType.FILE,
    sourceId: String = receiveMessage.source.id,
    sourceType: SourceTypeEnum = receiveMessage.source.type,
    content: String = "",
    reference: Boolean = false,
    reply: QQReply = QQReply.Auto,
): JsonNode = sendMedia(uploadMedia(file, type, sourceId, sourceType), content, reference = reference, reply = reply)
