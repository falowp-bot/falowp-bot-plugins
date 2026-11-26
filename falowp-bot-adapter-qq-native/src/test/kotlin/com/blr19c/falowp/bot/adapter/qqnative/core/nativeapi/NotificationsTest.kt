package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.QQNativeNoticeDetail
import kotlinx.coroutines.runBlocking
import tools.jackson.databind.JsonNode
import kotlin.test.*

class NotificationsTest {
    private class Fixture {
        val notices = mutableListOf<JsonNode>()
        private val bridge = NativeBridge { _, args ->
            val params = j(args).path("arguments")
            when (args["method"]) {
                "getUinByUids" -> j(
                    obj(
                        "uins" to nativeMap(
                            params[0][0].asString() to params[0][0].asString().removePrefix("u_")
                        )
                    )
                )

                "getSingleMsg" -> j(
                    obj(
                        "msgList" to listOf(
                            obj(
                                "msgId" to "message",
                                "msgSeq" to params[1].asString()
                            )
                        )
                    )
                )

                else -> error(args["method"].toString())
            }
        }
        val notifications = NativeNotifications(NativeContacts(bridge)) { _, data -> notices.add(data) }
        suspend fun system(type: Int, body: ByteArray, sub: Int = 0) =
            notifications.system(cat(m(2, n(1, type), n(2, sub)), m(3, m(2, body))))

        suspend fun message(id: String, vararg elements: Any?, updates: Boolean = false) = notifications.message(
            j(
                obj(
                    "chatType" to 2,
                    "peerUid" to "42",
                    "msgId" to id,
                    "senderUid" to "u_11",
                    "elements" to elements.toList()
                )
            ), id, updates
        )
    }

    @Test
    fun partialCardUpdatesKeepRoleAndAllowEmptyCard(): Unit = runBlocking {
        val contacts = NativeContacts(NativeBridge { _, _ -> error("Cached profile expected") })
        contacts.memberUpdate(
            "42",
            j(nativeMap("u_11" to obj("uin" to "11", "role" to 3, "cardName" to "名片", "nick" to "昵称")))
        )
        contacts.memberUpdate("42", j(nativeMap("u_11" to obj("cardName" to ""))))
        val profile = contacts.profile("11", "42")
        assertEquals(3, profile.i("role"))
        assertEquals("昵称", profile.s("nickname"))
    }

    @Test
    fun administratorAndProfileLikeDecodeProtocolFields(): Unit = runBlocking {
        val f = Fixture()
        f.system(44, cat(n(1, 42), m(4, m(2, m(1, "u_11")))))
        f.system(44, cat(n(1, 42), m(4, m(1, m(1, "u_11")))))
        assertTrue((f.notices[0].notice()!!.detail as QQNativeNoticeDetail.GroupAdmin).enabled)
        assertFalse((f.notices[1].notice()!!.detail as QQNativeNoticeDetail.GroupAdmin).enabled)
        assertEquals("0", f.notices[0].s("actor"))
        f.system(
            528,
            cat(n(1, 0), n(2, 203), m(203, m(14, n(2, 123), m(3, m(1, "赞了你 10 次"), n(3, 11), m(5, "小明"))))),
            39
        )
        val like = f.notices.last().notice()!!.detail as QQNativeNoticeDetail.ProfileLike
        assertEquals(10, like.count)
        assertEquals(123, like.time)
    }

    @Test
    fun essenceAndReactionsHandleAddRemoveAndKeepSequenceWithoutOriginal(): Unit = runBlocking {
        val f = Fixture()
        for (flag in listOf(1, 1, 2, 1)) {
            f.system(732, cat(ByteArray(7), m(33, n(1, 42), n(2, 19), n(4, flag), n(5, 11), n(6, 12))), 16)
        }
        val essence = f.notices.map { it.notice()!!.detail as QQNativeNoticeDetail.GroupEssence }
        assertEquals(listOf(true, false, true), essence.map { it.added })
        assertEquals("", essence[0].messageId)
        assertEquals("19", essence[0].sequence)
        for (flag in listOf(1, 1, 2)) f.system(
            732, cat(
                ByteArray(7), n(4, 42), n(13, 35),
                m(44, m(1, m(1, m(2, n(1, 19)), m(3, m(1, "66"), n(3, 4), m(4, "u_11"), n(5, flag)))))
            ), 16
        )
        val reaction = f.notices.takeLast(2).map { it.notice()!!.detail as QQNativeNoticeDetail.MessageReaction }
        assertEquals(listOf(true, false), reaction.map { it.added })
        assertEquals(4, reaction[0].reactions.single().count)
    }

