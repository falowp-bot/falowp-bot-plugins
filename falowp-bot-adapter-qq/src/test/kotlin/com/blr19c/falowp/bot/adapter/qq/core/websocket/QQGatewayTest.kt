package com.blr19c.falowp.bot.adapter.qq.core.websocket

import com.blr19c.falowp.bot.adapter.qq.core.client.QQClient
import com.blr19c.falowp.bot.adapter.qq.core.config.QQConfig
import com.blr19c.falowp.bot.system.json.Json
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class QQGatewayTest {
    @Test
    fun `gateway identifies heartbeats and reidentifies invalid session without registering twice`() = testApplication {
        val connections = AtomicInteger()
        val registrations = AtomicInteger()
        val heartbeatSeen = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        application {
            install(WebSockets)
            routing {
                post("/token") {
                    call.respondText(
                        """{"access_token":"token","expires_in":7200}""",
                        ContentType.Application.Json
                    )
                }
                get("/gateway") {
                    call.respondText(
                        """{"url":"wss://localhost/events"}""",
                        ContentType.Application.Json
                    )
                }
                webSocket("/events") {
                    val attempt = connections.incrementAndGet()
                    send("""{"op":10,"d":{"heartbeat_interval":50}}""")
                    val identify = Json.readJsonNode((incoming.receive() as Frame.Text).readText())
                    assertEquals(2, identify.path("op").asInt())
                    assertEquals((1 shl 25) or (1 shl 26), identify.path("d").path("intents").asInt())
                    send("""{"op":0,"s":1,"t":"READY","d":{"session_id":"session","user":{"id":"bot"}}}""")
                    if (attempt == 1) {
                        val heartbeat = Json.readJsonNode((incoming.receive() as Frame.Text).readText())
                        assertEquals(1, heartbeat.path("op").asInt())
                        assertEquals(1, heartbeat.path("d").asInt())
                        heartbeatSeen.complete(Unit)
                        send("""{"op":11}""")
                        send("""{"op":9}""")
                    } else {
                        send("""{"op":0,"s":2,"t":"TEST_EVENT","d":{}}""")
                    }
                    for (frame in incoming) {
                        if (frame is Frame.Text) send("""{"op":11}""")
                    }
                }
            }
        }
        val http = createClient { install(ClientWebSockets) }
        val config = QQConfig("app", "secret", apiUrl = "http://localhost", tokenUrl = "http://localhost/token")
        val gateway = QQGateway(
            QQClient(config, http), onEvent = { finished.complete(Unit) },
            onSelf = { assertEquals("bot", it.id) }, http = http
        )
        val job = CoroutineScope(currentCoroutineContext()).launch { gateway.run { registrations.incrementAndGet() } }
        try {
            withTimeout(10000) { heartbeatSeen.await(); finished.await() }
            assertEquals(1, registrations.get())
            assertEquals(2, connections.get())
        } finally {
            job.cancelAndJoin()
            http.close()
        }
    }
}
