package com.blr19c.falowp.bot.adapter.qq.core.webhook

import com.blr19c.falowp.bot.adapter.qq.core.config.QQConfig

import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.*

class QQWebhookTest {
    @Test
    fun `callback challenge validates app and returns official signature`() = testApplication {
        val webhook = QQWebhook(QQConfig("11111111", "DG5g3B4j9X2KOErG")) {}
        application { routing { webhook.mount(this) } }
        val payload = """{"op":13,"d":{"plain_token":"Arq0D5A61EgUu4OxUvOp","event_ts":"1725442341"}}"""
        assertEquals(HttpStatusCode.Unauthorized, client.post("/qq/webhook") { setBody(payload) }.status)
        val response = client.post("/qq/webhook") { header("X-Bot-Appid", "11111111"); setBody(payload) }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.readJsonNode(response.bodyAsText())
        assertEquals("Arq0D5A61EgUu4OxUvOp", body.path("plain_token").safeString())
        assertEquals(
            "87befc99c42c651b3aac0278e71ada338433ae26fcb24307bdc5ad38c1adc2d01bcfcadc0842edac85e85205028a1132afe09280305f13aa6909ffc2d652c706",
            body.path("signature").safeString()
        )
    }

    @Test
    fun `callbacks require valid signature of exact raw bytes and acknowledge unknown events`() = testApplication {
        var delivered = 0
        val webhook = QQWebhook(QQConfig("app", "secret")) { event ->
            assertEquals("FUTURE_EVENT", event.path("t").safeString())
            delivered++
        }
        application { routing { webhook.mount(this) } }
        val payload = """{ "op":0, "t":"FUTURE_EVENT", "d":{} }"""
        val signature = QQSignature("secret").sign("12345", payload.toByteArray())
        suspend fun post(body: String, signed: Boolean) = client.post("/qq/webhook") {
            header("X-Bot-Appid", "app")
            if (signed) {
                header("X-Signature-Timestamp", "12345")
                header("X-Signature-Ed25519", signature)
            }
            setBody(body)
        }
        assertEquals(HttpStatusCode.Unauthorized, post(payload, false).status)
        assertEquals(HttpStatusCode.Unauthorized, post(payload + " ", true).status)
        assertEquals(0, delivered)
        val response = post(payload, true)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(12, Json.readJsonNode(response.bodyAsText()).path("op").asInt())
        assertEquals(1, delivered)
        assertEquals(HttpStatusCode.BadRequest, post("not-json", true).status)
    }
}
