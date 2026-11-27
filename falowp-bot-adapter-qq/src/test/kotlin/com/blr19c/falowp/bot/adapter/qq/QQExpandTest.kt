package com.blr19c.falowp.bot.adapter.qq

import com.blr19c.falowp.bot.adapter.qq.expand.model.QQKeyboard
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQMediaType

import com.blr19c.falowp.bot.adapter.qq.core.client.QQClient
import com.blr19c.falowp.bot.adapter.qq.core.config.QQConfig

import com.blr19c.falowp.bot.adapter.qq.api.*
import com.blr19c.falowp.bot.adapter.qq.expand.*
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import tools.jackson.databind.JsonNode
import java.util.Base64
import kotlin.test.*

class QQExpandTest {
    private fun message() = ReceiveMessage.empty().copy(
        id = "expand-message",
        source = ReceiveMessage.Source("group", SourceTypeEnum.GROUP),
        adapter = ReceiveMessage.Adapter(
            "QQ",
            Json.readJsonNode("""{"d":{"message_scene":{"ext":["msg_idx=REFIDX_expand"]}}}""")
        )
    )

    private fun fixture(block: suspend (BotApi, MutableList<Pair<String, JsonNode>>) -> Unit) = runBlocking {
        val requests = mutableListOf<Pair<String, JsonNode>>()
        val headers = headersOf(HttpHeaders.ContentType, "application/json")
        val http = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("getAppAccessToken")) {
                respond("""{"access_token":"token","expires_in":7200}""", headers = headers)
            } else {
                requests.add(request.url.encodedPath to Json.readJsonNode((request.body as? TextContent)?.text ?: "{}"))
                respond(
                    if (request.url.encodedPath.endsWith("files"))
                        """{"file_info":"file-token","file_uuid":"uuid","ttl":300}"""
                    else """{"id":"sent"}""", headers = headers
                )
            }
        })
        try {
            QQBotApiSupport.configure(QQClient(QQConfig("app", "secret"), http), "app")
            block(QQBotApi(message(), this::class), requests)
        } finally {
            http.close()
        }
    }

    @Test
    fun `markdown keyboard and normal replies share context and sequence`() = fixture { bot, sent ->
        assertSame(bot, bot.qq())
        assertSame(bot, bot.qq)
        bot.qq.sendMarkdown("# 标题", keyboard = QQKeyboard(id = "keyboard"), reference = true)
        bot.sendReply("normal")
        val markdown = sent[0].second
        assertEquals(2, markdown.path("msg_type").asInt())
        assertFalse(markdown.has("content"))
        assertEquals("# 标题", markdown.path("markdown").path("content").safeString())
        assertEquals("keyboard", markdown.path("keyboard").path("id").safeString())
        assertEquals("REFIDX_expand", markdown.path("message_reference").path("message_id").safeString())
        assertEquals(markdown.path("msg_seq").asInt() + 1, sent[1].second.path("msg_seq").asInt())
    }

    @Test
    fun `file bytes upload and send use private destination without old reply context`() = fixture { bot, sent ->
        val data = byteArrayOf(0, 1, 2, -1)
        bot.qq().sendMedia(data, "example.bin", QQMediaType.FILE, "user", SourceTypeEnum.PRIVATE, content = "附件")
        assertEquals("/v2/users/user/files", sent[0].first)
        val upload = sent[0].second
        assertEquals(4, upload.path("file_type").asInt())
        assertEquals("example.bin", upload.path("file_name").safeString())
        assertContentEquals(data, Base64.getDecoder().decode(upload.path("file_data").safeString()))
        assertFalse(upload.path("srv_send_msg").asBoolean())
        assertEquals("/v2/users/user/messages", sent[1].first)
        val message = sent[1].second
        assertEquals(7, message.path("msg_type").asInt())
        assertEquals("file-token", message.path("media").path("file_info").safeString())
        assertEquals("附件", message.path("content").safeString())
        assertFalse(message.has("msg_id"))
    }

    @Test
    fun `media rejects wrong destination expiration and invalid direct uploads`() = fixture { bot, sent ->
        val media = bot.qq.uploadMedia("https://example.com/video.mp4", QQMediaType.VIDEO)
        assertFailsWith<IllegalArgumentException> { bot.qq.sendMedia(media, sourceId = "other") }
        assertFailsWith<IllegalArgumentException> { bot.qq.sendMedia(media.copy(expiresAt = 0)) }
        assertFailsWith<IllegalArgumentException> { bot.qq.uploadMedia(byteArrayOf(), "empty.bin") }
        assertFailsWith<IllegalArgumentException> { bot.qq.uploadMedia(byteArrayOf(1), "") }
        assertEquals(1, sent.size)
        assertEquals(2, sent[0].second.path("file_type").asInt())
    }

    @Test
    fun `extension validates target types and keyboard before network`() = fixture { bot, sent ->
        assertFailsWith<IllegalArgumentException> { bot.qq.sendMarkdown("text", sourceType = SourceTypeEnum.UNKNOWN) }
        assertFailsWith<IllegalArgumentException> { bot.qq.sendMarkdown(" ") }
        assertFailsWith<IllegalArgumentException> { QQKeyboard() }
        assertFailsWith<IllegalArgumentException> { QQKeyboard("id", Json.readJsonNode("{}")) }
        assertFailsWith<IllegalArgumentException> { bot.qq.apiRequest("//other-host/path") }
        assertTrue(sent.isEmpty())
    }
}
