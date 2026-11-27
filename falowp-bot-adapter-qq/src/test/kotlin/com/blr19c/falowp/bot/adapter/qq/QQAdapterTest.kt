package com.blr19c.falowp.bot.adapter.qq

import com.blr19c.falowp.bot.adapter.qq.core.client.QQClient
import com.blr19c.falowp.bot.adapter.qq.core.client.QQApiException
import com.blr19c.falowp.bot.adapter.qq.core.config.QQConfig
import com.blr19c.falowp.bot.adapter.qq.core.state.QQState

import com.blr19c.falowp.bot.adapter.qq.core.webhook.QQSignature
import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.core.message.QQMessageConverter
import com.blr19c.falowp.bot.adapter.qq.api.QQBotApiSupport
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import tools.jackson.databind.JsonNode
import kotlin.test.*

class QQAdapterTest {
    private val config = QQConfig("app", "secret")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val self = BotSelf.Default("bot")

    private fun event(
        type: String = "GROUP_AT_MESSAGE_CREATE",
        content: String = " /hello https://example.com/a/b "
    ): JsonNode =
        Json.readJsonNode(
            Json.toJsonString(
                mapOf(
                    "op" to 0, "t" to type, "d" to mapOf(
                        "id" to "message", "content" to content, "group_openid" to "group",
                        "author" to mapOf(
                            "id" to "fallback",
                            "member_openid" to "member",
                            "user_openid" to "user",
                            "member_role" to "owner"
                        ),
                        "message_scene" to mapOf("ext" to listOf("msg_idx=REFIDX_test", "auth_token=unused")),
                        "attachments" to listOf(mapOf("url" to "//example.com/p.png", "content_type" to "image/png"))
                    )
                )
            )
        )

    @Test
    fun `group conversion preserves URL and maps attachments and role`() {
        val message = assertNotNull(QQMessageConverter.convert(event(), self))
        assertEquals("hello https://example.com/a/b", message.content.message)
        assertEquals("member", message.sender.id)
        assertEquals(ApiAuth.MANAGER, message.sender.auth)
        assertEquals("https://example.com/p.png", message.content.image.single().info)
        assertTrue(message.atMe())
        assertEquals(SourceTypeEnum.GROUP, message.source.type)
    }

    @Test
    fun `private event uses user openid and full group event does not fabricate mention`() {
        val private = assertNotNull(QQMessageConverter.convert(event("C2C_MESSAGE_CREATE"), self))
        assertEquals("user", private.source.id)
        assertTrue(private.private())
        val group = assertNotNull(QQMessageConverter.convert(event("GROUP_MESSAGE_CREATE", "https://a/b"), self))
        assertFalse(group.atMe())
        assertEquals("https://a/b", group.content.message)
        assertNull(QQMessageConverter.convert(event("AT_MESSAGE_CREATE"), self))
    }

    @Test
    fun `target isolation and reference index`() {
        val message = assertNotNull(QQMessageConverter.convert(event(), self))
        val api = QQBotApi(message, this::class)
        assertEquals("message", api.replyId("group", SourceTypeEnum.GROUP))
        assertEquals("message", api.replyId("member", SourceTypeEnum.PRIVATE))
        assertNull(api.replyId("another-group", SourceTypeEnum.GROUP))
        assertNull(api.replyId("group", SourceTypeEnum.PRIVATE))
        assertEquals("REFIDX_test", api.referenceId())
    }

    @Test
    fun `signature matches official challenge vector and rejects tampering`() {
        val signature = QQSignature("DG5g3B4j9X2KOErG")
        val body = "Arq0D5A61EgUu4OxUvOp".toByteArray()
        val expected =
            "87befc99c42c651b3aac0278e71ada338433ae26fcb24307bdc5ad38c1adc2d01bcfcadc0842edac85e85205028a1132afe09280305f13aa6909ffc2d652c706"
        assertEquals(expected, signature.sign("1725442341", body))
        assertTrue(signature.verify("1725442341", expected, body))
        assertFalse(signature.verify("1725442342", expected, body))
        assertFalse(signature.verify("1725442341", expected, body + 1.toByte()))
        assertFalse(signature.verify("1725442341", "z".repeat(128), body))
        assertFalse(signature.verify(null, expected, body))
    }

    @Test
    fun `concurrent token consumers refresh once and renew before expiration`() = runBlocking {
        var calls = 0
        var now = 0L
        val http = HttpClient(MockEngine {
            calls++
            respond("{\"access_token\":\"token$calls\",\"expires_in\":\"7200\"}", headers = jsonHeaders)
        })
        try {
            val client = QQClient(config, http) { now }
            val tokens = (1..30).map { async { client.authorization() } }.awaitAll()
            assertEquals(setOf("QQBot token1"), tokens.toSet())
            assertEquals(1, calls)
            now = 7140000
            assertEquals("QQBot token2", client.authorization())
            assertEquals(2, calls)
        } finally {
            http.close()
        }
    }

