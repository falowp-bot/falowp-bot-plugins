package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.RecordingAccount
import com.blr19c.falowp.bot.adapter.qqnative.core.history.*
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeReceivedPart
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import java.awt.image.BufferedImage
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.*
import com.blr19c.falowp.bot.adapter.qqnative.adapter.message.NativeMessages as SystemMessages

class VideoThumbnailTest {
    private val noCalls = NativeBridge { _, _ -> error("Unexpected native call") }
    private fun png(path: Path) {
        ImageIO.write(BufferedImage(16, 8, BufferedImage.TYPE_INT_RGB), "png", path.toFile())
    }

    private fun config(root: Path) = j(
        obj(
            "accountPath" to root.toString(),
            "installer" to root.resolve("install.sh").toString(),
            "tools" to root.resolve("tools").toString()
        )
    )

    private fun extractor(root: Path, valid: Boolean = true) {
        Files.createDirectories(root.resolve("tools"))
        png(root.resolve("tools/frame.png"))
        Files.writeString(root.resolve("install.sh"), "exit 0\n")
        val script = root.resolve("tools/ffmpeg")
        val body = if (valid) $$"cp \"$(dirname \"$0\")/frame.png\" \"$output\"" else $$"printf broken > \"$output\""
        Files.writeString(script, "#!/bin/sh\nfor output do :; done\n$body\nprintf x >> \"$(dirname \"$0\")/runs\"\n")
        check(script.toFile().setExecutable(true))
    }

