package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import tools.jackson.databind.JsonNode
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class MediaAndEventsTest {
    @Test
    fun downloadHandlesEarlyCallbacksRetriesAndConcurrentReaders(): Unit = runBlocking(Dispatchers.IO) {
        val dir = Files.createTempDirectory("qq-media-")
        var attempts = 0
        lateinit var media: NativeMedia
        val bridge = NativeBridge { _, args ->
            assertEquals("downloadRichMedia", args["method"])
            attempts++
            val request = j(args).path("arguments")[0]
            if (attempts == 1) j(obj("result" to 1)) else {
                media.complete(j(obj("msgId" to "m", "msgElementId" to "other", "fileErrCode" to 1)))
                withContext(Dispatchers.IO) { Files.writeString(Path.of(request.s("filePath")), "complete") }
                media.complete(
                    j(
                        obj(
                            "msgId" to "m",
                            "msgElementId" to "e",
                            "fileErrCode" to "0",
                            "filePath" to request.s("filePath")
                        )
                    )
                )
                j(obj("result" to 0))
            }
        }
        media = NativeMedia(bridge, j(obj("accountPath" to dir.toString())))
        val message = j(obj("chatType" to 2, "peerUid" to "g", "msgId" to "m"))
        val element = j(obj("elementId" to "e"))
        try {
            assertFailsWith<IllegalStateException> { media.read(message, element, j(obj())) }
            val result = awaitAll(
                async { media.read(message, element, j(obj())) },
                async { media.read(message, element, j(obj())) })
            assertEquals(result[0], result[1])
            assertEquals("complete", Files.readString(Path.of(URI(result[0]))))
            assertEquals(2, attempts)
            assertEquals(result[0], media.read(message, element, j(obj())))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun mediaOutsideAccountDirectoryIsNeverPublishedAsIncomingContent(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("qq-root-")
        val outside = Files.createTempFile("qq-outside-", ".bin")
        Files.writeString(outside, "private")
        val bridge = NativeBridge { _, _ -> j(obj("result" to 1)) }
        val media = NativeMedia(bridge, j(obj("accountPath" to root.toString())))
        try {
            assertFailsWith<IllegalStateException> {
                media.read(
                    j(obj("msgId" to "m")),
                    j(obj("elementId" to "e")),
                    j(obj("filePath" to outside.toString()))
                )
            }
        } finally {
            root.toFile().deleteRecursively()
            Files.delete(outside)
        }
    }

    @Test
    fun roleCacheIsGroupScopedAndUpdatesOnDemotion(): Unit = runBlocking(Dispatchers.IO) {
        lateinit var contacts: NativeContacts
        var calls = 0
        contacts = NativeContacts(NativeBridge { _, args ->
            calls++
            val p = j(args).path("arguments")
            contacts.memberUpdate(p[0].asString(), j(nativeMap(p[1][0].asString() to obj("role" to 3))))
            j(obj("result" to 0))
        })
        assertEquals(listOf(3, 3), awaitAll(async { contacts.role("10", "u_A") }, async { contacts.role("10", "u_A") }))
        assertEquals(1, calls)
        contacts.memberUpdate("20", j(nativeMap("u_A" to obj("role" to 4))))
        contacts.memberUpdate("10", j(nativeMap("u_A" to obj("role" to 2))))
        assertEquals(2, contacts.role("10", "u_A"))
        assertEquals(4, contacts.role("20", "u_A"))
    }

    @Test
    fun noticesPreserveApplicantsInvitersNudgeAndWithdrawIdentities(): Unit = runBlocking(Dispatchers.IO) {
        val bridge = NativeBridge { _, args ->
            val p = j(args).path("arguments")
            j(obj("uins" to nativeMap(p[0][0].asString() to p[0][0].asString().removePrefix("u_"))))
        }
        val contacts = NativeContacts(bridge)
        val account = j(obj("id" to "9", "uid" to "u_9"))
        val media = NativeMedia(bridge, j(obj()))
        val messages = NativeMessages(bridge, account, contacts, media, NativeDelivery(bridge, account))
        val result = mutableListOf<Pair<String, JsonNode>>()
        val events = NativeEvents(
            account, contacts, messages,
            onMessage = { result.add("message" to it) }, onNotice = { result.add("notice" to it) })
        val request = obj(
            "status" to 1,
            "group" to obj("groupCode" to "55"),
            "user1" to obj("uid" to "u_100"),
            "user2" to obj("uid" to "u_200"),
            "postscript" to "hello"
        )
        events.request("groupRequest", j(request + obj("type" to 7, "seq" to "1")))
        events.request("groupRequest", j(request + obj("type" to 1, "seq" to "2")))
        events.request("groupRequest", j(request + obj("type" to 13, "seq" to "3")))
        assertEquals(listOf("100", "200"), result.map { it.second.s("actor") })
        val base = obj("chatType" to 1, "peerUid" to "u_200", "peerUin" to "0", "senderUin" to "100", "msgId" to "msg")
        events.receive(j(base + obj("elements" to listOf(obj("textElement" to obj("content" to "原消息"))))), false)
        events.receive(
            j(base + obj("elements" to listOf(obj("grayTipElement" to obj("revokeElement" to obj("operatorUid" to "u_100")))))),
            true
        )
        val withdraw = result.last().second
        assertEquals("withdraw", withdraw.s("kind"))
        assertEquals("100", withdraw.s("actor"))
        assertEquals("原消息", withdraw.path("original").path("parts")[0].s("value"))
        events.receive(
            j(
                base + obj(
                    "msgId" to "poke",
                    "elements" to listOf(
                        obj(
                            "grayTipElement" to obj(
                                "jsonGrayTipElement" to obj(
                                    "busiId" to "5143",
                                    "xmlToJsonParam" to obj(
                                        "templParam" to nativeMap(
                                            "uin_str1" to "u_100",
                                            "uin_str2" to "u_200"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ), false
        )
        val poke = result.last().second
        assertEquals("nudge", poke.s("kind"))
        assertEquals("100", poke.s("actor"))
        assertEquals("200", poke.s("member"))
        assertEquals("200", poke.s("target"))
    }
}
