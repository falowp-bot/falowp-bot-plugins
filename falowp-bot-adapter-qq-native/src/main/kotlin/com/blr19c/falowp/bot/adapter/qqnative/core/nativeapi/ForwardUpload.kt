package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * 编码并上传合并转发记录，生成可发送的转发卡片
 */
internal class ForwardUpload(
    private val bridge: NativeBridge,
    private val account: JsonNode,
    private val contacts: NativeContacts,
    private val media: ForwardMedia,
    private val originals: OriginalMessages,
    private val references: NativeReferences
) {
    private val files = ForwardFiles(bridge, account)

    private data class Upload(val rows: List<ByteArray>, val uuid: String, val json: String)

    private fun record(group: Long, parts: List<EncodedPart>, original: JsonNode? = null): ByteArray {
        val senderId = original?.s("senderUin")?.takeIf { it.isNotBlank() && it != "0" } ?: account.s("id")
        val name = original?.s("sendMemberName")?.ifBlank { original.s("sendNickName") }
            ?.takeIf { it.isNotBlank() } ?: if (original != null) senderId else account.s("nickname")
            .ifEmpty { senderId }
        val senderUid = original?.s("senderUid").orEmpty()
        val toUid = if (original == null || senderUid != account.s("uid")) account.s("uid") else original.s("peerUid")
        val toUin =
            if (original == null) "0" else if (senderUid != account.s("uid")) account.s("id") else original.s("peerUin")
        val sender = if (group != 0L) m(8, n(1, group), m(4, name), n(5, 2)) else m(7, m(6, name))
        val head = m(
            1,
            n(1, senderId.toLong()),
            m(2, senderUid),
            n(3, 0),
            n(4, 0),
            n(5, toUin.toLongOrNull() ?: 0),
            *(if (group == 0L) listOf(m(6, toUid)) else emptyList()).toTypedArray(),
            sender
        )
        val avatar = "https://q.qlogo.cn/headimg_dl?dst_uin=$senderId&spec=0&img_type=jpg"
        val forwardHead = m(15, n(1, 0), n(2, 0), n(3, if (group != 0L) 1 else 2), m(5, avatar), m(6, avatar))
        val fileContent = parts.mapNotNull { it.content }.singleOrNull()
        val content = m(
            2,
            n(1, if (group != 0L) 82 else 9),
            *(if (group == 0L) listOf(n(2, 4), n(9, 4)) else emptyList()).toTypedArray(),
            n(5, original?.s("msgSeq")?.toLongOrNull() ?: ThreadLocalRandom.current().nextLong(1, 0xffffffffL)),
            n(6, original?.s("msgTime")?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)),
            n(12, original?.s("msgId")?.toULongOrNull()?.toLong() ?: 0),
            n(10, 0),
            forwardHead
        )
        return m(
            1,
            head,
            content,
            m(
                3,
                cat(
                    m(1, *parts.flatMap { it.elements }.map { m(2, it) }.toTypedArray()),
                    fileContent?.let { m(2, it) } ?: byteArrayOf())))
    }

    private suspend fun upload(
        group: Long,
        rows: List<ByteArray>,
        children: List<Upload>,
        previews: List<String>,
        title: String = ""
    ): Upload {
        fun section(name: String, records: List<ByteArray>) = m(2, m(1, name), m(2, *records.toTypedArray()))
        val body = cat(section("MultiMsg", rows), *children.map { section(it.uuid, it.rows) }.toTypedArray())
        val request = cat(
            m(
                2,
                n(1, if (group != 0L) 3 else 1),
                m(2, m(2, if (group != 0L) group.toString() else account.s("uid"))),
                n(3, group),
                m(4, gzip(body))
            ), m(15, n(1, 4), n(2, 1), n(3, 7), n(4, 0))
        )
        val resid = s(b(bridge.packet("trpc.group.long_msg_interface.MsgService.SsoSendLongMsg", request), 2), 3)
        check(resid.isNotBlank()) { "NTQQ 没有返回转发资源 ID" }
        val uuid = UUID.randomUUID().toString()
        val card =
            card(resid, uuid, rows.size, title.ifEmpty { if (group != 0L) "群聊的聊天记录" else "聊天记录" }, previews)
        return Upload(rows, uuid, mapper.writeValueAsString(card))
    }

    private suspend fun encode(
        part: QQNativeOutgoingPart,
        peer: Peer,
        depth: Int = 0,
        visited: Set<String> = emptySet()
    ): EncodedPart = when (part) {
        is QQNativeOutgoingPart.MarketFace, is QQNativeOutgoingPart.Game -> NativeSpecialMessages.forward(part)
        is QQNativeOutgoingPart.File -> files.encode(part, peer)
        is QQNativeOutgoingPart.Original -> error("原始消息尚未展开")
        is QQNativeOutgoingPart.Reply -> {
            require(depth < 8) { "引用嵌套超过 8 层" }
            val source = part.source?.let { Peer(it.i("chatType"), it.s("peerUid")) } ?: peer
            val row = references.read(source, part.data).deepCopy() as ObjectNode
            if (row.s("senderUin") in listOf("", "0")) row.put("senderUin", contacts.uin(row.s("senderUid")))
            check(row.s("senderUin").toLongOrNull()?.let { it > 0 } == true) { "无法确认引用消息的发送者" }
            val identity = "${source.chatType}/${source.peerUid}/${row.s("msgId")}/${row.s("msgSeq")}"
            require(identity !in visited) { "引用消息形成循环" }
            val parts = originals.expand(QQNativeOutgoingPart.Original(account.s("id"), row))
                .map { encode(it, source, depth + 1, visited + identity) }
            val embedded = b(record(if (source.chatType == 2) source.peerUid.toLong() else 0, parts, row), 1)
            val reserve = m(
                8, n(3, row.s("msgId").toULongOrNull()?.toLong() ?: 0),
                m(6, row.s("senderUid")), n(8, row.l("msgSeq"))
            )
            EncodedPart(
                listOf(
                    m(
                        45,
                        n(
                            1,
                            if (source.chatType == 1) row.l("msgClientSeq").takeIf { it > 0 }
                                ?: row.l("msgSeq") else row.l("msgSeq")),
                        n(2, row.l("senderUin")),
                        n(3, row.l("msgTime")),
                        *parts.flatMap { it.elements }.map { m(5, it) }.toTypedArray(),
                        reserve,
                        m(9, embedded),
                        n(10, 0)
                    )
                ), "[回复消息]"
            )
        }

        is QQNativeOutgoingPart.Ark -> EncodedPart(
            listOf(
                m(
                    51,
                    m(1, cat(byteArrayOf(1), deflate(part.json.toByteArray())))
                )
            ), "[分享]"
        )

        is QQNativeOutgoingPart.Text -> EncodedPart(listOf(m(1, m(1, part.value))), part.value)
        is QQNativeOutgoingPart.Mention -> {
            val all = part.id == "all"
            val display = if (all) "@全体成员" else "@${part.id}"
            val mention =
                m(12, n(3, if (all) 1 else 2), n(4, 0), n(5, 0), m(9, if (all) "all" else contacts.uid(part.id)))
            EncodedPart(listOf(m(1, m(1, display), mention)), display)
        }

        is QQNativeOutgoingPart.Emoji -> {
            val id = part.id.toInt()
            val display = part.name.ifEmpty { "[表情]" }
            val elements = when {
                part.style.toIntOrNull() == 3 -> listOf(
                    m(
                        53,
                        n(1, 37),
                        m(2, m(1, "1"), m(2, "8"), n(3, id), n(4, 1), n(9, 1)),
                        n(3, 1)
                    ), m(1, m(1, display))
                )

                id < 260 -> listOf(m(2, n(1, id)))
                else -> listOf(m(53, n(1, 33), m(2, n(1, id), m(2, ""), m(3, "")), n(3, 1)))
            }
            EncodedPart(elements, display)
        }

        is QQNativeOutgoingPart.Image, is QQNativeOutgoingPart.Voice, is QQNativeOutgoingPart.Video -> media.encode(
            part,
            peer
        )

        is QQNativeOutgoingPart.Nudge, QQNativeOutgoingPart.Partition -> error("戳一戳和分段不能编码为转发内容")
    }

    suspend fun create(peer: Peer, input: List<List<QQNativeOutgoingPart>>): JsonNode {
        val chains = input.map { chain -> chain.filter { it !is QQNativeOutgoingPart.Nudge } }
        val nonempty = chains.map { chain -> chain.filter { it != QQNativeOutgoingPart.Partition } }
        require(chains.isNotEmpty() && nonempty.none { it.isEmpty() }) { "合并转发缺少消息" }
        val group = if (peer.chatType == 2) peer.peerUid.toLong() else 0L
        val outer = if (chains.size == 1) {
            val sections = mutableListOf<List<QQNativeOutgoingPart>>()
            var section = mutableListOf<QQNativeOutgoingPart>()
            for (part in chains[0]) {
                if (part == QQNativeOutgoingPart.Partition || part is QQNativeOutgoingPart.File || part is QQNativeOutgoingPart.Voice || part is QQNativeOutgoingPart.Video) {
                    if (section.isNotEmpty()) sections.add(section)
                    section = mutableListOf()
                    if (part != QQNativeOutgoingPart.Partition) sections.add(listOf(part))
                } else section.add(part)
            }
            if (section.isNotEmpty()) sections.add(section)
            val entries = sections.map { it.map { part -> encode(part, peer) } }
            upload(
                group,
                entries.map { record(group, it) },
                emptyList(),
                entries.map { it.joinToString("") { part -> part.preview } },
                chains[0].filterIsInstance<QQNativeOutgoingPart.Text>().firstOrNull()?.value.orEmpty()
            )
        } else {
            val children = nonempty.map { chain ->
                val encoded = chain.map { encode(it, peer) }
                upload(group, encoded.map { record(group, listOf(it)) }, emptyList(), encoded.map { it.preview })
            }
            val rows =
                children.map {
                    record(
                        group,
                        listOf(
                            EncodedPart(
                                listOf(m(51, m(1, cat(byteArrayOf(1), deflate(it.json.toByteArray()))))),
                                "[聊天记录]"
                            )
                        )
                    )
                }
            upload(group, rows, children, children.map { "[聊天记录]" })
        }
        return j(obj("elementType" to 10, "elementId" to "", "arkElement" to obj("bytesData" to outer.json)))
    }

    companion object {
        fun card(
            resid: String,
            uuid: String,
            count: Int,
            title: String = "聊天记录",
            previews: List<String> = emptyList()
        ) = j(
            obj(
                "app" to "com.tencent.multimsg",
                "view" to "contact",
                "ver" to "0.0.0.5",
                "prompt" to "[聊天记录]",
                "desc" to "[聊天记录]",
                "config" to obj("autosize" to 1, "forward" to 1, "round" to 1, "type" to "normal", "width" to 300),
                "extra" to obj("filename" to uuid, "tsum" to count),
                "meta" to obj(
                    "detail" to obj(
                        "resid" to resid,
                        "uniseq" to uuid,
                        "source" to title,
                        "summary" to "查看${count}条转发消息",
                        "news" to previews.take(4).map { obj("text" to it) })
                )
            )
        )
    }
}
