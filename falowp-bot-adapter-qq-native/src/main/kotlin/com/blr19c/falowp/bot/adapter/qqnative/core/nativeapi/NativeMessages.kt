package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MessageHistory
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MemoryMessageHistory
import kotlinx.coroutines.CancellationException
import tools.jackson.databind.JsonNode

/**
 * 协调消息发送、合并转发、历史查询及引用解析
 */
internal class NativeMessages(
    val bridge: NativeBridge,
    private val account: JsonNode,
    private val contacts: NativeContacts,
    private val media: NativeMedia,
    private val delivery: NativeDelivery,
    history: MessageHistory = MemoryMessageHistory()
) {
    private val forwardMedia = ForwardMedia(bridge, media, account.s("id"))
    private val forwardFiles = ForwardFiles(bridge, account)
    private val originals = OriginalMessages(media, contacts)
    private val upload = ForwardUpload(
        bridge,
        account,
        contacts,
        forwardMedia,
        originals,
        NativeReferences(bridge, history, account.s("id"))
    )

    suspend fun peer(group: Boolean, id: String) = Peer(if (group) 2 else 1, if (group) id else contacts.uid(id))
    private fun usable(id: String) = id.isNotEmpty() && id != "0"
    suspend fun decode(row: JsonNode): JsonNode {
        val group = row.i("chatType") == 2
        val sender = row.s("senderUin").takeIf { usable(it) } ?: contacts.uin(row.s("senderUid"))
        contacts.remember(row.s("senderUin"), row.s("senderUid"))
        val target =
            row.s("peerUin").takeIf { usable(it) } ?: if (group) row.s("peerUid") else contacts.uin(row.s("peerUid"))
        val parts = mutableListOf<JsonNode>()
        for (e in row.list("elements")) {
            when {
                e.hasValue("packetFile") -> {
                    val file = e.path("packetFile")
                    parts.add(
                        try {
                            forwardFiles.resolve(file)
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            j(obj("kind" to "unavailable", "mediaKind" to "file", "id" to file.s("id")))
                        }
                    )
                }

                e.hasValue("packetMedia") -> {
                    val packet = e.path("packetMedia")
                    parts.add(
                        forwardMedia.resolve(
                            Peer(row.i("chatType"), row.s("peerUid")),
                            packet.s("kind"),
                            packet.path("info").binary()
                        )
                    )
                }

                e.hasValue("textElement") -> {
                    val t = e.path("textElement")
                    if (t.i("atType") == 0) {
                        parts.add(j(obj("kind" to "text", "value" to t.s("content"))))
                    } else {
                        val id = when {
                            t.i("atType") == 1 -> "all"
                            t.i("atType") == 4 || t.s("atNtUid") == account.s("uid") -> account.s("id")
                            usable(t.s("atUid")) -> t.s("atUid")
                            else -> contacts.uin(t.s("atNtUid"))
                        }
                        val profile =
                            if (id == "all") null else contacts.profile(id, target.takeIf { group }, t.s("content"))
                        parts.add(
                            j(
                                obj(
                                    "kind" to "at",
                                    "id" to id,
                                    "name" to (profile?.s("nickname") ?: t.s("content")),
                                    "role" to (profile?.i("role") ?: 0)
                                )
                            )
                        )
                    }
                }

                e.hasValue("marketFaceElement") -> {
                    val face = e.path("marketFaceElement")
                    val id = face.s("emojiId")
                    val url = if (id.matches(Regex("[A-Fa-f0-9]{32}")))
                        "https://gxh.vip.qq.com/club/item/parcel/item/${id.take(2)}/$id/raw300.gif" else ""
                    parts.add(
                        j(
                            obj(
                                "kind" to "marketFace", "id" to id,
                                "packageId" to face.s("emojiPackageId"), "name" to face.s("faceName"),
                                "key" to face.s("key"), "url" to url
                            )
                        )
                    )
                }

                e.hasValue("faceElement") -> {
                    val f = e.path("faceElement")
                    parts.add(
                        j(
                            obj(
                                "kind" to "emoji",
                                "id" to f.s("faceIndex"),
                                "style" to f.s("faceType"),
                                "name" to f.s("faceText")
                            )
                        )
                    )
                }

                e.hasValue("replyElement") -> {
                    val r = e.path("replyElement")
                    parts.add(
                        j(
                            obj(
                                "kind" to "reply",
                                "id" to r.s("replayMsgId"),
                                "seq" to r.s("replayMsgSeq"),
                                "clientSeq" to r.s("replyMsgClientSeq"),
                                "time" to r.s("replyMsgTime"),
                                "embedded" to r.path("embedded").asBoolean(false),
                                "original" to r.path("original").takeIf { it.isObject }?.let { decode(it) }
                            )
                        )
                    )
                }

                e.hasValue("multiForwardMsgElement") -> parts.add(
                    j(
                        obj(
                            "kind" to "share",
                            "app" to "com.tencent.multimsg",
                            "appId" to "FORWARD_MESSAGE",
                            "appName" to "合并转发消息",
                            "title" to "[聊天记录]",
                            "url" to "",
                            "preview" to ""
                        )
                    )
                )

                e.hasValue("arkElement") -> {
                    val ark = runCatching { mapper.readTree(e.path("arkElement").s("bytesData")) }.getOrNull()
                    if (ark != null) parts.add(NativeShares.decode(ark))
                }

                else -> {
                    val item = listOf(
                        "image" to "picElement",
                        "voice" to "pttElement",
                        "video" to "videoElement",
                        "file" to "fileElement"
                    ).firstOrNull { e.hasValue(it.second) } ?: continue
                    val data = e.path(item.second)
                    parts.add(
                        try {
                            val uri = media.read(row, e, data)
                            j(
                                obj(
                                    "kind" to item.first,
                                    "uri" to uri,
                                    "id" to data.s("fileUuid").ifEmpty { e.s("elementId") },
                                    "name" to data.s("fileName"),
                                    "size" to data.s("fileSize"),
                                    "thumbnail" to if (item.first == "video") media.videoThumbnail(data, uri) else null
                                )
                            )
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            j(obj("kind" to "unavailable", "mediaKind" to item.first, "id" to e.s("elementId")))
                        }
                    )
                }
            }
        }
        return j(
            obj(
                "id" to row.s("msgId"),
                "seq" to row.s("msgSeq"),
                "time" to row.s("msgTime"),
                "source" to if (group) "group" else "private",
                "target" to target,
                "peerUid" to row.s("peerUid"),
                "sender" to sender,
                "nickname" to row.s("sendMemberName").ifEmpty { row.s("sendNickName").ifEmpty { sender } },
                "role" to if (group) row.i("roleType").takeIf { it != 0 } ?: contacts.role(
                    target,
                    row.s("senderUid")
                ) else 0,
                "parts" to parts))
    }

    private suspend fun element(part: QQNativeOutgoingPart): JsonNode = when (part) {
        is QQNativeOutgoingPart.MarketFace, is QQNativeOutgoingPart.Game -> NativeSpecialMessages.element(part)
        is QQNativeOutgoingPart.Reply -> j(obj("elementType" to 7, "elementId" to "", "replyElement" to part.data))
        is QQNativeOutgoingPart.Original -> error("原始消息需要按元素展开")
        is QQNativeOutgoingPart.Ark -> j(
            obj(
                "elementType" to 10,
                "elementId" to "",
                "arkElement" to obj("bytesData" to part.json)
            )
        )

        is QQNativeOutgoingPart.Text, is QQNativeOutgoingPart.Mention -> {
            val id = (part as? QQNativeOutgoingPart.Mention)?.id
            val all = id == "all"
            j(
                obj(
                    "elementId" to "", "elementType" to 1,
                    "textElement" to obj(
                        "content" to if (part is QQNativeOutgoingPart.Text) part.value else "@${if (all) "全体成员" else id}",
                        "atType" to if (id == null) 0 else if (all) 1 else 2,
                        "atUid" to if (id == null) "" else if (all) "0" else id,
                        "atNtUid" to if (id == null) "" else if (all) "all" else contacts.uid(id),
                        "atTinyId" to ""
                    )
                )
            )
        }

        is QQNativeOutgoingPart.Emoji -> j(
            obj(
                "elementId" to "", "elementType" to 6,
                "faceElement" to obj(
                    "faceIndex" to part.id.toInt(),
                    "faceType" to (part.style.toIntOrNull()?.takeIf { it != 0 } ?: 1),
                    "sourceType" to 1, "faceText" to part.name
                )
            ))

        is QQNativeOutgoingPart.File, is QQNativeOutgoingPart.Image, is QQNativeOutgoingPart.Voice, is QQNativeOutgoingPart.Video -> media.element(
            part
        )

        is QQNativeOutgoingPart.Nudge, QQNativeOutgoingPart.Partition -> error("戳一戳和分段需要单独处理")
    }

    suspend fun send(
        to: QQNativeConversation,
        chains: List<QQNativeOutgoingChain>,
        replyTo: QQNativeMessage?,
        forward: Boolean
    ): List<JsonNode> {
        require(to.id.isNotBlank()) { "缺少目标 QQ 号或群号" }
        for (part in chains.flatMap { it.parts }) {
            if (part is QQNativeOutgoingPart.Original) {
                require(part.accountId == account.s("id")) { "原始消息不属于当前账号" }
            }
            if (part is QQNativeOutgoingPart.File) require(part.folderId.isEmpty() || to is QQNativeConversation.Group) { "私聊文件不能指定群目录" }
        }
        val prepared = chains.map { chain ->
            chain.parts.flatMap {
                if (it is QQNativeOutgoingPart.Original) originals.expand(it) else listOf(it)
            }
        }
        val group = to is QQNativeConversation.Group
        val peer = peer(group, to.id)
        val reference = replyTo?.takeIf { it.conversation == to }?.receipt
        if (forward) return listOf(
            delivery.deliver(
                peer,
                listOf(upload.create(peer, prepared))
            )
        )
        val receipts = mutableListOf<JsonNode>()
        for (chain in prepared) {
            val elements = mutableListOf<JsonNode>()
            suspend fun flush() {
                if (elements.isEmpty()) return
                if (reference != null) {
                    elements.add(
                        0,
                        j(
                            obj(
                                "elementType" to 7,
                                "elementId" to "",
                                "replyElement" to obj(
                                    "replayMsgId" to reference.id,
                                    "replayMsgSeq" to reference.seq,
                                    "senderUin" to reference.sender,
                                    "senderUinStr" to reference.sender,
                                    "replyMsgTime" to reference.time
                                )
                            )
                        )
                    )
                }
                receipts.add(delivery.deliver(peer, elements.toList()))
                elements.clear()
            }
            for (part in chain) {
                when (part) {
                    QQNativeOutgoingPart.Partition -> flush()
                    is QQNativeOutgoingPart.Nudge -> {
                        flush()
                        val member = contacts.uin(part.id)
                        val conversation = contacts.uin(to.id)
                        require(
                            listOf(
                                member,
                                conversation
                            ).all { Regex("[1-9]\\d*").matches(it) }) { "戳一戳需要数字 QQ 号和会话号" }
                        require(group || member == conversation) { "私聊戳一戳的目标必须是会话对方" }
                        val body = cat(
                            Wire.n(1, member.toLong()),
                            Wire.n(if (group) 2 else 5, conversation.toLong()),
                            Wire.n(6, 0)
                        )
                        val response = bridge.packet(
                            "OidbSvcTrpcTcp.0xED3_1",
                            cat(Wire.n(1, 0xed3), Wire.n(2, 1), Wire.m(4, body), Wire.n(12, 1))
                        )
                        check(Wire.u(response, 3) == 0uL) {
                            "QQ 戳一戳失败 (${Wire.u(response, 3)})：${
                                Wire.s(
                                    response,
                                    5
                                )
                            }"
                        }
                    }

                    else -> {
                        val alone =
                            part is QQNativeOutgoingPart.File || part is QQNativeOutgoingPart.Voice || part is QQNativeOutgoingPart.Video
                        if (alone) flush()
                        elements.add(element(part))
                        if (alone) flush()
                    }
                }
            }
            flush()
        }
        return receipts
    }

    suspend fun reference(message: QQNativeMessage, r: QQNativeReceivedPart.Reply): JsonNode? {
        if (r.embedded) return null
        val peer = peer(message.conversation is QQNativeConversation.Group, message.peerUid)
        var row: JsonNode? = null
        if (usable(r.id)) row = bridge.msg("getMsgsByMsgId", peer, listOf(r.id)).list("msgList").firstOrNull()
        if (row == null && usable(r.seq)) row =
            bridge.msg("getSingleMsg", peer, r.seq).list("msgList").firstOrNull()
        if (row == null && usable(r.clientSeq) && usable(r.time)) row =
            bridge.msg("getMsgByClientSeqAndTime", peer, r.clientSeq, r.time).list("msgList").firstOrNull()
        return row?.let { decode(it) }
    }
}
