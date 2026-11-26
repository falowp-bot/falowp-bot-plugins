package com.blr19c.falowp.bot.adapter.qqnative.core.message

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import java.net.URI

/**
 * 统一的 QQ 接收消息，包含会话、发送回执、消息元素及原文可用状态
 */
data class QQNativeMessage(
    val conversation: QQNativeConversation,
    val peerUid: String,
    val receipt: QQNativeMessageReceipt,
    val nickname: String,
    val role: Int,
    val parts: List<QQNativeReceivedPart>,
    /**
     * 是否保存了消息原文；仅收到撤回通知且无法找到原文时为 false
     */
    val contentAvailable: Boolean = true
)

/**
 * 原生消息定位信息，用于引用、撤回及历史记录查询
 */
data class QQNativeMessageReceipt(val id: String, val seq: String, val time: String, val sender: String)

/**
 * 原生消息解析后的元素类型
 */
sealed interface QQNativeReceivedPart {
    data class Text(val value: String) : QQNativeReceivedPart
    data class Mention(val id: String, val name: String, val role: Int = 0) : QQNativeReceivedPart
    data class Emoji(val id: String, val style: String, val name: String) : QQNativeReceivedPart

    /**
     * 引用信息，转发记录优先使用内嵌原文，不在目标会话猜测来源
     */
    data class Reply(
        val id: String, val seq: String, val clientSeq: String, val time: String,
        val original: QQNativeMessage? = null, val embedded: Boolean = false
    ) : QQNativeReceivedPart

    data class Share(
        val app: String, val appId: String, val title: String, val url: String, val preview: String,
        val appName: String = "", val description: String = ""
    ) : QQNativeReceivedPart

    /**
     * 商城表情的资源标识、表情包、摘要及原图地址
     */
    data class MarketFace(
        val id: String, val packageId: String, val name: String, val key: String, val url: String
    ) : QQNativeReceivedPart

    data class Attachment(
        val type: QQNativeMediaType, val uri: URI, val id: String, val name: String, val size: Long,
        val thumbnail: URI? = null
    ) : QQNativeReceivedPart

    data class Unavailable(val type: QQNativeMediaType, val id: String) : QQNativeReceivedPart
}

/**
 * 接收媒体的类型
 */
enum class QQNativeMediaType { IMAGE, VOICE, VIDEO, FILE }
