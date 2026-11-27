package com.blr19c.falowp.bot.adapter.qq.expand

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQReply
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.adapter.qq.api.QQBotApiSupport
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import io.ktor.http.*
import tools.jackson.databind.JsonNode

/**
 * QQ 专有接口入口，共享鉴权及业务错误处理
 * @param path 以 / 开头的 API 相对路径，不包含域名
 * @param body JSON 请求体，null 表示不发送请求体
 * @param method HTTP 请求方法，默认 POST
 * @return 平台原始 JSON 响应
 */
@Suppress("UnusedReceiverParameter") // 保持 botApi.qq 扩展入口一致
suspend fun QQBotApi.apiRequest(path: String, body: Any? = null, method: HttpMethod = HttpMethod.Post): JsonNode {
    require(path.startsWith("/") && !path.startsWith("//")) { "QQ API 必须使用相对路径" }
    return QQBotApiSupport.client.request(path, body, method)
}

/**
 * 构建单聊或群聊目标路径，并验证会话类型
 */
internal fun targetPath(sourceId: String, sourceType: SourceTypeEnum): String {
    require(sourceId.isNotBlank()) { "QQ 消息目标不能为空" }
    require(sourceType == SourceTypeEnum.GROUP || sourceType == SourceTypeEnum.PRIVATE) { "QQ 仅支持群聊和单聊" }
    return "/v2/${if (sourceType == SourceTypeEnum.GROUP) "groups" else "users"}/${sourceId.encodeURLPathPart()}"
}

/**
 * 所有发送方式共用回复上下文及序号，避免扩展发送与通用发送冲突
 * @param reference 是否以引用形式展示当前消息
 */
internal suspend fun QQBotApi.sendPayload(
    fields: Map<String, Any>, sourceId: String, sourceType: SourceTypeEnum, reference: Boolean,
    reply: QQReply = QQReply.Auto,
): JsonNode {
    val body = fields + replyFields(sourceId, sourceType, reply)
    val referenceFields =
        if (reference && body["msg_id"] == receiveMessage.id && receiveMessage.id.isNotBlank()) referenceId()?.let {
            mapOf("message_reference" to mapOf("message_id" to it))
        }.orEmpty() else emptyMap()
    return apiRequest("${targetPath(sourceId, sourceType)}/messages", body + referenceFields)
}

/**
 * 统一消息回复、事件回复和主动发送的序号
 */
internal fun QQBotApi.replyFields(target: String, type: SourceTypeEnum, reply: QQReply): Map<String, Any> {
    val selected = if (reply == QQReply.Auto) automaticReply(target, type) else reply
    return when (selected) {
        is QQReply.Message -> replySequence("msg_id", selected.id)
        is QQReply.Event -> replySequence("event_id", selected.id)
        QQReply.Wakeup -> mapOf("is_wakeup" to true)
        else -> emptyMap()
    }
}

private fun QQBotApi.automaticReply(target: String, type: SourceTypeEnum): QQReply {
    replyId(target, type)?.let { return QQReply.Message(it) }
    if (target != receiveMessage.source.id || type != receiveMessage.source.type) return QQReply.Active
    val event = receiveMessage.adapter.data as? JsonNode ?: return QQReply.Active
    val supported = if (type == SourceTypeEnum.GROUP) {
        setOf("INTERACTION_CREATE", "GROUP_ADD_ROBOT", "GROUP_MSG_RECEIVE")
    } else setOf("INTERACTION_CREATE", "FRIEND_ADD", "C2C_MSG_RECEIVE")
    val id = event.path("id").safeString()
    return if (event.path("t").safeString() in supported && id.isNotBlank()) QQReply.Event(id) else QQReply.Active
}

private fun replySequence(field: String, id: String): Map<String, Any> {
    require(id.isNotBlank()) { "回复 ID 不能为空" }
    val key = if (field == "msg_id") "reply:$id" else "event:$id"
    return mapOf(field to id, "msg_seq" to QQBotApiSupport.sequences.next(key))
}
