package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeAccount
import com.blr19c.falowp.bot.adapter.qqnative.adapter.message.NativeMessages as FrameworkMessages
import com.blr19c.falowp.bot.adapter.qqnative.core.history.HistoryKey
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MemoryMessageHistory
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessage
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeReceivedPart
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.all
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import java.nio.file.Files
import tools.jackson.databind.JsonNode
import kotlin.test.*

class ForwardReferencesTest {
    private class Fixture(root: String = "") {
        val account = j(obj("id" to "9", "uid" to "u_9", "nickname" to "Bot"))
        val history = MemoryMessageHistory()
        val queries = mutableListOf<Pair<String, String>>()
        var response = emptyList<JsonNode>()
        var records = emptyList<ByteArray>()
        val bridge = NativeBridge { _, args ->
            val params = j(args).path("arguments")
            when (val method = args["method"].toString()) {
                "getMsgsByMsgId", "getSingleMsg", "getMsgByClientSeqAndTime" -> {
                    queries.add(method to params[0].s("peerUid"))
                    j(obj("msgList" to response))
                }

                "sendSsoCmdReqByContend" -> {
                    if (params[0].asString() == "OidbSvcTrpcTcp.0x6D6_0") {
                        assertEquals(84uL, u(b(b(params[1].binary(), 4), 1), 1))
                        return@NativeBridge j(
                            obj(
                                "result" to 0,
                                "rspbuffer" to binary(m(4, m(1, n(1, 0), m(7, "/quoted-file"), n(10, 1))))
                            )
                        )
                    }
                    if (params[0].asString() == "OidbSvcTrpcTcp.0x6D6_2") {
                        assertEquals(84uL, u(b(b(params[1].binary(), 4), 3), 1))
                        return@NativeBridge j(
                            obj(
                                "result" to 0,
                                "rspbuffer" to binary(
                                    m(
                                        4,
                                        m(3, n(1, 0), m(5, "download.qq.com"), m(6, byteArrayOf(1)))
                                    )
                                )
                            )
                        )
                    }
                    assertEquals("trpc.group.long_msg_interface.MsgService.SsoSendLongMsg", params[0].asString())
                    records = ForwardReader.sections(unpack(b(b(params[1].binary(), 2), 4), true)).getValue("MultiMsg")
                    j(obj("result" to 0, "rspbuffer" to binary(m(2, m(3, "resource")))))
                }

                else -> error("Unexpected $method")
            }
        }
        val contacts = NativeContacts(bridge).apply {
            remember("9", "u_9"); remember("11", "u_11")
            for (group in listOf("42", "84")) for (uid in listOf("u_9", "u_11", ""))
                memberUpdate(group, j(nativeMap(uid to obj("role" to 2, "nick" to "Name"))))
        }
        val media = NativeMedia(bridge, j(obj("accountPath" to root)))
        val originals = OriginalMessages(media, contacts)
        val delivery = NativeDelivery(bridge, account)
        val messages = NativeMessages(bridge, account, contacts, media, delivery, history)
        val api = NativeAccount(contacts, messages, delivery, NativeExtensions(bridge, contacts, account, history)) {}
        val forward = ForwardUpload(
            bridge, account, contacts, ForwardMedia(bridge, media, "9"), originals,
            NativeReferences(bridge, history, "9")
        )

        fun row(
            id: String = "123456789",
            seq: String = "15",
            peer: Peer = Peer(2, "84"),
            sender: String = "11",
            senderUin: String = sender,
            elements: List<Any> = listOf(obj("textElement" to obj("content" to "原群正文")))
        ) = j(
            obj(
                "chatType" to peer.chatType,
                "peerUid" to peer.peerUid,
                "peerUin" to peer.peerUid.removePrefix("u_"),
                "msgId" to id,
                "msgSeq" to seq,
                "msgTime" to "100",
                "senderUin" to senderUin,
                "senderUid" to "u_$sender",
                "sendNickName" to "原发送者",
                "elements" to elements
            )
        )

