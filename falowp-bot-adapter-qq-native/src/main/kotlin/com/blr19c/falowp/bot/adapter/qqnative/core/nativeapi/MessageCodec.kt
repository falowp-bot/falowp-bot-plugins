package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.*
import tools.jackson.databind.JsonNode
import java.net.URI

private fun JsonNode.field(key: String) = path(key).asString("")
private fun JsonNode.conversation() = when (field("source")) {
    "group" -> QQNativeConversation.Group(field("target"))
    "private" -> QQNativeConversation.Private(field("target"))
    else -> error("未知 NTQQ 会话类型")
}

internal fun JsonNode.receipt() = QQNativeMessageReceipt(field("id"), field("seq"), field("time"), field("sender"))

internal fun JsonNode.message() = QQNativeMessage(
    conversation(), field("peerUid"), receipt(), field("nickname"), path("role").asInt(0),
    path("parts").mapNotNull { it.receivedPart() },
    path("contentAvailable").asBoolean(true)
)

private fun JsonNode.receivedPart(): QQNativeReceivedPart? = when (val kind = field("kind")) {
    "text" -> QQNativeReceivedPart.Text(field("value"))
    "at" -> QQNativeReceivedPart.Mention(field("id"), field("name"), path("role").asInt(0))
    "emoji" -> QQNativeReceivedPart.Emoji(field("id"), field("style"), field("name"))
    "reply" -> QQNativeReceivedPart.Reply(
        field("id"), field("seq"), field("clientSeq"), field("time"),
        path("original").takeIf { it.isObject }?.message(), path("embedded").asBoolean(false)
    )

    "share" -> QQNativeReceivedPart.Share(
        field("app"),
        field("appId"),
        field("title"),
        field("url"),
        field("preview"),
        field("appName"),
        field("description")
    )

    "marketFace" -> QQNativeReceivedPart.MarketFace(
        field("id"),
        field("packageId"),
        field("name"),
        field("key"),
        field("url")
    )

    "image", "voice", "video", "file" -> QQNativeReceivedPart.Attachment(
        QQNativeMediaType.valueOf(kind.uppercase()),
        URI(field("uri")),
        field("id"),
        field("name"),
        field("size").toLongOrNull() ?: 0,
        field("thumbnail").takeIf { it.isNotBlank() }?.let { runCatching { URI(it) }.getOrNull() }
    )

    "unavailable" -> QQNativeMediaType.entries.find { it.name.equals(field("mediaKind"), ignoreCase = true) }?.let {
        QQNativeReceivedPart.Unavailable(it, field("id"))
    }

    else -> null
}

internal fun JsonNode.notice(): QQNativeNotice? {
    val detail = when (field("kind")) {
        "groupAdmin" -> QQNativeNoticeDetail.GroupAdmin(field("member"), path("enabled").asBoolean())
        "groupMute" -> QQNativeNoticeDetail.GroupMute(field("member"), path("duration").asLong())
        "groupCard" -> QQNativeNoticeDetail.GroupCard(field("member"), field("oldCard"), field("newCard"))
        "groupTitle" -> QQNativeNoticeDetail.GroupTitle(field("member"), field("title"))
        "groupEssence" -> QQNativeNoticeDetail.GroupEssence(
            field("messageId"),
            field("sequence"),
            field("member"),
            path("added").asBoolean(),
            path("message").takeIf { it.isObject }?.message()
        )

        "groupFileUpload" -> QQNativeNoticeDetail.GroupFileUpload(
            field("fileId"),
            field("name"),
            path("size").asLong(),
            path("busId").asLong(),
            field("messageId"),
            path("url").takeIf { it.isString }?.asString()
        )

        "messageReaction" -> QQNativeNoticeDetail.MessageReaction(
            field("messageId"),
            field("sequence"),
            listOf(QQNativeReaction(field("emojiId"), path("count").asInt())),
            path("added").asBoolean(),
            path("message").takeIf { it.isObject }?.message()
        )

        "inputStatus" -> QQNativeNoticeDetail.InputStatus(path("status").asInt(), field("text"))
        "profileLike" -> QQNativeNoticeDetail.ProfileLike(path("count").asInt(), path("time").asLong())
        "nudge" -> QQNativeNoticeDetail.Nudge(field("member"))
        "join" -> QQNativeNoticeDetail.Join(field("member"), field("action"))
        "leave" -> QQNativeNoticeDetail.Leave(field("member"), field("action"))
        "friendRequest" -> QQNativeNoticeDetail.FriendRequest(field("comment"), field("flag"))
        "groupRequest" -> QQNativeNoticeDetail.GroupRequest(field("comment"), field("flag"), field("action"))
        "withdraw" -> QQNativeNoticeDetail.Withdraw(path("original").message())
        else -> return null
    }

    fun profile(key: String): QQNativeNoticeUser? = path(key).takeIf { it.isObject }?.let {
        QQNativeNoticeUser(it.field("id"), it.field("nickname"), it.path("role").asInt(0))
    }
    return QQNativeNotice(conversation(), field("actor"), detail, profile("actorInfo"), profile("memberInfo"))
}
