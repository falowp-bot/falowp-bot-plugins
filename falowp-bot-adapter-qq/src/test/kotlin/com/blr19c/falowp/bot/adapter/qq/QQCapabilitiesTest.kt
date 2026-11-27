package com.blr19c.falowp.bot.adapter.qq

import com.blr19c.falowp.bot.adapter.qq.api.*
import com.blr19c.falowp.bot.adapter.qq.core.client.QQClient
import com.blr19c.falowp.bot.adapter.qq.core.config.QQConfig
import com.blr19c.falowp.bot.adapter.qq.core.event.QQEventDispatcher
import com.blr19c.falowp.bot.adapter.qq.core.message.QQMessageConverter
import com.blr19c.falowp.bot.adapter.qq.expand.*
import com.blr19c.falowp.bot.adapter.qq.expand.event.*
import com.blr19c.falowp.bot.adapter.qq.expand.model.*
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.*
import tools.jackson.databind.JsonNode
import java.nio.file.Files
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID
import kotlin.test.*

class QQCapabilitiesTest {
    private data class Call(val path: String, val body: JsonNode, val request: io.ktor.client.request.HttpRequestData)

    private fun fixture(
        response: (String) -> String = { """{"id":"stream-id"}""" },
        block: suspend (QQBotApi, MutableList<Call>) -> Unit,
    ) = runBlocking {
        val calls = java.util.Collections.synchronizedList(mutableListOf<Call>())
        val http = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            val result = if (path.endsWith("getAppAccessToken")) {
                """{"access_token":"token","expires_in":7200}"""
            } else {
                val body = (request.body as? TextContent)?.text ?: "{}"
                calls.add(Call(path, Json.readJsonNode(body), request))
                response(path)
            }
            respond(result, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            QQBotApiSupport.configure(QQClient(QQConfig("app", "secret"), http), "app")
            val message = ReceiveMessage.empty().copy(
                id = UUID.randomUUID().toString(),
                source = ReceiveMessage.Source("user", SourceTypeEnum.PRIVATE)
            )
            block(QQBotApi(message, this::class), calls)
        } finally {
            http.close()
        }
    }

    @Test
    fun `typing card and wakeup use distinct message fields`() = fixture { bot, calls ->
        bot.sendTyping(12)
        bot.sendCard(QQCard("标题", "摘要", "https://example.com", "https://example.com/cover"))
        bot.sendText("召回", reply = QQReply.Wakeup)
        bot.sendMarkdown("主动", reply = QQReply.Active)
        assertEquals(6, calls[0].body.path("msg_type").asInt())
        assertEquals(12, calls[0].body.path("input_notify").path("input_second").asInt())
        assertEquals(8, calls[1].body.path("msg_type").asInt())
        assertEquals("tuwen", calls[1].body.path("card").path("type").safeString())
        assertTrue(calls[2].body.path("is_wakeup").asBoolean())
        assertFalse(calls[2].body.has("msg_id"))
        assertFalse(calls[2].body.has("event_id"))
        assertFalse(calls[3].body.has("msg_id"))
        assertEquals(calls[0].body.path("msg_seq").asInt() + 1, calls[1].body.path("msg_seq").asInt())
    }

