package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.history.*
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.QQNativeNoticeDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import tools.jackson.databind.JsonNode
import java.nio.file.Files
import kotlin.test.*

class NoticeRecoveryTest {
    private class Fixture(history: MessageHistory = MemoryMessageHistory()) {
        var fileUrlAvailable = false
        var related: JsonNode? = null
        val emitted = mutableListOf<Pair<String, JsonNode>>()
        val contacts: NativeContacts
        val bridge = NativeBridge { _, args ->
            val p = j(args).path("arguments")
            when (args["method"]) {
                "getUinByUids" -> j(
                    obj(
                        "uins" to nativeMap(
                            p[0][0].asString() to p[0][0].asString().removePrefix("u_")
                        )
                    )
                )

                "getUidByUins" -> j(obj("uids" to nativeMap(p[0][0].asString() to "u_${p[0][0].asString()}")))
                "getMemberInfo" -> {
                    val uid = p[1][0].asString()
                    contacts.memberUpdate(
                        p[0].asString(),
                        j(nativeMap(uid to obj("nick" to "Nickname", "cardName" to "Group card", "role" to 3)))
                    )
                    j(obj("result" to 0))
                }

                "getCoreAndBaseInfo" -> j(nativeMap(p[1][0].asString() to obj("coreInfo" to obj("nick" to "Private name"))))
                "getSingleMsg" -> j(obj("msgList" to listOfNotNull(related)))
                "getGroupFileInfo" -> {
                    check(fileUrlAvailable) { "文件查询失败" }
                    j(
                        obj(
                            "result" to 0, "groupFileInfoResult" to obj(
                                "retCode" to 0,
                                "fileInfo" to obj("fileId" to "file", "busId" to 102)
                            )
                        )
                    )
                }

                "sendSsoCmdReqByContend" -> j(
                    obj(
                        "result" to 0, "rspbuffer" to binary(
                            Wire.m(
                                4, Wire.m(
                                    3,
                                    Wire.n(1, 0), Wire.m(5, "download.qq.com"), Wire.m(6, byteArrayOf(1, 2))
                                )
                            )
                        )
                    )
                )

                else -> error("Unexpected ${args["method"]}")
            }
        }
        val account = j(obj("id" to "9", "uid" to "u_9"))

        init {
            contacts = NativeContacts(bridge)
        }

        val events = NativeEvents(
            account,
            contacts,
            NativeMessages(bridge, account, contacts, NativeMedia(bridge, j(obj())), NativeDelivery(bridge, account)),
            history,
            { emitted.add("message" to it) },
            { emitted.add("notice" to it) }
        )

        fun row(id: String, gray: Map<String, Any?>) = j(
            obj(
                "chatType" to 2,
                "peerUid" to "42",
                "senderUid" to "u_11",
                "senderUin" to "11",
                "msgId" to id,
                "elements" to listOf(obj("grayTipElement" to gray))
            )
        )

        fun packet(type: Int, reason: Int, member: String, operator: ByteArray) = cat(
            Wire.m(2, Wire.n(1, type)),
            Wire.m(3, Wire.m(2, Wire.n(1, 42), Wire.m(3, member), Wire.n(4, reason), Wire.m(5, operator)))
        )
    }

    @Test
    fun replayAndOutOfOrderMessagesDoNotRollBackCards(): Unit = runBlocking(Dispatchers.IO) {
        val f = Fixture()
        fun row(id: String, seq: Long, card: String) = j(
            obj(
                "chatType" to 2, "peerUid" to "42", "senderUid" to "u_11",
                "senderUin" to "11", "msgId" to id, "msgSeq" to seq, "sendMemberName" to card,
                "elements" to listOf(obj("textElement" to obj("content" to id)))
            )
        )
        f.events.receive(row("first", 10, "旧名片"), false)
        f.events.receive(row("second", 20, "新名片"), false)
        f.events.receive(row("first", 10, "旧名片"), false)
        f.events.receive(row("late", 15, "旧名片"), false)
        val cards = f.emitted.filter { it.second.s("kind") == "groupCard" }
        assertEquals(1, cards.size)
        assertEquals("新名片", cards.single().second.s("newCard"))
    }

