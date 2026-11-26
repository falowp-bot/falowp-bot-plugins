package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.all
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import tools.jackson.databind.JsonNode
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 下载并解析合并转发记录，恢复嵌套消息及媒体信息
 */
internal class ForwardReader(private val bridge: NativeBridge, private val uid: String) {
    suspend fun download(resid: String): Map<String, List<ByteArray>> {
        require(resid.isNotBlank()) { "缺少转发资源 ID" }
        val request = cat(m(1, m(1, m(2, uid)), m(2, resid), n(3, 1)), m(15, n(1, 2), n(2, 0), n(3, 0), n(4, 0)))
        val reply = bridge.packet("trpc.group.long_msg_interface.MsgService.SsoRecvLongMsg", request)
        val zipped = b(b(reply, 1), 4)
        check(zipped.isNotEmpty()) { "QQ 未返回转发正文" }
        return sections(unpack(zipped, true, 32 * 1024 * 1024))
    }

    companion object {
        fun sections(body: ByteArray): Map<String, List<ByteArray>> =
            all(body, 2).associate { s(it, 1) to all(b(it, 2), 1) }.also {
                check(!it["MultiMsg"].isNullOrEmpty()) { "QQ 转发正文没有消息" }
            }

        fun elements(row: ByteArray, selfUid: String = "", depth: Int = 0): List<JsonNode> {
            require(depth <= 8) { "引用嵌套超过 8 层" }
            val content = b(b(row, 3), 2)
            val offline = b(content, 1)
            val files = if (s(offline, 3).isNotBlank()) listOf(
                j(
                    obj(
                        "packetFile" to obj(
                            "id" to s(offline, 3), "name" to s(offline, 5), "size" to u(offline, 6).toLong(),
                            "hash" to s(offline, 57), "group" to ""
                        )
                    )
                )
            ) else emptyList()
            return files + all(b(b(row, 3), 1), 2).map { part ->
                val txt = b(part, 1)
                val face = b(part, 2)
                val market = b(part, 6)
                val transfer = b(part, 5)
                val reply = b(part, 45)
                val common = b(part, 53)
                val rich = b(b(part, 12), 1)
                val app = b(b(part, 51), 1)
                when {
                    u(transfer, 1) == 24uL -> {
                        val tlv = b(transfer, 2)
                        require(
                            tlv.size >= 3 && tlv[0] == 1.toByte() &&
                                    ((tlv[1].toInt() and 255) * 256 + (tlv[2].toInt() and 255)) == tlv.size - 3
                        ) { "群文件转发记录不完整" }
                        val info = b(b(tlv.copyOfRange(3, tlv.size), 7), 2)
                        j(
                            obj(
                                "packetFile" to obj(
                                    "id" to s(info, 2), "name" to s(info, 4),
                                    "size" to u(info, 3).toLong(), "busId" to u(info, 1).toLong(),
                                    "group" to u(b(b(row, 1), 8), 1).toString()
                                )
                            )
                        )
                    }

                    reply.isNotEmpty() -> {
                        val reserve = b(reply, 8)
                        val source = b(reply, 9)
                        val original = if (source.isNotEmpty()) quoted(source, selfUid, depth + 1) else {
                            val elements = all(reply, 5)
                            if (elements.isEmpty()) null else quoted(
                                cat(
                                    m(1, n(1, u(reply, 2).toLong()), m(2, s(reserve, 6)), m(8, b(b(row, 1), 8))),
                                    m(
                                        2,
                                        n(5, u(reply, 1).toLong()),
                                        n(6, u(reply, 3).toLong()),
                                        n(12, u(reserve, 3).toLong())
                                    ),
                                    m(3, m(1, *elements.map { m(2, it) }.toTypedArray()))
                                ), selfUid, depth + 1
                            )
                        }
                        j(
                            obj(
                                "elementType" to 7, "replyElement" to obj(
                                    "replayMsgSeq" to (original?.s("msgSeq") ?: u(reserve, 8).takeIf { it != 0uL }
                                        ?.toString() ?: u(reply, 1).toString()),
                                    "replyMsgTime" to u(reply, 3).toString(), "senderUin" to u(reply, 2).toString(),
                                    "replayMsgId" to (original?.s("msgId") ?: u(reserve, 3).toString()),
                                    "embedded" to true, "original" to original
                                )
                            )
                        )
                    }

                    txt.isNotEmpty() -> j(
                        obj(
                            "elementType" to 1,
                            "textElement" to obj(
                                "content" to s(txt, 1),
                                "atType" to u(b(txt, 12), 3).toInt(),
                                "atNtUid" to s(b(txt, 12), 9)
                            )
                        )
                    )

                    market.isNotEmpty() -> j(
                        obj(
                            "elementType" to 11, "marketFaceElement" to obj(
                                "faceName" to s(market, 1), "emojiId" to b(market, 4).hex(),
                                "emojiPackageId" to u(market, 5).toString(), "key" to s(market, 7)
                            )
                        )
                    )

                    face.isNotEmpty() -> j(
                        obj(
                            "elementType" to 6,
                            "faceElement" to obj("faceIndex" to u(face, 1).toInt(), "faceType" to 1)
                        )
                    )

                    u(common, 1) == 48uL -> j(
                        obj(
                            "elementType" to 0, "packetMedia" to obj(
                                "kind" to when (u(common, 3).toInt()) {
                                    10, 20 -> "image"
                                    11, 21 -> "video"
                                    else -> "voice"
                                }, "info" to binary(b(common, 2))
                            )
                        )
                    )

                    u(common, 1) in listOf(33uL, 37uL) -> j(
                        obj(
                            "elementType" to 6,
                            "faceElement" to obj(
                                "faceIndex" to u(b(common, 2), if (u(common, 1) == 33uL) 1 else 3).toInt(),
                                "faceType" to if (u(common, 1) == 33uL) 1 else 3
                            )
                        )
                    )

                    rich.isNotEmpty() -> {
                        val xml = compressedText(rich)

                        // 仅提取属性文本，不解析 XML 实体，也不访问外部资源
                        fun attr(name: String): String =
                            Regex("\\b$name=[\"']([^\"']*)[\"']").find(xml)?.groupValues?.get(1).orEmpty()
                                .replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<")
                                .replace("&gt;", ">")
                                .replace("&amp;", "&")
                        if (attr("serviceID") == "35" && attr("m_fileName").isNotBlank()) j(
                            obj(
                                "elementType" to 16,
                                "multiForwardMsgElement" to obj(
                                    "fileName" to attr("m_fileName"),
                                    "resId" to attr("m_resid")
                                )
                            )
                        ) else j(obj("elementType" to 0))
                    }

                    app.isNotEmpty() -> j(
                        obj(
                            "elementType" to 10,
                            "arkElement" to obj("bytesData" to compressedText(app))
                        )
                    )

                    else -> j(obj("elementType" to 0))
                }
            }

        }

        private fun quoted(row: ByteArray, selfUid: String, depth: Int): JsonNode {
            val head = b(row, 1)
            val content = b(row, 2)
            val group = u(b(head, 8), 1).toString().takeIf { it != "0" }
            val own = s(head, 2) == selfUid && selfUid.isNotBlank()
            return j(
                obj(
                    "chatType" to if (group != null) 2 else 1,
                    "peerUid" to (group ?: s(head, if (own) 6 else 2)),
                    "peerUin" to (group ?: u(head, if (own) 5 else 1).toString()),
                    "msgId" to u(content, 12).toString(), "msgSeq" to u(content, 5).toString(),
                    "msgTime" to u(content, 6).toString(), "senderUin" to u(head, 1).toString(),
                    "senderUid" to s(head, 2), "sendNickName" to s(b(head, 8), 4).ifBlank { s(b(head, 7), 6) },
                    "elements" to elements(row, selfUid, depth)
                )
            )
        }

        private fun compressedText(data: ByteArray) =
            text(if (data[0] == 1.toByte()) unpack(data.copyOfRange(1, data.size)) else data.copyOfRange(1, data.size))

        fun rows(
            sections: Map<String, List<ByteArray>>,
            section: String,
            peer: Peer,
            selfUid: String = ""
        ): List<JsonNode> =
            sections[section].orEmpty().mapIndexed { index, row ->
                val head = b(row, 1)
                val content = b(row, 2)
                j(
                    obj(
                        "chatType" to peer.chatType,
                        "peerUid" to peer.peerUid,
                        "peerUin" to if (peer.chatType == 2) peer.peerUid else "",
                        "msgId" to "forward:${
                            URLEncoder.encode(section, StandardCharsets.UTF_8).replace("+", "%20")
                        }:$index",
                        "msgSeq" to u(content, 5).toString(),
                        "msgTime" to u(content, 6).toString(),
                        "senderUin" to u(head, 1).toString(),
                        "senderUid" to s(head, 2),
                        "sendNickName" to s(b(head, 8), 4).ifEmpty { s(b(head, 7), 6) },
                        "elements" to elements(row, selfUid)
                    )
                )
            }
    }
}