    @Test
    fun `token business errors with HTTP 200 are failures`() = runBlocking {
        val http = HttpClient(MockEngine {
            respond(
                "{\"code\":100016,\"message\":\"invalid secret\"}",
                headers = jsonHeaders
            )
        })
        try {
            val error = assertFailsWith<QQApiException> { QQClient(config, http).authorization() }
            assertEquals(100016L, error.code)
            assertFalse(error.message!!.contains("invalid secret"))
        } finally {
            http.close()
        }
    }

    @Test
    fun `REST error exposes trace and is not retried`() = runBlocking {
        var sends = 0
        val http = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("getAppAccessToken")) {
                respond("{\"access_token\":\"token\",\"expires_in\":7200}", headers = jsonHeaders)
            } else {
                sends++
                assertEquals("QQBot token", request.headers[HttpHeaders.Authorization])
                respond("{\"err_code\":40034005,\"message\":\"expired\",\"trace_id\":\"trace\"}", headers = jsonHeaders)
            }
        })
        try {
            val error = assertFailsWith<QQApiException> {
                QQClient(config, http).request(
                    "/v2/groups/g/messages",
                    mapOf("content" to "hi")
                )
            }
            assertEquals(40034005L, error.code)
            assertEquals("trace", error.traceId)
            assertEquals(1, sends)
        } finally {
            http.close()
        }
    }

    @Test
    fun `media uses actual target and active messages omit reply fields`() = runBlocking {
        val requests = mutableListOf<Pair<String, JsonNode>>()
        val http = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("getAppAccessToken")) {
                respond("{\"access_token\":\"token\",\"expires_in\":7200}", headers = jsonHeaders)
            } else {
                requests.add(request.url.encodedPath to Json.readJsonNode((request.body as TextContent).text))
                respond(
                    if (request.url.encodedPath.endsWith("files")) "{\"file_info\":\"uploaded\",\"ttl\":300}" else "{\"id\":\"sent\"}",
                    headers = jsonHeaders
                )
            }
        })
        try {
            QQBotApiSupport.configure(QQClient(config, http), "app")
            val api = QQBotApi(assertNotNull(QQMessageConverter.convert(event(), self)), this::class)
            api.sendGroup(
                SendMessage.builder().image("https://example.com/p.png").build(),
                sourceId = "other",
                reference = true
            )
            assertEquals(listOf("/v2/groups/other/files", "/v2/groups/other/messages"), requests.map { it.first })
            assertFalse(requests[0].second.path("srv_send_msg").asBoolean())
            val sent = requests[1].second
            assertEquals("uploaded", sent.path("media").path("file_info").safeString())
            assertFalse(sent.has("msg_id"))
            assertFalse(sent.has("message_reference"))
            assertFalse(sent.path("media").has("ttl"))
        } finally {
            http.close()
        }
    }

    @Test
    fun `multiple API instances share reply sequence and use reference index`() = runBlocking {
        val sent = mutableListOf<JsonNode>()
        val http = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("getAppAccessToken")) respond(
                "{\"access_token\":\"token\",\"expires_in\":7200}",
                headers = jsonHeaders
            )
            else {
                sent.add(Json.readJsonNode((request.body as TextContent).text))
                respond("{\"id\":\"sent\"}", headers = jsonHeaders)
            }
        })
        try {
            QQBotApiSupport.configure(QQClient(config, http), "app")
            val message = assertNotNull(QQMessageConverter.convert(event(), self))
            QQBotApi(message, this::class).sendReply("first", reference = true)
            QQBotApi(message, this::class).sendReply("second")
            assertEquals(sent[0].path("msg_seq").asInt() + 1, sent[1].path("msg_seq").asInt())
            assertEquals("message", sent[0].path("msg_id").safeString())
            assertEquals("REFIDX_test", sent[0].path("message_reference").path("message_id").safeString())
        } finally {
            http.close()
        }
    }

    @Test
    fun `state expires and concurrent sequences remain unique`() = runBlocking {
        var now = 0L
        val state = QQState { now }
        val values = (1..100).map { async(Dispatchers.Default) { state.next("same") } }.awaitAll()
        assertEquals((1..100).toSet(), values.toSet())
        now = 66 * 60 * 1000L
        assertEquals(1, state.next("same"))
    }
}
