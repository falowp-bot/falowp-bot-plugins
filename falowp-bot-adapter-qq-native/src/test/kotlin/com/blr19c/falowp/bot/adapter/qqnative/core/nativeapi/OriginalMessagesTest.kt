package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class OriginalMessagesTest {
    @Test
    fun originalMediaUsesSourceConversationAndDownloadContextBeforeResending(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("qq-original-")
        lateinit var media: NativeMedia
        val bridge = NativeBridge { _, args ->
            assertEquals("downloadRichMedia", args["method"])
            val request = j(args).path("arguments")[0]
            assertEquals("source-message", request.s("msgId"))
            assertEquals("84", request.s("peerUid"))
            assertEquals("file-element", request.s("elementId"))
            withContext(Dispatchers.IO) { Files.writeString(Path.of(request.s("filePath")), "original bytes") }
            media.complete(
                j(
                    obj(
                        "msgId" to "source-message", "msgElementId" to "file-element", "fileErrCode" to 0,
                        "filePath" to request.s("filePath")
                    )
                )
            )
            j(obj("result" to 0))
        }
        media = NativeMedia(bridge, j(obj("accountPath" to root.toString())))
        val original = j(
            obj(
                "chatType" to 2, "peerUid" to "84", "msgId" to "source-message", "elements" to listOf(
                    obj(
                        "elementType" to 3,
                        "elementId" to "file-element",
                        "fileElement" to obj("fileUuid" to "old-id", "fileName" to "test.txt")
                    )
                )
            )
        )
        try {
            val expanded =
                OriginalMessages(media, NativeContacts(bridge)).expand(QQNativeOutgoingPart.Original("9", original))
            val file = expanded.single() as QQNativeOutgoingPart.File
            assertEquals("original bytes", Files.readString(file.path))
            assertEquals("test.txt", file.name)
            assertEquals("file-element", original.list("elements").single().s("elementId"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun originalMixedMessagesPreserveMentionsGameResultsAndReplyReferences(): Unit = runBlocking {
        val bridge = NativeBridge { _, _ -> error("No network expected") }
        val originals = OriginalMessages(NativeMedia(bridge, j(obj())), NativeContacts(bridge))
        val source = j(
            obj(
                "elements" to listOf(
                    obj("textElement" to obj("content" to "hello")),
                    obj("textElement" to obj("atType" to 2, "atUid" to "11")),
                    obj("faceElement" to obj("faceIndex" to 358, "resultId" to "6")),
                    obj("replyElement" to obj("replayMsgSeq" to "123", "senderUin" to "11")),
                    obj(
                        "multiForwardMsgElement" to obj(
                            "resId" to "nested",
                            "fileName" to "nested-file",
                            "msgCount" to 2
                        )
                    )
                )
            )
        )
        val parts = originals.expand(QQNativeOutgoingPart.Original("9", source))
        assertEquals(QQNativeOutgoingPart.Mention("11"), parts[1])
        assertEquals(QQNativeOutgoingPart.Game(true, 6), parts[2])
        assertEquals("123", (parts[3] as QQNativeOutgoingPart.Reply).data.s("replayMsgSeq"))
        assertEquals(
            "nested",
            mapper.readTree((parts[4] as QQNativeOutgoingPart.Ark).json).path("meta").path("detail").s("resid")
        )
        assertFailsWith<IllegalStateException> {
            originals.expand(
                QQNativeOutgoingPart.Original(
                    "9",
                    j(obj("elements" to listOf(obj("elementType" to 8))))
                )
            )
        }
    }
}
