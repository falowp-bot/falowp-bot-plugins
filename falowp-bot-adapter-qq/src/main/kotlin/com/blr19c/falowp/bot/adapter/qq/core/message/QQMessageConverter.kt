package com.blr19c.falowp.bot.adapter.qq.core.message

import com.blr19c.falowp.bot.adapter.qq.core.config.QQGroupMessageMode
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.safeString
import java.net.URI
import tools.jackson.databind.JsonNode

/**
 * 将 QQ 事件转换为框架消息
 */
internal object QQMessageConverter {
    /**
     * 忽略非消息事件和不符合模式的群消息
     */
    internal fun convert(
        event: JsonNode, self: BotSelf, groupMode: QQGroupMessageMode = QQGroupMessageMode.ALL,
    ): ReceiveMessage? {
        val type = event.path("t").safeString()
        if (type !in setOf("C2C_MESSAGE_CREATE", "GROUP_AT_MESSAGE_CREATE", "GROUP_MESSAGE_CREATE")) return null
        val data = event.path("d")
        val isPrivate = type == "C2C_MESSAGE_CREATE"
        val sender = user(data.path("author"), isPrivate)
        val source = ReceiveMessage.Source(
            if (isPrivate) sender.id else data.path("group_openid").safeString(),
            if (isPrivate) SourceTypeEnum.PRIVATE else SourceTypeEnum.GROUP,
        )
        val id = data.path("id").safeString()
        require(id.isNotBlank() && sender.id.isNotBlank() && source.id.isNotBlank()) { "QQ 消息缺少 ID/OpenID" }
        val mentions = readMentions(data, type, self)
        if (!isPrivate && groupMode == QQGroupMessageMode.AT && mentions.users.none { it.id == self.id }) return null
        val content = readContent(data).copy(
            message = normalizeText(data.path("content").safeString(), mentions.selfIds),
            at = mentions.users,
            reference = { referenced(data, source, self) },
        )
        return ReceiveMessage(
            id,
            content.toMessageType(),
            content,
            sender,
            source,
            self,
            ReceiveMessage.Adapter("QQ", event)
        )
    }

    private fun readContent(data: JsonNode): ReceiveMessage.Content {
        val ark = data.path("ark_data")
        val fields = ark.path("fields")
        val shares = if (ark.isObject) listOf(
            ReceiveMessage.Share(
                ark.path("ark_type").safeString(), "", fields.path("source").safeString(),
                fields.path("title").safeString().ifBlank { ark.path("prompt").safeString() },
                ImageUrl(fields.path("preview").safeString()), fields.path("jump_url").safeString()
            )
        ) else emptyList()
        return readAttachments(data).copy(message = data.path("content").safeString(), share = shares)
    }

    /**
     * 引用快照没有原作者和消息 ID，不将引用索引冒充可回复的消息 ID
     */
    private fun referenced(data: JsonNode, source: ReceiveMessage.Source, self: BotSelf): ReceiveMessage? {
        val index = data.path("message_scene").path("ext").asSequence().map { it.safeString() }
            .firstOrNull { it.startsWith("ref_msg_idx=") }?.removePrefix("ref_msg_idx=")
        val elements = data.path("msg_elements")
        val element = if (index != null) findElement(elements, index, 0)
        else if (data.path("message_type").asInt() == 103) elements.firstOrNull() else null
        element ?: return null
        val content = readContent(element)
        return ReceiveMessage.empty().copy(
            content = content, messageType = content.toMessageType(),
            source = source, self = self, adapter = ReceiveMessage.Adapter("QQ", element)
        )
    }

    private fun findElement(elements: JsonNode, index: String, depth: Int): JsonNode? {
        if (depth >= 16) return null
        for (element in elements) {
            if (element.path("msg_idx").safeString() == index) return element
            findElement(element.path("msg_elements"), index, depth + 1)?.let { return it }
        }
        return null
    }

    /**
     * 解析图片、语音、视频和文件附件
     */
    private fun readAttachments(data: JsonNode): ReceiveMessage.Content {
        var content = ReceiveMessage.Content.empty()
        data.path("attachments").forEach { attachment ->
            val rawUrl = attachment.path("url").safeString()
            if (rawUrl.isNotBlank()) {
                val url = if (rawUrl.startsWith("//")) "https:$rawUrl" else rawUrl
                content = appendAttachment(content, attachment, url)
            }
        }
        return content
    }

    private fun appendAttachment(
        content: ReceiveMessage.Content,
        attachment: JsonNode,
        url: String
    ): ReceiveMessage.Content {
        val mime = attachment.path("content_type").safeString()
        val name = attachment.path("filename").safeString()
        val size = attachment.path("size").asLong(0)
        return when {
            mime.startsWith("image/") -> content.copy(image = content.image + ImageUrl(url))
            mime.startsWith("audio/") || mime == "voice" -> content.copy(voice = ReceiveMessage.Voice(name, URI(url)))
            mime.startsWith("video/") -> content.copy(
                video = ReceiveMessage.Video(
                    name,
                    ImageUrl.empty(),
                    URI(url),
                    size
                )
            )

            else -> content.copy(file = content.file + ReceiveMessage.File(name, mime, name, URI(url), size))
        }
    }

    /**
     * 将平台的机器人 OpenID 统一映射为当前 self.id
     */
    private fun readMentions(data: JsonNode, type: String, self: BotSelf): Mentions {
        val users = mutableListOf<ReceiveMessage.User>()
        val selfIds = mutableSetOf(self.id)
        data.path("mentions").forEach { node ->
            val mentioned = if (node.path("is_you").asBoolean(false)) {
                listOf("id", "member_openid").forEach { field ->
                    node.path(field).safeString().takeIf(String::isNotBlank)?.let { selfIds.add(it) }
                }
                ReceiveMessage.User.empty().copy(id = self.id)
            } else user(node, type == "C2C_MESSAGE_CREATE")
            if (mentioned.id.isNotBlank()) users.add(mentioned)
        }
        if (type == "GROUP_AT_MESSAGE_CREATE") users.add(ReceiveMessage.User.empty().copy(id = self.id))
        return Mentions(users.distinctBy { it.id }, selfIds)
    }

    /**
     * 只移除开头的 @机器人和指令斜杠，保留正文中的 @ 和 URL
     */
    private fun normalizeText(content: String, selfIds: Set<String>): String {
        var text = content.trim()
        while (true) {
            val prefix = selfIds.firstOrNull { it.isNotBlank() && text.startsWith("<@$it>") } ?: break
            text = text.removePrefix("<@$prefix>").trimStart()
        }
        return text.removePrefix("/").trimStart()
    }

    private data class Mentions(val users: List<ReceiveMessage.User>, val selfIds: Set<String>)

    /**
     * 将单聊用户或群成员字段映射为框架用户
     */
    private fun user(node: JsonNode, private: Boolean): ReceiveMessage.User {
        val id = node.path(if (private) "user_openid" else "member_openid").safeString()
            .ifBlank { node.path("id").safeString() }
        val auth = when (node.path("member_role").safeString()) {
            "owner", "admin" -> ApiAuth.MANAGER
            else -> ApiAuth.ORDINARY_MEMBER
        }
        return ReceiveMessage.User(
            id,
            node.path("member_nickname").safeString().ifBlank { node.path("username").safeString() },
            auth, ImageUrl(node.path("avatar").safeString())
        )
    }
}