        suspend fun store(row: JsonNode) =
            history.put(HistoryKey("9", row.i("chatType"), row.s("peerUid"), row.s("msgId")), j(obj("raw" to row)))

        fun reply(id: String = "123456789", seq: String = "15", peer: Peer = Peer(2, "84")) =
            QQNativeOutgoingPart.Reply(j(obj("replayMsgId" to id, "replayMsgSeq" to seq)), j(peer))

        suspend fun send(part: QQNativeOutgoingPart.Reply): QQNativeMessage {
            forward.create(Peer(2, "42"), listOf(listOf(part, QQNativeOutgoingPart.Text("回复正文"))))
            val rows = ForwardReader.rows(mapOf("MultiMsg" to records), "MultiMsg", Peer(2, "42"), "u_9")
            return messages.decode(rows.single()).message()
        }
    }

    @Test
    fun crossGroupQuoteEmbedsArchiveBodyAndUnsignedIdAndResolvesWithoutDestinationLookup(): Unit = runBlocking {
        val f = Fixture()
        val id = "18446744073709551600"
        f.store(f.row(id = id, senderUin = "0"))
        f.store(
            f.row(
                id = id,
                peer = Peer(2, "42"),
                elements = listOf(obj("textElement" to obj("content" to "错误会话")))
            )
        )
        val message = f.send(f.reply(id = id))
        val reply = message.parts.filterIsInstance<QQNativeReceivedPart.Reply>().single()
        assertEquals(id, reply.id)
        assertTrue(reply.embedded)
        val original = assertNotNull(f.api.resolve(message, reply))
        assertEquals(QQNativeConversation.Group("84"), original.conversation)
        assertEquals("11", original.receipt.sender)
        assertEquals("原发送者", original.nickname)
        assertEquals("原群正文", (original.parts.single() as QQNativeReceivedPart.Text).value)
        val framework = FrameworkMessages.decode(
            message,
            QQNativeAccount("9", f.api, f.api, "Bot") { _, _ -> error("No extension expected") })
        val quoted = assertNotNull(framework.content.reference())
        assertEquals("原群正文", quoted.content.message)
        assertEquals("84", quoted.source.id)
        assertEquals(id, quoted.id)
        assertTrue(f.queries.isEmpty())
        val encoded = b(all(b(b(f.records.single(), 3), 1), 2).first(), 45)
        assertTrue(all(encoded, 5).isNotEmpty())
        assertTrue(b(encoded, 9).isNotEmpty())
        assertEquals(id, u(b(encoded, 8), 3).toString())
    }

    @Test
    fun incomingAndOwnPrivateQuotesPreserveTheOriginalPeer(): Unit = runBlocking {
        for (sender in listOf("9", "11")) {
            val f = Fixture()
            val peer = Peer(1, "u_11")
            f.store(f.row(peer = peer, sender = sender))
            val message = f.send(f.reply(peer = peer))
            val quote = message.parts.filterIsInstance<QQNativeReceivedPart.Reply>().single()
            val original = assertNotNull(f.api.resolve(message, quote))
            assertEquals(QQNativeConversation.Private("11"), original.conversation)
            assertEquals("u_11", original.peerUid)
            assertEquals(sender, original.receipt.sender)
            assertTrue(f.queries.isEmpty())
        }
    }

    @Test
    fun missingArchiveQueriesOnlyTheSourceAndRejectsWrongPeerResults(): Unit = runBlocking {
        val f = Fixture()
        f.response = listOf(f.row(peer = Peer(2, "42")))
        assertFailsWith<IllegalStateException> { f.send(f.reply()) }
        assertTrue(f.records.isEmpty())
        assertTrue(f.queries.all { it.second == "84" })
        f.response = listOf(f.row())
        assertNotNull(f.send(f.reply()).parts.filterIsInstance<QQNativeReceivedPart.Reply>().single().original)
    }

