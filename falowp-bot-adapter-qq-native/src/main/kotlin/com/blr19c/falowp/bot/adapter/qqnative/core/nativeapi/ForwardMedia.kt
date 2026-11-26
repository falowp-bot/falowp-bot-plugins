package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.all
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import java.nio.file.Path
import java.security.SecureRandom

internal class EncodedPart(val elements: List<ByteArray>, val preview: String, val content: ByteArray? = null)

/**
 * 为合并转发准备媒体数据与上传凭据
 */
internal class ForwardMedia(private val bridge: NativeBridge, private val media: NativeMedia, uin: String) {
    private val highway = Highway(bridge, uin)
    private fun business(kind: String) = when (kind) {
        "image" -> 1
        "video" -> 2
        "voice" -> 3
        else -> error("未知媒体类型")
    }

    private fun command(kind: String, group: Boolean) = when (kind) {
        "image" -> if (group) 0x11c4 else 0x11c5
        "video" -> if (group) 0x11ea else 0x11e9
        "voice" -> if (group) 0x126e else 0x126d
        else -> error(
            "未知媒体类型"
        )
    }

    private fun head(peer: Peer, kind: String, command: Int) = m(
        1, m(1, n(1, if (kind == "video") 3 else 1), n(2, command)),
        m(
            2,
            n(101, 2),
            n(102, business(kind)),
            n(200, if (peer.chatType == 2) 2 else 1),
            if (peer.chatType == 2) m(202, n(1, peer.peerUid.toLong())) else m(201, n(1, 2), m(2, peer.peerUid))
        ), m(3, n(1, 2))
    )

    private fun fileInfo(file: FileHashes, kind: String, meta: JsonNode, sub: Int) = m(
        1, m(
            1,
            n(1, file.size),
            m(2, file.md5.hex()),
            m(3, file.sha1.hex()),
            m(4, file.path.fileName.toString()),
            m(
                5,
                n(1, if (sub != 0) 1 else business(kind)),
                n(2, if (kind == "image") meta.i("picType") else 0),
                n(3, if (kind == "video" && sub == 0) 2 else 0),
                n(4, if (kind == "voice") 1 else 0)
            ),
            n(6, if (sub != 0) meta.i("thumbWidth") else meta.i("picWidth").takeIf { it != 0 } ?: meta.i("fileWidth")),
            n(
                7,
                if (sub != 0) meta.i("thumbHeight") else meta.i("picHeight").takeIf { it != 0 }
                    ?: meta.i("fileHeight")),
            n(8, if (sub != 0) 0 else meta.i("duration").takeIf { it != 0 } ?: meta.i("fileTime")),
            n(9, if (kind == "image") 1 else 0)), n(2, sub))