    @Test
    fun nativePreviewSurvivesDecodeStorageReopenAndSystemMapping(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("video-preview-").toRealPath()
        try {
            val video = Files.writeString(root.resolve("video.mp4"), "video")
            val cover = root.resolve("cover.png")
            png(cover)
            val account = j(obj("id" to "9", "uid" to "u_9"))
            val messages = NativeMessages(
                noCalls,
                account,
                NativeContacts(noCalls),
                NativeMedia(noCalls, config(root)),
                NativeDelivery(noCalls, account)
            )
            val row = j(
                obj(
                    "chatType" to 1,
                    "peerUid" to "u_11",
                    "peerUin" to "11",
                    "senderUin" to "11",
                    "senderUid" to "u_11",
                    "msgId" to "1",
                    "elements" to listOf(
                        obj(
                            "elementId" to "e",
                            "videoElement" to obj(
                                "filePath" to video.toString(),
                                "fileSize" to 5,
                                "thumbPath" to nativeMap(0 to cover.toString())
                            )
                        )
                    )
                )
            )
            val decoded = messages.decode(row)
            val settings = HistorySettings(backend = "sqlite")
            val key = HistoryKey("9", 1, "u_11", "1")
            HistoryStores.open(settings, root).put(key, j(obj("raw" to row, "decoded" to decoded)))
            val restored = checkNotNull(HistoryStores.open(settings, root).get(key)).path("decoded").message()
            assertEquals(
                cover.toUri(),
                restored.parts.filterIsInstance<QQNativeReceivedPart.Attachment>().single().thumbnail
            )
            val qq = RecordingAccount("9", "self").account
            val system = SystemMessages.decode(restored, qq)
            assertEquals(cover.toUri().toString(), system.content.video?.thumbnail?.info)
            assertEquals(video.toUri(), system.content.video?.src)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun missingInvalidAndOutsidePreviewsExtractOnceAndReuseCachedFrame(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("video-frame-").toRealPath()
        val outside = Files.createTempFile("outside-cover-", ".png")
        try {
            extractor(root)
            png(outside)
            val video = Files.writeString(root.resolve("video.mp4"), "video")
            val broken = Files.writeString(root.resolve("broken.png"), "invalid image")
            val link = Files.createSymbolicLink(root.resolve("outside-link.png"), outside)
            val data = j(
                obj(
                    "thumbPath" to nativeMap(
                        0 to broken.toString(),
                        1 to link.toString(),
                        2 to root.resolve("missing.png").toString()
                    )
                )
            )
            val media = NativeMedia(noCalls, config(root))
            val previews = awaitAll(
                async { media.videoThumbnail(data, video.toUri().toString()) },
                async { media.videoThumbnail(data, video.toUri().toString()) })
            assertEquals(previews[0], previews[1])
            val path = Path.of(URI(assertNotNull(previews[0])))
            assertTrue(path.startsWith(root))
            assertNotNull(ImageIO.read(path.toFile()))
            assertEquals("x", Files.readString(root.resolve("tools/runs")))
            assertEquals(previews[0], media.videoThumbnail(j(obj()), video.toUri().toString()))
            assertNull(media.videoThumbnail(j(obj()), outside.toUri().toString()))
        } finally {
            root.toFile().deleteRecursively()
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun failedFrameExtractionLeavesVideoReadableAndCleansTemporaryOutput(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("video-bad-frame-").toRealPath()
        try {
            extractor(root, false)
            val video = Files.writeString(root.resolve("video.mp4"), "video")
            val account = j(obj("id" to "9", "uid" to "u_9"))
            val messages = NativeMessages(
                noCalls,
                account,
                NativeContacts(noCalls),
                NativeMedia(noCalls, config(root)),
                NativeDelivery(noCalls, account)
            )
            val decoded = messages.decode(
                j(
                    obj(
                        "chatType" to 1,
                        "peerUid" to "u_11",
                        "peerUin" to "11",
                        "senderUin" to "11",
                        "senderUid" to "u_11",
                        "msgId" to "1",
                        "elements" to listOf(
                            obj(
                                "elementId" to "e",
                                "videoElement" to obj("filePath" to video.toString())
                            )
                        )
                    )
                )
            )
            val part = decoded.path("parts")[0]
            assertEquals("video", part.s("kind"))
            assertEquals(video.toUri().toString(), part.s("uri"))
            assertTrue(part.path("thumbnail").isNull)
            assertEquals(0, Files.list(root.resolve("falowp-thumbnails")).use { it.count() })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun forwardedVideoResolvesCoverResourceAndSurvivesMissingOrFailedCover(): Unit = runBlocking(Dispatchers.IO) {
        var rejectCover = false
        val requests = mutableListOf<String>()
        val bridge = NativeBridge { _, args ->
            val params = j(args).path("arguments")
            val body = Wire.b(params[1].binary(), 4)
            val index = Wire.b(Wire.b(body, 3), 1)
            val id = Wire.s(index, 2)
            requests.add(id)
            if (id == "cover" && rejectCover) j(obj("result" to 5)) else
                j(
                    obj(
                        "result" to 0,
                        "rspbuffer" to binary(
                            Wire.m(
                                4,
                                Wire.m(
                                    3,
                                    Wire.m(1, "?ticket=x"),
                                    Wire.m(3, Wire.m(1, "media.example.com"), Wire.m(2, "/$id"))
                                )
                            )
                        )
                    )
                )
        }
        val media = ForwardMedia(bridge, NativeMedia(bridge, j(obj())), "9")
        fun entry(id: String, sub: Int) =
            Wire.m(1, Wire.m(1, Wire.m(1, Wire.n(1, 123), Wire.m(4, "$id.mp4")), Wire.m(2, id)), Wire.n(2, sub))

        val main = entry("video", 0)
        val cover = entry("cover", 100)
        val result = media.resolve(Peer(2, "42"), "video", cat(main, cover))
        assertEquals(listOf("video", "cover"), requests)
        assertEquals("https://media.example.com/cover?ticket=x", result.s("thumbnail"))
        rejectCover = true
        val failure = media.resolve(Peer(2, "42"), "video", cat(main, cover))
        assertTrue(failure.path("thumbnail").isNull)
        assertEquals(result.s("uri"), failure.s("uri"))
        assertTrue(media.resolve(Peer(2, "42"), "video", main).path("thumbnail").isNull)
    }
}
