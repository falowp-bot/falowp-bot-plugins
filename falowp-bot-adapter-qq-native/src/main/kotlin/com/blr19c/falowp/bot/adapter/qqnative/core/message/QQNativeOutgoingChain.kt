package com.blr19c.falowp.bot.adapter.qqnative.core.message

import java.nio.file.Path
import tools.jackson.databind.JsonNode

/**
 * 按发送顺序组织的原生消息元素链
 */
data class QQNativeOutgoingChain(val parts: List<QQNativeOutgoingPart>) {
    constructor(vararg parts: QQNativeOutgoingPart) : this(parts.toList())
}

/**
 * 原生发送协议使用的消息元素
 */
sealed interface QQNativeOutgoingPart {
    data class Ark(val json: String) : QQNativeOutgoingPart
    data object Partition : QQNativeOutgoingPart
    data class Text(val value: String) : QQNativeOutgoingPart
    data class Mention(val id: String) : QQNativeOutgoingPart
    data class Nudge(val id: String) : QQNativeOutgoingPart
    data class Emoji(val id: String, val style: String, val name: String) : QQNativeOutgoingPart
    data class File(val path: Path, val name: String, val folderId: String = "") : QQNativeOutgoingPart
    data class MarketFace(val id: String, val packageId: Long, val key: String, val name: String) : QQNativeOutgoingPart
    data class Game(val dice: Boolean, val result: Int?) : QQNativeOutgoingPart
    data class Reply(val data: JsonNode, val source: JsonNode? = null) : QQNativeOutgoingPart
    data class Original(val accountId: String, val message: JsonNode) : QQNativeOutgoingPart
    data class Voice(val path: Path) : QQNativeOutgoingPart
    data class Video(val path: Path) : QQNativeOutgoingPart
    data class Image(val path: Path, val extension: String, val pictureType: Int, val width: Int, val height: Int) :
        QQNativeOutgoingPart
}
