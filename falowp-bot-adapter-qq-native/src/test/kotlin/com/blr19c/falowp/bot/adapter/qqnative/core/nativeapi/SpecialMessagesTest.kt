package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.*
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.*
import com.blr19c.falowp.bot.adapter.qqnative.adapter.message.OutboundMedia
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import com.blr19c.falowp.bot.system.api.SendMessage
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class SpecialMessagesTest {
    @Test
    fun buildersValidateParametersAndNativeAndForwardKeepGameResults() {
        assertFailsWith<IllegalArgumentException> { SendMessage.builder().diceMessage(7) }
        assertFailsWith<IllegalArgumentException> { SendMessage.builder().rpsMessage(0) }
        assertFailsWith<IllegalArgumentException> { SendMessage.builder().marketFaceMessage("bad", 1, "key") }
        for (dice in listOf(true, false)) {
            val game = QQNativeOutgoingPart.Game(dice, 3)
            val native = NativeSpecialMessages.element(game).path("faceElement")
            assertEquals(if (dice) 358 else 359, native.i("faceIndex"))
            assertEquals("3", native.s("resultId"))
            val forward = NativeSpecialMessages.forward(game)
            assertEquals(2, forward.elements.size)
            assertEquals("3", s(b(b(forward.elements[0], 53), 2), 6))
        }
        assertFalse(
            NativeSpecialMessages.element(QQNativeOutgoingPart.Game(true, null)).path("faceElement").has("resultId")
        )
        val face = QQNativeOutgoingPart.MarketFace("0123456789abcdef0123456789abcdef", 123, "key", "表情")
        assertEquals(123, NativeSpecialMessages.element(face).path("marketFaceElement").i("emojiPackageId"))
        val encoded = b(NativeSpecialMessages.forward(face).elements.single(), 6)
        assertEquals(123uL, u(encoded, 5))
        assertEquals(face.id, b(encoded, 4).hex())
        assertEquals("key", s(encoded, 7))
    }

    @Test
    fun remoteFilesAreDownloadedAndDeletedAfterNativeCopy(): Unit = runBlocking(Dispatchers.IO) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val body = "文件内容".toByteArray()
        server.createContext("/file") { exchange ->
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        val native = Files.createTempFile("qq-file-copy-", ".dat")
        var temporary: Path? = null
        try {
            OutboundMedia().use { outbound ->
                val part = outbound.encode(
                    QQNativeFileMessage(
                        URI("http://127.0.0.1:${server.address.port}/file"),
                        "测试.txt",
                        "/folder"
                    )
                ) as QQNativeOutgoingPart.File
                temporary = part.path
                assertContentEquals(body, Files.readAllBytes(part.path))
                val bridge = NativeBridge { _, args ->
                    assertEquals("getRichMediaFilePathForGuild", args["method"])
                    assertEquals(3, j(args).path("arguments")[0].i("elementType"))
                    j(native.toString())
                }
                val element = NativeMedia(bridge, j(obj())).element(part).path("fileElement")
                assertEquals("测试.txt", element.s("fileName"))
                assertEquals("/folder", element.s("folderId"))
                assertContentEquals(body, Files.readAllBytes(native))
            }
            assertFalse(Files.exists(temporary))
            assertContentEquals(body, Files.readAllBytes(native))
        } finally {
            Files.deleteIfExists(native)
            server.stop(0)
        }
    }

    @Test
    fun musicSigningRequiresConfigurationAndSendsOnlySongMetadata(): Unit = runBlocking(Dispatchers.IO) {
        assertFailsWith<IllegalArgumentException> { NativeMusic.card(j(obj("type" to "qq", "id" to "1"))) }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var valid = true
        server.createContext("/") { exchange ->
            assertNull(exchange.requestHeaders.getFirst("Cookie"))
            val request = mapper.readTree(exchange.requestBody)
            assertEquals(setOf("type", "id"), request.properties().map { it.key }.toSet())
            val card =
                if (valid) "{\"app\":\"com.tencent.structmsg\",\"meta\":{\"music\":{}}}" else "{\"error\":\"failed\"}"
            val body = mapper.writeValueAsBytes(card)
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val args = j(obj("type" to "163", "id" to "1", "sign_url" to "http://127.0.0.1:${server.address.port}/"))
            assertEquals("com.tencent.structmsg", NativeMusic.card(args).s("app"))
            valid = false
            assertFailsWith<IllegalStateException> { NativeMusic.card(args) }
        } finally {
            server.stop(0)
        }
    }
}