    @Test
    fun brokenJsonAndXmlDoNotBlockTextOrLaterNotifications(): Unit = runBlocking(Dispatchers.IO) {
        val f = Fixture()
        f.events.receive(
            j(
                obj(
                    "chatType" to 2, "peerUid" to "42", "senderUid" to "u_11", "senderUin" to "11",
                    "msgId" to "broken", "elements" to listOf(
                        obj("grayTipElement" to obj("jsonGrayTipElement" to obj("jsonStr" to "{broken"))),
                        obj("grayTipElement" to obj("xmlElement" to obj("templId" to "10382", "content" to "<broken"))),
                        obj(
                            "grayTipElement" to obj(
                                "groupElement" to obj(
                                    "shutUp" to obj(
                                        "admin" to obj("uid" to "u_12"),
                                        "duration" to 60
                                    )
                                )
                            )
                        ),
                        obj("textElement" to obj("content" to "正常消息"))
                    )
                )
            ), false
        )
        assertEquals("正常消息", f.emitted.single { it.first == "message" }.second.list("parts").single().s("value"))
        assertEquals(1, f.emitted.count { it.second.s("kind") == "groupMute" })
    }

    @Test
    fun dedicatedNoticesCarryOriginalsAndDownloadUrlsButSurviveLookupFailure(): Unit = runBlocking(Dispatchers.IO) {
        val f = Fixture()
        f.related = j(
            obj(
                "chatType" to 2,
                "peerUid" to "42",
                "senderUid" to "u_11",
                "senderUin" to "11",
                "msgId" to "original",
                "msgSeq" to "19",
                "elements" to listOf(obj("textElement" to obj("content" to "原文")))
            )
        )
        fun essence(seq: Long) = cat(
            Wire.m(2, Wire.n(1, 732), Wire.n(2, 16)), Wire.m(
                3, Wire.m(
                    2,
                    ByteArray(7), Wire.m(33, Wire.n(1, 42), Wire.n(2, seq), Wire.n(4, 1), Wire.n(5, 11), Wire.n(6, 12))
                )
            )
        )
        f.events.system(essence(19))
        val notice = f.emitted.last().second
        assertEquals("original", notice.s("messageId"))
        assertEquals("原文", notice.path("message").list("parts").single().s("value"))
        assertNotNull((notice.notice()!!.detail as QQNativeNoticeDetail.GroupEssence).message)
        f.related = null
        f.events.system(essence(20))
        assertNull((f.emitted.last().second.notice()!!.detail as QQNativeNoticeDetail.GroupEssence).message)
        fun file(id: String) = j(
            obj(
                "chatType" to 2, "peerUid" to "42", "senderUid" to "u_11", "senderUin" to "11", "msgId" to id,
                "elements" to listOf(
                    obj(
                        "fileElement" to obj(
                            "fileUuid" to "file",
                            "fileName" to "test.txt",
                            "fileSize" to 5
                        )
                    )
                )
            )
        )
        f.events.receive(file("file1"), false)
        assertNull((f.emitted.first { it.second.s("kind") == "groupFileUpload" }.second.notice()!!.detail as QQNativeNoticeDetail.GroupFileUpload).url)
        f.fileUrlAvailable = true
        f.events.receive(file("file2"), false)
        assertEquals(
            "https://download.qq.com/ftn_handler/0102/?fname=",
            f.emitted.last { it.second.s("kind") == "groupFileUpload" }.second.s("url")
        )
    }

    @Test
    fun rawMembershipPushesDistinguishInviteLeaveKickSelfAndDisband(): Unit = runBlocking(Dispatchers.IO) {
        val f = Fixture()
        f.events.system(f.packet(33, 131, "u_11", "u_12".toByteArray()))
        f.events.system(f.packet(34, 130, "u_11", byteArrayOf()))
        f.events.system(f.packet(34, 131, "u_11", "u_12".toByteArray()))
        f.events.system(f.packet(34, 3, "u_9", Wire.m(1, Wire.m(1, "u_12"))))
        f.events.system(f.packet(34, 129, "u_9", byteArrayOf()))
        assertEquals(listOf("invite", "leave", "kick", "kick_me", "disband"), f.emitted.map { it.second.s("action") })
        assertEquals("12", f.emitted[3].second.s("actor"))
        assertEquals("Group card", f.emitted.first().second.path("actorInfo").s("nickname"))
        assertEquals(3, f.emitted.first().second.path("actorInfo").i("role"))
    }

