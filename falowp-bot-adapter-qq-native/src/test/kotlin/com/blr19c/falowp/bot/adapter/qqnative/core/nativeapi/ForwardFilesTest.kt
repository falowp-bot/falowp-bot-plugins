package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MemoryMessageHistory
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.*

class ForwardFilesTest {
    private class Fixture {
        val account = j(obj("id" to "9", "uid" to "u_9", "nickname" to "测试"))
        val calls = mutableListOf<String>()
        val uploads = mutableListOf<Map<String, List<ByteArray>>>()
        var exists = true
        var rejected = false
        var confirm = true
        var port = 1
        val bridge = NativeBridge { _, args ->
            assertEquals("sendSsoCmdReqByContend", args["method"])
            val params = j(args).path("arguments")
            val command = params[0].asString()
            val request = params[1].binary()
            calls.add(command)
            val reply = when (command) {
                "OidbSvcTrpcTcp.0x6D6_0" -> {
                    val file = b(b(request, 4), 1)
                    assertEquals(42uL, u(file, 1))
                    assertEquals(102uL, u(file, 3))
                    assertEquals("test.bin", s(file, 6))
                    m(
                        4, m(
                            1, n(1, if (rejected) 7 else 0), m(4, "127.0.0.1"), m(7, "/group-file"),
                            m(8, byteArrayOf(1)), m(9, byteArrayOf(2)), n(10, if (exists) 1 else 0), n(14, port)
                        )
                    )
                }

                "OidbSvcTrpcTcp.0xE37_1700" -> {
                    assertEquals(0uL, u(request, 12))
                    val file = b(b(request, 4), 19)
                    assertEquals("u_9", s(file, 10))
                    assertEquals("u_11", s(file, 20))
                    assertEquals(16, b(file, 50).size)
                    assertEquals(20, b(file, 60).size)
                    m(
                        4, m(
                            19, n(10, if (rejected) 7 else 0), m(60, "127.0.0.1"), n(80, port), m(90, "private-file"),
                            n(110, if (exists) 1 else 0), m(200, "file-hash"), m(220, byteArrayOf(2))
                        )
                    )
                }

                "OidbSvcTrpcTcp.0xE37_800" -> {
                    val body = b(b(request, 4), 10)
                    assertEquals("private-file", s(body, 30))
                    assertEquals("file-hash", s(body, 40))
                    m(
                        4, if (confirm) m(
                            10, n(10, 0), m(
                                30, n(110, 1), n(3, 2), m(101, byteArrayOf(3)),
                                m(100, byteArrayOf(4)), n(130, 100)
                            )
                        ) else m(10, n(10, 7))
                    )
                }

                "HttpConn.0x6ff_501" -> m(
                    0x501, m(1, byteArrayOf(1)), m(
                        3, n(1, 1),
                        m(2, m(2, byteArrayOf(127, 0, 0, 1)), n(3, port))
                    )
                )

                "trpc.group.long_msg_interface.MsgService.SsoSendLongMsg" -> {
                    uploads.add(ForwardReader.sections(unpack(b(b(request, 2), 4), true)))
                    m(2, m(3, "resource-${uploads.size}"))
                }

                "OidbSvcTrpcTcp.0x6D6_2" -> m(4, m(3, n(1, 0), m(5, "download.qq.com"), m(6, byteArrayOf(1))))
                "OidbSvcTrpcTcp.0xE37_1200" -> m(
                    4,
                    m(
                        14,
                        n(10, 0),
                        m(30, m(20, "download.qq.com"), n(40, 8080), m(50, "https://placeholder/file?key=1"))
                    )
                )

                else -> error(command)
            }
            j(obj("result" to 0, "rspbuffer" to binary(reply)))
        }
        val files = ForwardFiles(bridge, account)
        val forward = ForwardUpload(
            bridge,
            account,
            NativeContacts(bridge),
            ForwardMedia(bridge, NativeMedia(bridge, j(obj())), "9"),
            OriginalMessages(NativeMedia(bridge, j(obj())), NativeContacts(bridge)),
            NativeReferences(bridge, MemoryMessageHistory(), "9")
        )
    }

    @Test
    fun groupFileKeepsItsOwnForwardRecordAndRoundTripsDownloadMetadata(): Unit = runBlocking(Dispatchers.IO) {
        val path = Files.createTempFile("qq-forward-", ".bin")
        try {
            Files.writeString(path, "file bytes")
            val f = Fixture()
            f.forward.create(
                Peer(2, "42"), listOf(
                    listOf(
                        QQNativeOutgoingPart.Text("before"),
                        QQNativeOutgoingPart.File(path, "test.bin"), QQNativeOutgoingPart.Text("after")
                    )
                )
            )
            val rows = f.uploads.single().getValue("MultiMsg")
            assertEquals(3, rows.size)
            val metadata = ForwardReader.elements(rows[1]).single().path("packetFile")
            assertEquals("/group-file", metadata.s("id"))
            assertEquals(10, metadata.l("size"))
            assertEquals("42", metadata.s("group"))
            assertEquals("https://download.qq.com/ftn_handler/01/?fname=", f.files.resolve(metadata).s("uri"))
            assertFalse(f.calls.contains("HttpConn.0x6ff_501"))
        } finally {
            Files.delete(path)
        }
    }