    @Test
    @Suppress("HttpUrlsUsage") // 验证拒绝非 HTTPS 卡片链接
    fun `invalid cards typing and group streams fail before requests`() = fixture { bot, calls ->
        assertFailsWith<IllegalArgumentException> { bot.sendTyping(61) }
        assertFailsWith<IllegalArgumentException> { QQCard("", "摘要", "https://example.com", "https://example.com") }
        assertFailsWith<IllegalArgumentException> {
            QQCard(
                "标题",
                "摘要",
                "http://example.com",
                "https://example.com"
            )
        }
        val group = QQBotApi(
            bot.receiveMessage.copy(source = ReceiveMessage.Source("group", SourceTypeEnum.GROUP)),
            this::class
        )
        assertFailsWith<IllegalArgumentException> { group.stream() }
        assertFailsWith<IllegalArgumentException> { group.sendTyping() }
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `stream serializes concurrent fragments with one reply sequence`() = fixture { bot, calls ->
        val stream = bot.stream()
        stream.append("首片")
        coroutineScope { (1..3).map { async { stream.append("续片$it") } }.awaitAll() }
        stream.append("", finished = true)
        assertEquals((0..4).toList(), calls.map { it.body.path("index").asInt() })
        assertFalse(calls.first().body.has("stream_msg_id"))
        assertTrue(calls.drop(1).all { it.body.path("stream_msg_id").safeString() == "stream-id" })
        assertEquals(1, calls.map { it.body.path("msg_seq").asInt() }.distinct().size)
        assertEquals(10, calls.last().body.path("input_state").asInt())
        assertFailsWith<IllegalStateException> { stream.append("结束后") }
        assertEquals(5, calls.size)
    }

    @Test
    fun `stream stops after ambiguous server failure`() =
        fixture(response = { """{"code":500,"message":"failed"}""" }) { bot, calls ->
            val stream = bot.stream()
            assertFails { stream.replace("内容") }
            assertFailsWith<IllegalStateException> { stream.replace("内容") }
            assertEquals(1, calls.size)
        }

    @Test
    fun `interaction event preserves both ids and replies with outer id`() = fixture { _, calls ->
        val outer = UUID.randomUUID().toString()
        val raw =
            Json.readJsonNode("""{"t":"INTERACTION_CREATE","id":"$outer","d":{"id":"callback","type":11,"scene":"group","group_openid":"group","group_member_openid":"member","data":{"resolved":{"button_data":"confirm"}}}}""")
        var context: QQBotApi? = null
        var event: QQInteractionEvent? = null
        repeat(2) {
            QQEventDispatcher.dispatch(raw) { bot, incoming ->
                assertNull(context)
                context = bot
                event = incoming as QQInteractionEvent
            }
        }
        val interaction = assertNotNull(event)
        val bot = assertNotNull(context)
        assertEquals("member", interaction.actor.id)
        assertEquals("group", interaction.source.id)
        bot.respondInteraction(interaction)
        bot.sendReply("完成")
        assertEquals("/interactions/callback", calls[0].path)
        assertEquals(HttpMethod.Put, calls[0].request.method)
        assertEquals(outer, calls[1].body.path("event_id").safeString())
        assertFalse(calls[1].body.has("msg_id"))
        bot.sendText("跨会话", "other", SourceTypeEnum.GROUP)
        assertFalse(calls[2].body.has("event_id"))
    }

    @Test
    fun `all relationship switches publish and guild interactions are excluded`() = fixture { _, _ ->
        val types = listOf(
            "FRIEND_ADD", "FRIEND_DEL", "C2C_MSG_REJECT", "C2C_MSG_RECEIVE",
            "GROUP_ADD_ROBOT", "GROUP_DEL_ROBOT", "GROUP_MSG_REJECT", "GROUP_MSG_RECEIVE"
        )
        val received = mutableListOf<Plugin.Listener.Event>()
        for (type in types) {
            val field = if (type.startsWith("GROUP")) "group_openid" else "openid"
            val raw = Json.readJsonNode("""{"t":"$type","id":"${UUID.randomUUID()}","d":{"$field":"target"}}""")
            assertTrue(QQEventDispatcher.dispatch(raw) { _, event -> received.add(event) })
        }
        assertEquals(types, received.map { (it as QQRelationEvent).type })
        QQEventDispatcher.dispatch(Json.readJsonNode("""{"t":"INTERACTION_CREATE","d":{"scene":"guild","user_openid":"user"}}""")) { _, _ ->
            fail(
                "频道事件不应发布"
            )
        }
    }

    @Test
    fun `card share and nested quoted snapshot map to framework content`() = runBlocking {
        val raw =
            Json.readJsonNode("""{"t":"C2C_MESSAGE_CREATE","d":{"id":"message","author":{"user_openid":"user"},"content":"分享","ark_data":{"ark_type":"miniapp","fields":{"title":"标题","source":"应用","preview":"https://example.com/p","jump_url":"https://example.com"}},"message_scene":{"ext":["ref_msg_idx=ref"]},"msg_elements":[{"msg_elements":[{"msg_idx":"ref","content":"引用正文","attachments":[{"content_type":"image/png","url":"https://example.com/img"}]}]}]}}""")
        val message = QQMessageConverter.convert(raw, BotSelf.Default("app"))!!
        assertEquals("标题", message.content.share.single().title)
        val quoted = message.content.reference()!!
        assertEquals("引用正文", quoted.content.message)
        assertEquals(1, quoted.content.image.size)
        assertEquals("", quoted.id)
        assertEquals("", quoted.sender.id)
    }

    @Test
    fun `chunk upload hashes bytes confirms each part and merges without leaking credentials`() =
        fixture(response = { path ->
            when {
                path.endsWith("upload_prepare") -> """{"upload_id":"upload","block_size":"3","parts":[{"index":0,"presigned_url":"https://storage.example.com/part0"},{"index":1,"presigned_url":"https://storage.example.com/part1"}],"upload_config":{"concurrency":1}}"""
                path.endsWith("files") -> """{"file_info":"info","file_uuid":"uuid","ttl":300}"""
                else -> "{}"
            }
        }) { bot, calls ->
            val file = withContext(Dispatchers.IO) { Files.createTempFile("qq-upload", ".bin") }
            val bytes = byteArrayOf(0, 1, 2, 3, 4)
            try {
                withContext(Dispatchers.IO) { Files.write(file, bytes) }
                val media = bot.uploadMedia(file)
                assertEquals("info", media.fileInfo)
                assertEquals("user", media.sourceId)
                val expected = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes))
                assertEquals(expected, calls.first().body.path("md5").safeString())
                assertEquals(expected, calls.first().body.path("md5_10m").safeString())
                val uploads = calls.filter { it.path.startsWith("/part") }.sortedBy { it.path }
                assertEquals(2, uploads.size)
                assertContentEquals(
                    bytes.take(3).toByteArray(),
                    (uploads[0].request.body as OutgoingContent.ByteArrayContent).bytes()
                )
                assertContentEquals(
                    bytes.drop(3).toByteArray(),
                    (uploads[1].request.body as OutgoingContent.ByteArrayContent).bytes()
                )
                assertTrue(uploads.all { it.request.headers[HttpHeaders.Authorization] == null && it.request.method == HttpMethod.Put })
                val confirms = calls.filter { it.path.endsWith("upload_part_finish") }
                    .sortedBy { it.body.path("part_index").asInt() }
                assertEquals(listOf(0, 1), confirms.map { it.body.path("part_index").asInt() })
                assertEquals(listOf("3", "2"), confirms.map { it.body.path("block_size").safeString() })
                assertEquals("upload", calls.last().body.path("upload_id").safeString())
            } finally {
                withContext(Dispatchers.IO) { Files.deleteIfExists(file) }
            }
        }

    @Test
    fun `incomplete upload plan is rejected before uploading or merging`() = fixture(response = {
        """{"upload_id":"upload","block_size":"1","parts":[{"index":0,"presigned_url":"https://storage.example.com/part0"}]}"""
    }) { bot, calls ->
        val file = withContext(Dispatchers.IO) { Files.createTempFile("qq-upload", ".bin") }
        try {
            withContext(Dispatchers.IO) { Files.write(file, byteArrayOf(1, 2)) }
            assertFailsWith<IllegalStateException> { bot.uploadMedia(file) }
            assertEquals(1, calls.size)
        } finally {
            withContext(Dispatchers.IO) { Files.deleteIfExists(file) }
        }
    }
}