    @Test
    fun muteTitleFileAndInputUseOwnEventModels(): Unit = runBlocking {
        val f = Fixture()
        for (duration in listOf(60, 0)) f.message(
            "mute$duration", obj(
                "grayTipElement" to obj(
                    "groupElement" to obj(
                        "shutUp" to obj(
                            "admin" to obj("uid" to "u_12"),
                            "member" to obj("uid" to ""),
                            "duration" to duration.toString()
                        )
                    )
                )
            )
        )
        assertEquals(
            listOf(-1L, 0L),
            f.notices.map { (it.notice()!!.detail as QQNativeNoticeDetail.GroupMute).duration })
        f.message(
            "title", obj(
                "grayTipElement" to obj(
                    "jsonGrayTipElement" to obj(
                        "jsonStr" to j(
                            obj(
                                "items" to listOf(
                                    obj("txt" to "授予"),
                                    obj("param" to listOf("11")),
                                    obj("txt" to "新的"),
                                    obj("txt" to "测试头衔"),
                                    obj("txt" to "头衔")
                                )
                            )
                        ).toString()
                    )
                )
            )
        )
        assertEquals("测试头衔", (f.notices.last().notice()!!.detail as QQNativeNoticeDetail.GroupTitle).title)
        f.message(
            "file",
            obj(
                "fileElement" to obj(
                    "fileUuid" to "/f",
                    "fileName" to "a.txt",
                    "fileSize" to "9000000000",
                    "fileBizId" to 102
                )
            )
        )
        assertEquals(9000000000, (f.notices.last().notice()!!.detail as QQNativeNoticeDetail.GroupFileUpload).size)
        f.notifications.input(j(obj("fromUin" to "u_11", "eventType" to 2, "statusText" to "正在输入")), "input1")
        assertEquals("正在输入", (f.notices.last().notice()!!.detail as QQNativeNoticeDetail.InputStatus).text)
        assertEquals("11", f.notices.last().s("target"))
    }

    @Test
    fun cardBaselineDoesNotEmitAndClearingCardIsAChange(): Unit = runBlocking {
        val f = Fixture()
        suspend fun card(value: String) =
            f.notifications.members("42", j(nativeMap("u_11" to obj("uin" to "11", "cardName" to value))))
        card("旧名片")
        assertTrue(f.notices.isEmpty())
        card("新名片")
        card("新名片")
        card("")
        assertEquals(2, f.notices.size)
        val last = f.notices.last().notice()!!.detail as QQNativeNoticeDetail.GroupCard
        assertEquals("新名片", last.oldCard)
        assertEquals("", last.newCard)
    }

    @Test
    fun xmlReactionCannotReadExternalEntities(): Unit = runBlocking {
        val f = Fixture()
        val valid = "<gtip><qq jp='11'/><url msgseq='19'/><face id='66'/></gtip>"
        f.message("xml", obj("grayTipElement" to obj("xmlElement" to obj("templId" to "10382", "content" to valid))))
        assertEquals(
            "66",
            (f.notices.last().notice()!!.detail as QQNativeNoticeDetail.MessageReaction).reactions.single().emojiId
        )
        val malicious = "<!DOCTYPE gtip [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><gtip>&xxe;</gtip>"
        f.message(
            "bad",
            obj("grayTipElement" to obj("xmlElement" to obj("templId" to "10382", "content" to malicious)))
        )
        assertEquals(1, f.notices.size)
    }
}