    @Suppress("HttpUrlsUsage") // 验证服务端返回的下载协议和端口
    @Test
    fun privateFilesKeepContentInBothSingleAndNestedForwardRecords(): Unit = runBlocking(Dispatchers.IO) {
        val path = Files.createTempFile("qq-forward-", ".bin")
        try {
            Files.writeString(path, "private bytes")
            for (nested in listOf(false, true)) {
                val f = Fixture()
                val file = listOf(QQNativeOutgoingPart.File(path, "test.bin"))
                f.forward.create(
                    Peer(1, "u_11"),
                    if (nested) listOf(file, listOf(QQNativeOutgoingPart.Text("other"))) else listOf(file)
                )
                val row = f.uploads.first().getValue("MultiMsg").single()
                val content = b(b(row, 3), 2)
                assertEquals("private-file", s(b(content, 1), 3))
                assertEquals("u_11", s(b(b(content, 6), 2), 16))
                val metadata = ForwardReader.elements(row).single().path("packetFile")
                assertEquals("file-hash", metadata.s("hash"))
                assertEquals("http://download.qq.com:8080/file?key=1&isthumb=0", f.files.resolve(metadata).s("uri"))
                assertEquals(if (nested) 3 else 1, f.uploads.size)
            }
        } finally {
            Files.delete(path)
        }
    }

    @Test
    fun fileUploadTransfersBlocksWithCorrectCommandsAndReportsServerFailure(): Unit = runBlocking(Dispatchers.IO) {
        val frames = CopyOnWriteArrayList<Pair<ByteArray, ByteArray>>()
        var fail = false
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/cgi-bin/httpconn") { exchange ->
            val data = exchange.requestBody.readBytes()
            val sizes = ByteBuffer.wrap(data, 1, 8)
            val header = sizes.int
            val length = sizes.int
            frames.add(data.copyOfRange(9, 9 + header) to data.copyOfRange(9 + header, 9 + header + length))
            val response = Highway.frame(n(3, if (fail) 7 else 0), byteArrayOf())
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        val path = Files.createTempFile("qq-upload-", ".bin")
        val bytes = ByteArray(1024 * 1024 + 7) { (it % 251).toByte() }
        try {
            Files.write(path, bytes)
            for (group in listOf(true, false)) {
                frames.clear()
                val f = Fixture().apply { exists = false; port = server.address.port }
                f.files.encode(
                    QQNativeOutgoingPart.File(path, "test.bin"),
                    if (group) Peer(2, "42") else Peer(1, "u_11")
                )
                assertEquals(2, frames.size)
                assertContentEquals(bytes, cat(*frames.map { it.second }.toTypedArray()))
                assertEquals(if (group) 71uL else 95uL, u(b(frames[0].first, 1), 8))
                assertEquals(1048576uL, u(b(frames[1].first, 2), 3))
                val ext = b(frames[0].first, 3)
                assertEquals(if (group) 0uL else 1uL, u(ext, 200))
                assertContentEquals(digest("MD5", bytes), b(b(b(ext, 100), 200), 200))
            }
            fail = true
            val f = Fixture().apply { exists = false; port = server.address.port }
            assertFailsWith<IllegalStateException> {
                f.forward.create(
                    Peer(2, "42"),
                    listOf(listOf(QQNativeOutgoingPart.File(path, "test.bin")))
                )
            }
            assertTrue(f.uploads.isEmpty())
        } finally {
            server.stop(0); Files.delete(path)
        }
    }

    @Test
    fun rejectedUploadAndMissingPrivateConfirmationNeverPublishAForward(): Unit = runBlocking(Dispatchers.IO) {
        val path = Files.createTempFile("qq-reject-", ".bin")
        try {
            Files.writeString(path, "bytes")
            val f = Fixture().apply { rejected = true }
            for (peer in listOf(Peer(2, "42"), Peer(1, "u_11"))) {
                assertFailsWith<IllegalStateException> {
                    f.forward.create(
                        peer,
                        listOf(listOf(QQNativeOutgoingPart.File(path, "test.bin")))
                    )
                }
            }
            f.rejected = false
            f.confirm = false
            assertFailsWith<IllegalStateException> {
                f.forward.create(
                    Peer(1, "u_11"),
                    listOf(listOf(QQNativeOutgoingPart.File(path, "test.bin")))
                )
            }
            assertTrue(f.uploads.isEmpty())
        } finally {
            Files.delete(path)
        }
    }
}
