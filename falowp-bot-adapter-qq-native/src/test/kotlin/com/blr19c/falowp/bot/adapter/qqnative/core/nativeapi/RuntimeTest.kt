package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import tools.jackson.databind.JsonNode
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class RuntimeTest {
    @Test
    fun identityLookupPropagatesCancellation(): Unit = runBlocking(Dispatchers.IO) {
        val contacts = NativeContacts(NativeBridge { _, _ -> throw CancellationException("cancelled") })
        contacts.groupUpdate(0, emptyList())
        assertFailsWith<CancellationException> { contacts.uin("u_friend") }
        assertFailsWith<CancellationException> { contacts.has("123") }
    }

    private class Fixture : AutoCloseable {
        val sent = mutableListOf<JsonNode>()
        val stored = linkedMapOf<String, JsonNode>()
        val resources = linkedMapOf<String, ByteArray>()
        val outgoing = Channel<QQNativeMessage>(Channel.UNLIMITED)
        val notices = Channel<QQNativeNotice>(Channel.UNLIMITED)
        var active = true
        val calls = mutableListOf<Pair<String, JsonNode>>()
        var next = 0
        var forwardedRows = emptyList<JsonNode>()
        val runtime = NativeRuntime(
            { _, args ->
                val request = j(args)
                val method = request.s("method")
                val params = request.path("arguments")
                calls.add(method to params)
                when (method) {
                    "getUidByUins" -> j(obj("uids" to nativeMap(params[0][0].asString() to "u_${params[0][0].asString()}")))
                    "getUinByUids" -> j(
                        obj(
                            "uins" to nativeMap(
                                params[0][0].asString() to params[0][0].asString().removePrefix("u_")
                            )
                        )
                    )

                    "getServerTime" -> j(1700000000)
                    "generateMsgUniqueId" -> j((++next).toString())
                    "sendMsg" -> {
                        val peer = params[1]
                        val id = next.toString()
                        val row = j(
                            obj(
                                "msgId" to id,
                                "msgSeq" to id,
                                "msgTime" to "100",
                                "senderUin" to "9",
                                "senderUid" to "u_9",
                                "sendStatus" to 2,
                                "guildId" to peer.s("guildId"),
                                "peerUid" to peer.s("peerUid"),
                                "peerUin" to peer.s("peerUid").removePrefix("u_"),
                                "chatType" to peer.i("chatType"),
                                "elements" to params[2]
                            )
                        )
                        sent.add(row)
                        stored[id] = row
                        event("onMsgInfoListUpdate", listOf(row))
                        j(obj("result" to 0))
                    }

                    "multiForwardMsgWithComment" -> {
                        forwardedRows = params[0].toList().map { stored.getValue(it.s("msgId")) }
                        val destination = params[2]
                        val row = j(
                            obj(
                                "msgId" to "native-outer",
                                "senderUid" to "u_9",
                                "sendStatus" to 2,
                                "chatType" to destination.i("chatType"),
                                "peerUid" to destination.s("peerUid"),
                                "elements" to listOf(
                                    obj(
                                        "elementType" to 16,
                                        "multiForwardMsgElement" to obj(
                                            "resId" to "native-resource",
                                            "fileName" to "native-file"
                                        )
                                    )
                                )
                            )
                        )
                        event("onMsgInfoListUpdate", listOf(row))
                        j(obj("result" to 0))
                    }

                    "getMultiMsg" -> j(obj("result" to 0, "msgList" to forwardedRows))
                    "getMsgsByMsgId" -> j(
                        obj(
                            "result" to 0,
                            "msgList" to params[1].mapNotNull { stored[it.asString()] })
                    )

                    "getSingleMsg", "getMsgByClientSeqAndTime" -> j(obj("msgList" to emptyList<Any>()))
                    "getGroupList" -> {
                        event("onGroupListUpdate", 0, listOf(obj("groupCode" to "42"), obj("groupCode" to "84")))
                        j(obj("result" to 0))
                    }

                    "getBuddyListV2" -> j(obj("data" to listOf(obj("buddyUids" to listOf("u_11")))))
                    "getMemberInfo" -> {
                        event(
                            "onMemberInfoChange",
                            params[0].asString(),
                            0,
                            nativeMap(params[1][0].asString() to obj("role" to 2))
                        )
                        j(obj("result" to 0))
                    }

                    "sendNudge" -> j(obj("result" to 0))
                    "sendSsoCmdReqByContend" -> {
                        val payload = params[1].binary()
                        val command = params[0].asString()
                        val reply = if (command == "OidbSvcTrpcTcp.0xED3_1") n(
                            3,
                            0
                        ) else if (command.endsWith("SsoSendLongMsg")) {
                            val id = "resource-${resources.size}"
                            resources[id] = b(b(payload, 2), 4)
                            m(2, m(3, id))
                        } else {
                            assertTrue(command.endsWith("SsoRecvLongMsg"))
                            m(1, m(4, resources.getValue(s(b(payload, 1), 2))))
                        }
                        j(obj("result" to 0, "rspbuffer" to binary(reply)))
                    }

                    else -> error("Unexpected native call $method")
                }
            },
            j(obj("id" to "9", "uid" to "u_9", "nickname" to "Tester", "paths" to emptyMap<String, String>())),
            { outgoing.trySend(it) }, { notices.trySend(it) })
        val account = runtime.account("Tester") { check(active) { "Account changed" } }

        fun event(name: String, vararg args: Any?) {
            runtime.event(j(obj("name" to name, "arguments" to args.toList())))
        }

        suspend fun send(
            chains: List<List<QQNativeOutgoingPart>>,
            forward: Boolean = false,
            source: String = "group",
            target: String = "42",
            reference: QQNativeMessageReceipt? = null
        ): List<QQNativeMessageReceipt> {
            val to = if (source == "group") QQNativeConversation.Group(target) else QQNativeConversation.Private(target)
            val original = reference?.let { QQNativeMessage(to, target, it, "", 0, emptyList()) }
            return account.messages.send(
                to, *chains.map { QQNativeOutgoingChain(it) }.toTypedArray(),
                replyTo = original, forward = forward
            )
        }

        suspend fun read(root: String, parent: String = root) =
            account.messages.readForward(QQNativeConversation.Group("42"), root, parent)

        override fun close() {
            runtime.close()
            outgoing.close()
            notices.close()
        }
    }

    @Test
    fun specialMessagesAndOriginalElementsUseNormalDelivery(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val original =
                j(obj("elementType" to 1, "elementId" to "received-element", "textElement" to obj("content" to "原文")))
            f.send(
                listOf(
                    listOf(
                        QQNativeOutgoingPart.Original("9", j(obj("elements" to listOf(original)))),
                        QQNativeOutgoingPart.Game(true, 6),
                        QQNativeOutgoingPart.MarketFace("0123456789abcdef0123456789abcdef", 123, "key", "表情")
                    )
                )
            )
            val elements = f.sent.single().list("elements")
            assertEquals("原文", elements[0].path("textElement").s("content"))
            assertEquals("", elements[0].s("elementId"))
            assertEquals("received-element", original.s("elementId"))
            assertEquals("6", elements[1].path("faceElement").s("resultId"))
            assertEquals("key", elements[2].path("marketFaceElement").s("key"))
            assertFailsWith<IllegalArgumentException> {
                f.send(
                    listOf(
                        listOf(
                            QQNativeOutgoingPart.Original(
                                "other",
                                j(obj("elements" to listOf(original)))
                            )
                        )
                    )
                )
            }
            val originalForward =
                f.send(listOf(listOf(QQNativeOutgoingPart.Original("9", j(obj("elements" to listOf(original)))))), true)
                    .single().id
            assertEquals("原文", (f.read(originalForward).single().parts.single() as QQNativeReceivedPart.Text).value)
            val card = f.send(
                listOf(
                    listOf(
                        QQNativeOutgoingPart.MarketFace(
                            "0123456789abcdef0123456789abcdef",
                            123,
                            "key",
                            "表情"
                        )
                    )
                ), true
            ).single().id
            val market = f.read(card).single().parts.single() as QQNativeReceivedPart.MarketFace
            assertEquals("123", market.packageId)
            assertEquals("key", market.key)
        }
    }

    @Test
    fun typingCallbackTravelsThroughRuntimeAndIsNotDiscarded(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            f.event("onInputStatusPush", obj("fromUin" to "u_11", "eventType" to 1, "statusText" to "正在输入"))
            val notice = withTimeout(5000.milliseconds) { f.notices.receive() }
            assertEquals("11", notice.actor)
            assertEquals("正在输入", (notice.detail as QQNativeNoticeDetail.InputStatus).text)
        }
    }

    @Test
    fun customArkSurvivesDirectSendAndCompressedForwardRoundTrip(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val json =
                """{"app":"com.tencent.miniapp_01","meta":{"detail_1":{"title":"卡片测试","qqdocurl":"https://example.com/"}}}"""
            val chain = listOf(listOf(QQNativeOutgoingPart.Ark(json)))
            f.send(chain)
            assertEquals(json, f.sent.single().path("elements")[0].path("arkElement").s("bytesData"))
            val forward = f.send(chain, true).single().id
            val share = f.read(forward).single().parts.single() as QQNativeReceivedPart.Share
            assertEquals("卡片测试", share.title)
            assertEquals("https://example.com/", share.url)
        }
    }

    @Test
    fun partitionsQuoteEverySentSegmentAndPrivatePeerIsResolved(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val result = f.send(
                listOf(
                    listOf(
                        QQNativeOutgoingPart.Text("a"),
                        QQNativeOutgoingPart.Partition,
                        QQNativeOutgoingPart.Text("b")
                    )
                ),
                source = "private",
                target = "11",
                reference = QQNativeMessageReceipt("original", "8", "99", "11")
            )
            assertEquals(2, result.size)
            assertTrue(f.sent.all {
                it.s("peerUid") == "u_11" && it.path("elements")[0].path("replyElement").s("replayMsgId") == "original"
            })
            assertEquals(listOf("a", "b"), f.sent.map { it.path("elements")[1].path("textElement").s("content") })
        }
    }

    @Test
    fun newForwardAlwaysUploadsRequestedContentsInsteadOfReusingOldReceipts(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val chain = listOf(
                QQNativeOutgoingPart.Text("签到汇总"),
                QQNativeOutgoingPart.Partition,
                QQNativeOutgoingPart.Text("第一条"),
                QQNativeOutgoingPart.Partition,
                QQNativeOutgoingPart.Text("第二条")
            )
            f.send(listOf(chain))
            val card = f.send(listOf(chain), true).first().id
            assertEquals(1, f.resources.size)
            val rows = f.read(card)
            assertEquals(
                listOf("签到汇总", "第一条", "第二条"),
                rows.map { (it.parts.first() as QQNativeReceivedPart.Text).value })
            assertEquals(4, f.sent.size)
        }
    }

    @Test
    fun multipleChainsProduceOneVisibleCardWithReadableNestedContents(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val root = f.send(
                listOf(
                    listOf(QQNativeOutgoingPart.Text("链甲")),
                    listOf(QQNativeOutgoingPart.Text("链乙"))
                ), true
            ).first().id
            assertEquals(1, f.sent.size)
            val parents = f.read(root)
            assertEquals(2, parents.size)
            assertEquals(
                listOf("链甲", "链乙"),
                parents.map { (f.read(root, it.receipt.id).first().parts.first() as QQNativeReceivedPart.Text).value })
        }
    }

    @Test
    fun namedCardKeepsOriginalResourceAndSupportsFreshRead(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val root = f.send(listOf(listOf(QQNativeOutgoingPart.Text("正文"))), true).first().id
            val copy = f.account.messages.sendForwardCard(
                QQNativeConversation.Group("42"), root, QQNativeConversation.Group("42"),
                QQNativeForwardCardStyle(title = "新标题", summary = "摘要", preview = listOf("预览"))
            )
            val original = NativeDelivery.cardData(f.stored.getValue(root))!!
            val renamed = NativeDelivery.cardData(f.stored.getValue(copy.id))!!
            assertEquals(
                original.path("meta").path("detail").s("resid"),
                renamed.path("meta").path("detail").s("resid")
            )
            assertEquals("新标题", renamed.path("meta").path("detail").s("source"))
            assertEquals("正文", (f.read(copy.id).first().parts.first() as QQNativeReceivedPart.Text).value)
        }
    }

    @Test
    fun directoryWaitsForNativeCallbackAndDistinguishesFriends(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            assertEquals(listOf("42", "84"), f.account.contacts.groups().map { it.id })
            assertTrue(f.account.contacts.contains("11"))
            assertFalse(f.account.contacts.contains("12"))
            f.event("onGroupListUpdate", 3, listOf(obj("groupCode" to "42")))
            assertEquals(listOf("84"), f.account.contacts.groups().map { it.id })
        }
    }

    @Test
    fun nativeMessagesAreDecodedOnceWithIdentityAndRole(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val row = obj(
                "chatType" to 2,
                "msgId" to "500",
                "peerUid" to "42",
                "senderUid" to "u_11",
                "senderUin" to "0",
                "elements" to listOf(obj("textElement" to obj("content" to "你好", "atType" to 0)))
            )
            f.event("onRecvMsg", listOf(row, row))
            val data = withTimeout(3000.milliseconds) { f.outgoing.receive() }
            assertEquals("11", data.receipt.sender)
            assertEquals(2, data.role)
            assertEquals("你好", (data.parts.first() as QQNativeReceivedPart.Text).value)
            assertNull(withTimeoutOrNull(100.milliseconds) { f.outgoing.receive() })
        }
    }

    @Test
    fun malformedMessageDoesNotDiscardLaterRowsInTheSameCallback(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val base = obj("chatType" to 2, "peerUid" to "42", "senderUid" to "u_11", "senderUin" to "11")
            val bad = base + obj(
                "msgId" to "bad",
                "elements" to listOf(
                    obj(
                        "packetMedia" to obj(
                            "kind" to "image",
                            "info" to obj($$"$buffer" to "invalid base64!")
                        )
                    )
                )
            )
            val good =
                base + obj("msgId" to "good", "elements" to listOf(obj("textElement" to obj("content" to "后续消息"))))
            f.event("onRecvMsg", listOf(bad, good))
            val data = withTimeout(3000.milliseconds) { f.outgoing.receive() }
            assertEquals("good", data.receipt.id)
            assertEquals("后续消息", (data.parts.first() as QQNativeReceivedPart.Text).value)
        }
    }

    @Test
    fun requestsFilterCompletedAndDuplicateEvents(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val request = obj("friendUid" to "u_11", "reqTime" to 7, "reqType" to 1, "extWords" to "hello")
            f.event("onBuddyReqChange", obj("buddyReqs" to listOf(request, request, request + obj("isDecide" to true))))
            val event = withTimeout(3000.milliseconds) { f.notices.receive() }
            assertIs<QQNativeNoticeDetail.FriendRequest>(event.detail)
            assertEquals("11", event.actor)
            assertNull(withTimeoutOrNull(100.milliseconds) { f.notices.receive() })
        }
    }

    @Test
    fun privateNudgeCannotTargetAnotherContact(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            assertFailsWith<IllegalArgumentException> {
                f.send(
                    listOf(
                        listOf(
                            QQNativeOutgoingPart.Nudge("12")
                        )
                    ), source = "private", target = "11"
                )
            }
        }
    }

    @Test
    fun nudgeUsesSeparateGroupAndPrivateConversationFields(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            f.send(listOf(listOf(QQNativeOutgoingPart.Nudge("11"))))
            val group = b(f.calls.last().second[1].binary(), 4)
            assertEquals(11uL, Wire.u(group, 1))
            assertEquals(42uL, Wire.u(group, 2))
            assertEquals(0uL, Wire.u(group, 5))
            f.send(listOf(listOf(QQNativeOutgoingPart.Nudge("11"))), source = "private", target = "11")
            val private = b(f.calls.last().second[1].binary(), 4)
            assertEquals(11uL, Wire.u(private, 1))
            assertEquals(11uL, Wire.u(private, 5))
            assertEquals(0uL, Wire.u(private, 2))
        }
    }

    @Test
    fun existingForwardAcknowledgementComparesNativeRowsBeforeServerNormalization(): Unit =
        runBlocking(Dispatchers.IO) {
            Fixture().use { f ->
                val text = f.send(listOf(listOf(QQNativeOutgoingPart.Text("文本")))).first().id
                val card = f.send(listOf(listOf(QQNativeOutgoingPart.Text("内层"))), true).first().id
                val receipt = f.account.messages.forward(
                    QQNativeConversation.Group("42"), QQNativeConversation.Group("42"),
                    listOf(text, card).map { QQNativeMessageReceipt(it, "", "", "") }
                )
                assertEquals("native-outer", receipt.id)
                assertEquals(1, f.calls.count { it.first == "multiForwardMsgWithComment" })
            }
        }

    @Test
    fun accountBindingRejectsEveryOperationAfterCloseOrAccountChange(): Unit = runBlocking(Dispatchers.IO) {
        for (close in listOf(false, true)) Fixture().use { f ->
            val peer = QQNativeConversation.Group("42")
            val original = QQNativeMessage(peer, "42", QQNativeMessageReceipt("1", "2", "3", "11"), "", 0, emptyList())
            if (close) f.runtime.close() else f.active = false
            assertFailsWith<IllegalStateException> { f.account.contacts.groups() }
            assertFailsWith<IllegalStateException> { f.account.contacts.contains("11") }
            assertFailsWith<IllegalStateException> {
                f.account.messages.send(
                    peer,
                    QQNativeOutgoingChain(QQNativeOutgoingPart.Text("拒绝"))
                )
            }
            assertFailsWith<IllegalStateException> { f.account.messages.forward(peer, peer, listOf(original.receipt)) }
            assertFailsWith<IllegalStateException> {
                f.account.messages.sendForwardCard(
                    peer,
                    "1",
                    peer,
                    QQNativeForwardCardStyle()
                )
            }
            assertFailsWith<IllegalStateException> { f.account.messages.readForward(peer, "1") }
            assertFailsWith<IllegalStateException> {
                f.account.messages.resolve(
                    original,
                    QQNativeReceivedPart.Reply("1", "2", "3", "4")
                )
            }
            assertFailsWith<IllegalStateException> { f.account.extensionCall("get_login_info", emptyMap()) }
            assertTrue(f.calls.isEmpty())
        }
    }

    @Test
    fun referenceLookupPreservesPeerAndAllFallbackKeys(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val original = QQNativeMessage(
                QQNativeConversation.Private("11"), "u_11",
                QQNativeMessageReceipt("101", "3", "99", "11"), "", 0, emptyList()
            )
            assertNull(f.account.messages.resolve(original, QQNativeReceivedPart.Reply("100", "2", "1", "98")))
            val lookups =
                f.calls.filter { it.first in listOf("getMsgsByMsgId", "getSingleMsg", "getMsgByClientSeqAndTime") }
            assertEquals(listOf("getMsgsByMsgId", "getSingleMsg", "getMsgByClientSeqAndTime"), lookups.map { it.first })
            assertTrue(lookups.all { it.second[0].s("peerUid") == "u_11" && it.second[0].i("chatType") == 1 })
            assertEquals("100", lookups[0].second[1][0].asString())
            assertEquals("2", lookups[1].second[1].asString())
            assertEquals(listOf("1", "98"), lookups[2].second.toList().drop(1).map { it.asString() })
        }
    }

    @Test
    fun replyDoesNotCrossConversationTypesWithTheSameId(): Unit = runBlocking(Dispatchers.IO) {
        Fixture().use { f ->
            val original = QQNativeMessage(
                QQNativeConversation.Group("42"), "42",
                QQNativeMessageReceipt("101", "3", "99", "11"), "", 0, emptyList()
            )
            for (to in listOf(QQNativeConversation.Group("42"), QQNativeConversation.Private("42"))) {
                f.account.messages.send(
                    to,
                    QQNativeOutgoingChain(QQNativeOutgoingPart.Text("引用")),
                    replyTo = original
                )
            }
            assertEquals("101", f.sent[0].path("elements")[0].path("replyElement").s("replayMsgId"))
            assertFalse(f.sent[1].path("elements").any { it.hasValue("replyElement") })
        }
    }

}