    suspend fun encode(part: QQNativeOutgoingPart, peer: Peer): EncodedPart {
        val kind = when (part) {
            is QQNativeOutgoingPart.Image -> "image"
            is QQNativeOutgoingPart.Voice -> "voice"
            is QQNativeOutgoingPart.Video -> "video"
            else -> error("需要图片、语音或视频")
        }

        val group = peer.chatType == 2
        val element = media.element(part)
        val meta = listOf("picElement", "pttElement", "videoElement").map { element.path(it) }.first { it.isObject }
        val files = withContext(Dispatchers.IO) {
            buildList {
                add(
                    hashes(
                        Path.of(meta.s("filePath")),
                        kind == "video"
                    )
                )
                if (kind == "video") add(
                    hashes(
                        Path.of(
                            meta.path("thumbPath").mapEntries().first { it.first == "0" }.second.asString()
                        )
                    )
                )
            }
        }
        val ext = m(
            6,
            m(
                1,
                n(1, 0),
                m(2, if (kind == "image") "[图片]" else ""),
                *(if (kind == "image") listOf(m(if (group) 12 else 11, n(1, 0))) else emptyList()).toTypedArray()
            ),
            m(2, m(3, if (kind == "video") byteArrayOf(128.toByte(), 1, 0) else byteArrayOf())),
            m(
                3,
                m(11, if (kind == "voice") byteArrayOf(8, 0, 56, 0) else byteArrayOf()),
                m(12, byteArrayOf()),
                m(
                    13,
                    if (kind == "voice") (if (group) listOf(154, 1, 7, 170, 3, 4, 8, 8, 18, 0) else listOf(
                        154,
                        1,
                        11,
                        170,
                        3,
                        8,
                        8,
                        4,
                        18,
                        4,
                        0,
                        0,
                        0,
                        0
                    )).map { it.toByte() }.toByteArray() else byteArrayOf()
                )
            )
        )
        val body = cat(
            head(peer, kind, 100),
            m(
                2,
                *files.mapIndexed { index, file -> fileInfo(file, kind, meta, if (index == 0) 0 else 100) }
                    .toTypedArray(),
                n(2, 1),
                n(3, 0),
                n(4, SecureRandom().nextLong() and Long.MAX_VALUE),
                n(5, if (group) 2 else 1),
                ext,
                n(7, 0),
                n(8, 0)
            )
        )
        val upload = b(bridge.oidb(command(kind, group), 100, body), 2)

        val info = b(upload, 6)

        val infos = all(info, 1)
        check(infos.size >= files.size) { "QQ 未返回完整媒体资源" }
        val subFiles = all(upload, 10)
        for ((index, file) in files.withIndex()) {
            val sub = if (index == 0) upload else subFiles.getOrNull(index - 1) ?: byteArrayOf()
            val key = s(sub, if (index == 0) 1 else 2)
            val uuid = s(b(infos[index], 1), 2)
            check(uuid.isNotEmpty()) { "QQ 未返回媒体资源 ID" }
            val uploadCommand = when (kind) {
                "image" -> if (group) 1004 else 1003
                "voice" -> if (group) 1008 else 1007
                else -> (if (group) 1005 else 1001) + index
            }
            highway.upload(file, uploadCommand, uuid, key, all(sub, if (index == 0) 3 else 4), infos)
        }
        val biz = when (kind) {
            "image" -> 10
            "video" -> if (group) 21 else 11
            else -> if (group) 22 else 12
        }
        return EncodedPart(
            listOf(m(53, n(1, 48), m(2, info), n(3, biz))), when (kind) {
                "image" -> "[图片]"
                "video" -> "[视频]"
                else -> "[语音]"
            }
        )
    }

    suspend fun resolve(peer: Peer, kind: String, info: ByteArray): JsonNode {
        val entries = all(info, 1)
        val node = b(entries.firstOrNull() ?: byteArrayOf(), 1)
        check(node.isNotEmpty()) { "转发媒体缺少资源索引" }
        suspend fun url(index: ByteArray): String {
            val body = cat(head(peer, kind, 200), m(3, m(1, index), m(2, m(2, n(1, 0), n(2, 0)))))
            val reply = b(bridge.oidb(command(kind, peer.chatType == 2), 200, body), 3)
            val download = b(reply, 3)
            check(s(download, 1).isNotEmpty()) { "QQ 未返回转发媒体地址" }
            return "https://${s(download, 1)}${s(download, 2)}${s(reply, 1)}"
        }

        val uri = url(node)
        val thumbnail = if (kind == "video") {
            val cover = entries.drop(1).firstOrNull { u(it, 2) == 100uL || u(b(b(b(it, 1), 1), 5), 1) == 1uL }
            if (cover == null) null else try {
                url(b(cover, 1))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                LoggerFactory.getLogger(javaClass)
                    .warn("QQ 转发视频缩略图解析失败，保留视频正文：{}", error.javaClass.simpleName)
                null
            }
        } else null
        return j(
            obj(
                "kind" to kind,
                "uri" to uri,
                "thumbnail" to thumbnail,
                "id" to s(node, 2),
                "name" to s(b(node, 1), 4),
                "size" to u(b(node, 1), 1).toString()
            )
        )
    }
}