    @Test
    fun nestedQuotesSurviveRoundTripAndCyclesAreRejected(): Unit = runBlocking {
        val f = Fixture()
        f.store(f.row(id = "2", seq = "2"))
        f.store(
            f.row(
                id = "1", seq = "1", elements = listOf(
                    obj("replyElement" to obj("replayMsgId" to "2", "replayMsgSeq" to "2")),
                    obj("textElement" to obj("content" to "中间回复"))
                )
            )
        )
        val first = f.send(f.reply("1", "1")).parts.filterIsInstance<QQNativeReceivedPart.Reply>().single().original!!
        val second = first.parts.filterIsInstance<QQNativeReceivedPart.Reply>().single().original!!
        assertEquals("原群正文", (second.parts.single() as QQNativeReceivedPart.Text).value)
        f.store(
            f.row(
                id = "2",
                seq = "2",
                elements = listOf(obj("replyElement" to obj("replayMsgId" to "1", "replayMsgSeq" to "1")))
            )
        )
        assertFailsWith<IllegalArgumentException> { f.send(f.reply("1", "1")) }
    }

    @Test
    fun quotedFilesAreEncodedAndReadUsingTheOriginalGroup(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("qq-quote-file-")
        try {
            val file = Files.writeString(root.resolve("test.txt"), "quoted file")
            val f = Fixture(root.toString())
            f.store(
                f.row(
                    elements = listOf(
                        obj(
                            "elementType" to 3, "elementId" to "file-element",
                            "fileElement" to obj("filePath" to file.toString(), "fileName" to "test.txt")
                        )
                    )
                )
            )
            val message = f.send(f.reply())
            val original = message.parts.filterIsInstance<QQNativeReceivedPart.Reply>().single().original!!
            val attachment = original.parts.single() as QQNativeReceivedPart.Attachment
            assertEquals("test.txt", attachment.name)
            assertEquals("https://download.qq.com/ftn_handler/01/?fname=", attachment.uri.toString())
            assertEquals(QQNativeConversation.Group("84"), original.conversation)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun privateQuoteCanBeLocatedByClientSequenceAndTime(): Unit = runBlocking {
        val f = Fixture()
        val peer = Peer(1, "u_11")
        f.response = listOf(
            j(
                obj(
                    "chatType" to 1, "peerUid" to "u_11", "peerUin" to "11", "senderUin" to "11", "senderUid" to "u_11",
                    "msgId" to "123456789", "msgSeq" to "15", "msgClientSeq" to "5", "msgTime" to "100",
                    "elements" to listOf(obj("textElement" to obj("content" to "客户端序列原文")))
                )
            )
        )
        val message =
            f.send(QQNativeOutgoingPart.Reply(j(obj("replyMsgClientSeq" to "5", "replyMsgTime" to "100")), j(peer)))
        assertEquals("123456789", message.parts.filterIsInstance<QQNativeReceivedPart.Reply>().single().id)
        assertEquals(listOf("getMsgByClientSeqAndTime" to "u_11"), f.queries)
    }

    @Test
    fun quoteWithoutEmbeddedBodyNeverFallsBackToDestinationSequence(): Unit = runBlocking {
        val f = Fixture()
        val row = cat(
            m(1, n(1, 11), m(2, "u_11")), m(2, n(5, 20)),
            m(3, m(1, m(2, m(45, n(1, 15), n(2, 11), n(3, 100)))))
        )
        val decoded = ForwardReader.rows(mapOf("MultiMsg" to listOf(row)), "MultiMsg", Peer(2, "42"), "u_9")
        val message = f.messages.decode(decoded.single()).message()
        val reply = message.parts.filterIsInstance<QQNativeReceivedPart.Reply>().single()
        assertTrue(reply.embedded)
        assertNull(f.api.resolve(message, reply))
        assertTrue(f.queries.isEmpty())
    }
}
