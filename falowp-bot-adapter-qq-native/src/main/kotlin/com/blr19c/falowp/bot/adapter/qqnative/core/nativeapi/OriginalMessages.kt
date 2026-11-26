package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * 从原文恢复发送元素，媒体先下载再按目标会话重新上传
 */
internal class OriginalMessages(private val media: NativeMedia, private val contacts: NativeContacts) {
    suspend fun expand(part: QQNativeOutgoingPart.Original): List<QQNativeOutgoingPart> {
        val row = part.message
        val elements = row.list("elements")
        require(elements.isNotEmpty()) { "原始消息为空" }
        return elements.map { element ->
            when {
                element.hasValue("textElement") -> {
                    val text = element.path("textElement")
                    if (text.i("atType") == 0) QQNativeOutgoingPart.Text(text.s("content"))
                    else QQNativeOutgoingPart.Mention(
                        if (text.i("atType") == 1) "all" else
                            text.s("atUid").takeIf { it.isNotBlank() && it != "0" } ?: contacts.uin(text.s("atNtUid")))
                }

                element.hasValue("faceElement") -> {
                    val face = element.path("faceElement")
                    if (face.i("faceIndex") in listOf(358, 359))
                        QQNativeOutgoingPart.Game(face.i("faceIndex") == 358, face.s("resultId").toIntOrNull())
                    else QQNativeOutgoingPart.Emoji(face.s("faceIndex"), face.s("faceType"), face.s("faceText"))
                }

                element.hasValue("marketFaceElement") -> {
                    val face = element.path("marketFaceElement")
                    QQNativeOutgoingPart.MarketFace(
                        face.s("emojiId"),
                        face.l("emojiPackageId"),
                        face.s("key"),
                        face.s("faceName")
                    )
                }

                element.hasValue("arkElement") -> QQNativeOutgoingPart.Ark(element.path("arkElement").s("bytesData"))
                element.hasValue("multiForwardMsgElement") -> {
                    val forward = element.path("multiForwardMsgElement")
                    require(forward.s("resId").isNotBlank()) { "历史合并转发缺少资源 ID" }
                    QQNativeOutgoingPart.Ark(
                        ForwardUpload.card(
                            forward.s("resId"),
                            forward.s("fileName"),
                            forward.i("msgCount")
                        ).toString()
                    )
                }

                element.hasValue("replyElement") -> QQNativeOutgoingPart.Reply(
                    element.path("replyElement").deepCopy(),
                    j(obj("chatType" to row.i("chatType"), "peerUid" to row.s("peerUid")))
                )

                else -> {
                    val type = listOf("picElement", "pttElement", "videoElement", "fileElement")
                        .firstOrNull { element.hasValue(it) }
                        ?: error("原始消息包含尚不能发送的元素：${element.i("elementType")}")
                    val data = element.path(type)
                    val path = Path.of(URI(media.read(row, element, data)))
                    when (type) {
                        "fileElement" -> QQNativeOutgoingPart.File(path, data.s("fileName"))
                        "pttElement" -> QQNativeOutgoingPart.Voice(path)
                        "videoElement" -> QQNativeOutgoingPart.Video(path)
                        else -> withContext(Dispatchers.IO) {
                            ImageIO.createImageInputStream(path.toFile()).use { stream ->
                                val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull()
                                    ?: error("无法读取原消息图片")
                                try {
                                    reader.input = stream
                                    val format = reader.formatName.lowercase()
                                    QQNativeOutgoingPart.Image(
                                        path, ".$format", when (format) {
                                            "png" -> 1001
                                            "gif" -> 2000
                                            "webp" -> 1002
                                            "bmp" -> 1005
                                            else -> 1000
                                        }, reader.getWidth(0), reader.getHeight(0)
                                    )
                                } finally {
                                    reader.dispose()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
