package com.blr19c.falowp.bot.adapter.qqnative.adapter.message

import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeAccount
import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.systemConfigListProperty

/**
 * 将 QQ 原生消息转换为框架接收消息，并映射用户权限、媒体和引用内容
 */
internal object NativeMessages {
    private val administrators by lazy { systemConfigListProperty("administrator") { emptyList() }.toSet() }
    private fun avatar(id: String) = if (id.matches(Regex("[0-9]+")))
        ImageUrl("https://q1.qlogo.cn/g?b=qq&nk=$id&s=640") else ImageUrl.empty()

    fun self(account: QQNativeAccount) = BotSelf.Default(account.id, account.nickname, avatar(account.id))

    fun user(id: String, name: String = id, role: Int = 0): ReceiveMessage.User {
        val permission = when {
            id in administrators -> ApiAuth.ADMINISTRATOR
            role == 3 || role == 4 -> ApiAuth.MANAGER
            else -> ApiAuth.ORDINARY_MEMBER
        }
        return ReceiveMessage.User(id, name, permission, avatar(id))
    }

    fun source(conversation: QQNativeConversation) = ReceiveMessage.Source(
        conversation.id, when (conversation) {
            is QQNativeConversation.Group -> SourceTypeEnum.GROUP
            is QQNativeConversation.Private -> SourceTypeEnum.PRIVATE
        }
    )

    fun decode(message: QQNativeMessage, account: QQNativeAccount): ReceiveMessage {
        val parts = message.parts
        val media = parts.filterIsInstance<QQNativeReceivedPart.Attachment>().groupBy { it.type }
        val content = ReceiveMessage.Content(
            parts.filterIsInstance<QQNativeReceivedPart.Text>().joinToString("") { it.value },
            media[QQNativeMediaType.VOICE]?.firstOrNull()?.let { ReceiveMessage.Voice(it.id, it.uri) },
            parts.filterIsInstance<QQNativeReceivedPart.Mention>().map { user(it.id, it.name, it.role) },
            media[QQNativeMediaType.IMAGE].orEmpty().map { ImageUrl(it.uri.toString()) } +
                    parts.filterIsInstance<QQNativeReceivedPart.MarketFace>()
                        .mapNotNull { it.url.takeIf(String::isNotBlank)?.let(::ImageUrl) },
            parts.filterIsInstance<QQNativeReceivedPart.Emoji>().map { Face(it.id, it.style, it.name) } +
                    parts.filterIsInstance<QQNativeReceivedPart.MarketFace>()
                        .map { Face(it.id, it.packageId, it.name) },
            media[QQNativeMediaType.VIDEO]?.firstOrNull()?.let {
                ReceiveMessage.Video(
                    it.id,
                    it.thumbnail?.let { uri -> ImageUrl(uri.toString()) } ?: ImageUrl.empty(),
                    it.uri,
                    it.size)
            },
            parts.filterIsInstance<QQNativeReceivedPart.Share>().map {
                ReceiveMessage.Share(it.app, it.appId, it.appName, it.title, ImageUrl(it.preview), it.url)
            },
            media[QQNativeMediaType.FILE].orEmpty().map {
                ReceiveMessage.File(it.id, it.name.substringAfterLast('.', ""), it.name, it.uri, it.size)
            }
        ) {
            parts.filterIsInstance<QQNativeReceivedPart.Reply>().firstOrNull()?.let { reply ->
                account.messages.resolve(message, reply)?.let { decode(it, account) }
            }
        }
        return ReceiveMessage(
            message.receipt.id, content.toMessageType(), content,
            user(message.receipt.sender, message.nickname, message.role), source(message.conversation), self(account),
            ReceiveMessage.Adapter("QQNative", message)
        )
    }

    fun emoji(id: String): ReceiveMessage.Emoji = NativeEmoji.resolve(id)

    private data class Face(override val id: String, override val type: String, override val display: String) :
        ReceiveMessage.Emoji
}
