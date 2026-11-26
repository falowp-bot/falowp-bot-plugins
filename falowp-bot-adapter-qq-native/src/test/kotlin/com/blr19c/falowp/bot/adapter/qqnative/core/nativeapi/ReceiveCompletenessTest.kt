package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.RecordingAccount
import com.blr19c.falowp.bot.adapter.qqnative.core.history.*
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeReceivedPart
import com.blr19c.falowp.bot.system.api.ApiAuth
import kotlinx.coroutines.*
import java.nio.file.Files
import kotlin.test.*
import com.blr19c.falowp.bot.adapter.qqnative.adapter.message.NativeMessages as SystemMessages

class ReceiveCompletenessTest {
    private class Fixture {
        val calls = mutableListOf<String>()
        val contacts: NativeContacts
        val bridge = NativeBridge { _, args ->
            val p = j(args).path("arguments")
            val method = args["method"].toString()
            calls.add(method)
            when (method) {
                "getUidByUins" -> j(obj("uids" to nativeMap(p[0][0].asString() to "u_${p[0][0].asString()}")))
                "getMemberInfo" -> {
                    contacts.memberUpdate(
                        p[0].asString(),
                        j(nativeMap(p[1][0].asString() to obj("uin" to "11", "role" to 3, "cardName" to "管理名片")))
                    )
                    j(obj("result" to 0))
                }

                "getCoreAndBaseInfo" -> j(nativeMap("u_11" to obj("coreInfo" to obj("remark" to "好友备注"))))
                else -> error("Unexpected native method $method")
            }
        }
        val account = j(obj("id" to "9", "uid" to "u_9"))

        init {
            contacts = NativeContacts(bridge)
        }

        val messages =
            NativeMessages(bridge, account, contacts, NativeMedia(bridge, j(obj())), NativeDelivery(bridge, account))

        fun row(vararg elements: Any, group: Boolean = true) = j(
            obj(
                "chatType" to if (group) 2 else 1, "peerUid" to if (group) "42" else "u_11",
                "peerUin" to if (group) "42" else "11", "msgId" to "m", "senderUin" to "11", "senderUid" to "u_11",
                "roleType" to 2, "elements" to elements.toList()
            )
        )
    }

    @Test
    fun marketFaceAndMentionProfilesSurviveDatabaseReopen(): Unit = runBlocking(Dispatchers.IO) {
        val f = Fixture()
        val id = "0123456789abcdef0123456789abcdef"
        val raw = f.row(
            obj(
                "marketFaceElement" to obj(
                    "emojiId" to id,
                    "emojiPackageId" to 123,
                    "faceName" to "表情摘要",
                    "key" to "key"
                )
            ),
            obj("textElement" to obj("atType" to 2, "atUid" to "11", "atNtUid" to "u_11", "content" to "@旧名")),
            obj("textElement" to obj("atType" to 1, "content" to "@全体成员"))
        )
        val root = Files.createTempDirectory("qq-message-fields-")
        try {
            val key = HistoryKey("9", 2, "42", "m")
            val settings = HistorySettings(backend = "sqlite")
            val decoded = f.messages.decode(raw)
            HistoryStores.open(settings, root).put(key, j(obj("raw" to raw, "decoded" to decoded)))
            val restored = checkNotNull(HistoryStores.open(settings, root).get(key)).path("decoded").message()
            val face = restored.parts.filterIsInstance<QQNativeReceivedPart.MarketFace>().single()
            assertEquals(listOf(id, "123", "表情摘要", "key"), listOf(face.id, face.packageId, face.name, face.key))
            val content = SystemMessages.decode(restored, RecordingAccount("9", "Bot").account).content
            assertEquals("123", content.emoji.single().type)
            assertEquals("表情摘要", content.emoji.single().display)
            assertEquals(face.url, content.image.single().info)
            assertEquals("管理名片", content.at.first().nickname)
            assertEquals(ApiAuth.MANAGER, content.at.first().auth)
            assertEquals("all", content.at.last().id)
            assertEquals(1, f.calls.count { it == "getMemberInfo" })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun privateMentionsUseFriendProfilesAndProfileFailuresKeepMessageText(): Unit = runBlocking(Dispatchers.IO) {
        val f = Fixture()
        val mention = obj("textElement" to obj("atType" to 2, "atUid" to "11", "content" to "@旧名"))
        val part =
            f.messages.decode(f.row(mention, group = false)).message().parts.single() as QQNativeReceivedPart.Mention
        assertEquals("好友备注", part.name)
        assertEquals(0, part.role)
        val contacts = NativeContacts(NativeBridge { _, _ -> error("offline") })
        assertEquals("@原名", contacts.profile("11", "42", "@原名").s("nickname"))
    }

    @Test
    fun shareFieldsUseSelectedViewAndPreserveMiniAppDescription(): Unit = runBlocking(Dispatchers.IO) {
        val f = Fixture()
        val mini = j(
            obj(
                "app" to "com.tencent.miniapp_01", "view" to "detail_1", "meta" to obj(
                    "unused" to obj("title" to "错误视图"),
                    "detail_1" to obj(
                        "appId" to "appid",
                        "title" to "应用名称",
                        "desc" to "卡片内容",
                        "qqDocUrl" to "https://example.com/mini",
                        "preView" to "https://example.com/image"
                    )
                )
            )
        )
        val card = j(
            obj(
                "app" to "com.tencent.troopsharecard", "bizSrc" to "qq", "view" to "contact", "meta" to obj(
                    "contact" to obj(
                        "nickname" to "群名称",
                        "tag" to "群名片",
                        "url" to "https://example.com/group",
                        "avatar" to "https://example.com/avatar"
                    )
                )
            )
        )
        val decoded =
            f.messages.decode(f.row(*listOf(mini, card).map { obj("arkElement" to obj("bytesData" to it.toString())) }
                .toTypedArray())).message()
        val shares = SystemMessages.decode(decoded, RecordingAccount("9", "Bot").account).content.share
        assertEquals("应用名称", shares[0].appName)
        assertEquals("卡片内容", shares[0].title)
        assertEquals("https://example.com/mini", shares[0].sourceUrl)
        assertEquals("https://example.com/image", shares[0].preview.info)
        assertEquals("群名片", shares[1].appName)
        assertEquals("群名称", shares[1].title)
        assertEquals("qq", shares[1].appId)
        assertEquals("https://example.com/avatar", shares[1].preview.info)
        assertEquals("卡片内容", decoded.parts.filterIsInstance<QQNativeReceivedPart.Share>()[0].description)
    }

    @Test
    fun forwardedMarketFacesUseTheSameReceiveModel(): Unit = runBlocking(Dispatchers.IO) {
        val id = ByteArray(16) { it.toByte() }
        val face = Wire.m(6, Wire.m(1, "转发表情"), Wire.m(4, id), Wire.n(5, 456), Wire.m(7, "key"))
        val row = Wire.m(3, Wire.m(1, Wire.m(2, face)))
        val elements = ForwardReader.elements(row)
        val f = Fixture()
        val message = f.messages.decode(f.row(*elements.toTypedArray())).message()
        val received = assertIs<QQNativeReceivedPart.MarketFace>(message.parts.single())
        assertEquals(id.hex(), received.id)
        assertEquals("456", received.packageId)
        assertEquals("转发表情", received.name)
    }

}