    @Test
    fun nestedGrayInviteIsDecodedAndDuplicateSystemPushDoesNotPublishTwice(): Unit = runBlocking(Dispatchers.IO) {
        val f = Fixture()
        val add = obj(
            "showType" to 5,
            "otherInviteOther" to obj("invited" to obj("uid" to "u_11"), "inviter" to obj("uid" to "u_12"))
        )
        f.events.receive(f.row("join", obj("groupElement" to obj("type" to 1, "memberAdd" to add))), false)
        f.events.system(f.packet(33, 131, "u_11", "u_12".toByteArray()))
        assertEquals(1, f.emitted.size)
        assertEquals("invite", f.emitted.single().second.s("action"))
        assertEquals("11", f.emitted.single().second.s("member"))
    }

    @Test
    fun restartRecoversRevokedOriginalAndUnknownOriginalStillEmitsMarkedEvent(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("qq-revoke-")
        try {
            val first = Fixture(HistoryStores.open(HistorySettings(backend = "sqlite"), root))
            first.events.receive(
                j(
                    obj(
                        "chatType" to 2,
                        "peerUid" to "42",
                        "senderUid" to "u_11",
                        "senderUin" to "11",
                        "msgId" to "old",
                        "elements" to listOf(obj("textElement" to obj("content" to "重启前原文")))
                    )
                ), false
            )
            val second = Fixture(HistoryStores.open(HistorySettings(backend = "sqlite"), root))
            second.events.receive(second.row("old", obj("revokeElement" to obj("operatorUid" to "u_12"))), true)
            val original = second.emitted.single().second.path("original")
            assertEquals("重启前原文", original.path("parts")[0].s("value"))
            assertTrue(original.message().contentAvailable)
            second.events.receive(second.row("missing", obj("revokeElement" to obj("operatorUid" to "u_12"))), true)
            val missing = second.emitted.last().second.path("original").message()
            assertEquals("missing", missing.receipt.id)
            assertFalse(missing.contentAvailable)
            assertTrue(missing.parts.isEmpty())
            // 撤回更新不能覆盖已保存的原文
            val third = Fixture(HistoryStores.open(HistorySettings(backend = "sqlite"), root))
            third.events.receive(third.row("old", obj("revokeElement" to obj("operatorUid" to "u_12"))), true)
            assertEquals("重启前原文", third.emitted.single().second.path("original").path("parts")[0].s("value"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun unsupportedAndMalformedMessagesAreSavedBeforeDecoding(): Unit = runBlocking(Dispatchers.IO) {
        val history = MemoryMessageHistory()
        val f = Fixture(history)
        val unsupported = j(
            obj(
                "chatType" to 2, "peerUid" to "42", "senderUid" to "u_11", "senderUin" to "11", "msgId" to "unknown",
                "elements" to listOf(obj("futureElement" to obj("payload" to "原始字段")))
            )
        )
        f.events.receive(unsupported, false)
        assertEquals(unsupported, history.get(HistoryKey("9", 2, "42", "unknown"))?.path("raw"))
        val broken = j(
            obj(
                "chatType" to 2, "peerUid" to "42", "senderUid" to "u_11", "senderUin" to "11", "msgId" to "broken",
                "elements" to listOf(
                    obj(
                        "packetMedia" to obj(
                            "kind" to "image",
                            "info" to obj($$"$buffer" to "invalid base64!")
                        )
                    )
                )
            )
        )
        assertFails { f.events.receive(broken, false) }
        assertEquals(broken, history.get(HistoryKey("9", 2, "42", "broken"))?.path("raw"))
    }
}
