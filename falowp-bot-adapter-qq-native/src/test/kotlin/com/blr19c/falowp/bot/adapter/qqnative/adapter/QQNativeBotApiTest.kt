package com.blr19c.falowp.bot.adapter.qqnative.adapter

import com.blr19c.falowp.bot.adapter.qqnative.RecordingAccount
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.*
import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.system.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import tools.jackson.databind.json.JsonMapper
import kotlin.test.*

class QQNativeBotApiTest {
    @Test
    fun albumListFollowsCursorAndPreservesNativeMediaFields(): Unit = runBlocking(Dispatchers.IO) {
        val json = JsonMapper.builder().build()
        val cursors = mutableListOf<String>()
        var repeat = false
        val account = RecordingAccount("7", "test", extension = { op, args ->
            assertEquals("get_group_album_media_list", op)
            val cursor = args["attach_info"].toString()
            cursors.add(cursor)
            when (cursor) {
                "" -> json.readTree("""{"media_list":[{"image":{"lloc":"one","default_url":{"url":"https://example.com/1"}}}],"next_has_more":true,"next_attach_info":"next"}""")
                else -> json.readTree("""{"media_list":[{"image":{"lloc":"two","default_url":{"url":"https://example.com/2"}}}],"next_has_more":$repeat,"next_attach_info":"next"}""")
            }
        }).account
        val api = QQNativeBotApi(
            ReceiveMessage.empty().copy(self = BotSelf.Default(id = account.id)),
            QQNativeBotApiTest::class,
            account
        )
        val rows = api.getGroupAlbumMediaList(groupId = "42", albumId = "album")
        assertEquals(listOf("", "next"), cursors)
        assertEquals(2, rows.size)
        assertEquals("one", rows.first().mediaId)
        assertEquals("two", rows.last().mediaId)
        assertEquals("https://example.com/2", rows.last().url)
        assertEquals("two", rows.last().raw.path("image").path("lloc").asString())
        repeat = true
        assertFailsWith<IllegalStateException> { api.getGroupAlbumMediaList(groupId = "42", albumId = "album") }
    }

    @Test
    fun pluginCallsRoutePrivateRepliesBroadcastAndNestedGroupMessages(): Unit = runBlocking(Dispatchers.IO) {
        val sends = mutableListOf<RecordingAccount.Sent>()
        val account = RecordingAccount("7", "test", listOf("21", "22", "21"), onSend = { sends.add(it) }).account
        val incoming = ReceiveMessage.empty().copy(
            self = BotSelf.Default(id = account.id),
            source = ReceiveMessage.Source("11", SourceTypeEnum.PRIVATE)
        )
        val api: BotApi = QQNativeBotApi(incoming, QQNativeBotApiTest::class, account)
        api.sendReply("收到")
        api.sendAllGroup(SendMessage.builder("广播").build())
        api.sendGroup(
            SendMessage.builder("甲").build(),
            SendMessage.builder("乙").build(),
            sourceId = "21",
            forward = true
        )
        assertEquals(
            listOf(
                QQNativeConversation.Private("11"), QQNativeConversation.Group("21"),
                QQNativeConversation.Group("22"), QQNativeConversation.Group("21")
            ), sends.map { it.to })
        assertEquals(listOf(false, false, false, true), sends.map { it.forward })
        assertEquals(
            listOf("甲", "乙"),
            sends.last().chains.map { (it.parts.single() as QQNativeOutgoingPart.Text).value })
        assertEquals("test", api.self().nickname)
    }

    @Test
    fun readMarkersUseMessageIdsWithoutLeakingCurrentConversation(): Unit = runBlocking(Dispatchers.IO) {
        val requests = mutableListOf<Map<String, Any?>>()
        val account = RecordingAccount("7", "test", extension = { operation, parameters ->
            assertEquals("mark_msg_as_read", operation)
            requests.add(parameters)
            JsonMapper.builder().build().nullNode()
        }).account
        val api = QQNativeBotApi(
            ReceiveMessage.empty().copy(
                self = BotSelf.Default("7"),
                source = ReceiveMessage.Source("current", SourceTypeEnum.GROUP)
            ), javaClass.kotlin, account
        )
        api.markMsgAsRead(messageId = "elsewhere")
        api.markGroupMsgAsRead(messageId = "group-message")
        api.markPrivateMsgAsRead(messageId = "private-message")
        assertTrue(requests.all { it["target"] == null })
        assertEquals(listOf("elsewhere", "group-message", "private-message"), requests.map { it["message_id"] })
        assertEquals(listOf(null, true, false), requests.map { it["group"] })
        api.markMsgAsRead()
        assertEquals("current", requests.last()["target"])
        assertFailsWith<IllegalArgumentException> { api.markMsgAsRead(groupId = "42", userId = "11") }
    }

}
