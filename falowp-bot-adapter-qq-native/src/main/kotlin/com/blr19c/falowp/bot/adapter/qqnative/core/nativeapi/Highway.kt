package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.all
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.*
import java.nio.ByteBuffer
import java.nio.file.Files
import java.time.Duration

/**
 * 通过 QQ Highway 协议上传文件数据块
 */
internal class Highway(private val bridge: NativeBridge, private val uin: String) {
    private class Session(val ticket: ByteArray, val servers: List<String>)

    private var session: Session? = null
    private val mutex = Mutex()
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()
    private suspend fun session(): Session = mutex.withLock {
        session ?: run {
            val body = m(
                0x501,
                n(1, 0),
                n(2, 0),
                n(3, 16),
                n(4, 1),
                n(6, 3),
                *listOf(1, 5, 10, 21).map { n(7, it) }.toTypedArray(),
                n(9, 2),
                n(10, 9),
                n(11, 8),
                m(15, "1.0.1")
            )
            val reply = b(bridge.packet("HttpConn.0x6ff_501", body), 0x501)
            val servers = all(reply, 3).filter { u(it, 1) == 1uL }.flatMap { all(it, 2) }.mapNotNull {
                val raw = b(it, 2)
                if (raw.size == 4) "${raw.joinToString(".") { byte -> (byte.toInt() and 255).toString() }}:${
                    u(
                        it,
                        3
                    )
                }" else null
            }
            check(b(reply, 1).isNotEmpty() && servers.isNotEmpty()) { "QQ 未返回媒体上传会话" }
            Session(b(reply, 1), servers).also { session = it }
        }
    }

    suspend fun upload(
        file: FileHashes,
        command: Int,
        uuid: String,
        key: String,
        addresses: List<ByteArray>,
        info: List<ByteArray>
    ) {
        if (key.isEmpty()) return
        val network = addresses.map { m(1, m(1, n(1, 1), m(2, ip(u(it, 1)))), n(2, u(it, 2).toLong())) }
        val ext = cat(
            m(1, uuid),
            m(2, key),
            m(5, *network.toTypedArray()),
            *info.map { m(6, it) }.toTypedArray(),
            n(10, 1024 * 1024),
            m(11, *file.states.map { m(1, it) }.toTypedArray())
        )
        upload(file, command, ext)
    }

    // QQ Highway 返回的上传端点使用 HTTP
    @Suppress("HttpUrlsUsage")
    suspend fun upload(file: FileHashes, command: Int, ext: ByteArray) {
        val connection = session()
        withContext(Dispatchers.IO) {
            var offset = 0L
            Files.newInputStream(file.path).use { input ->
                while (true) {
                    val block = input.readNBytes(1024 * 1024)
                    if (block.isEmpty()) break
                    val head = cat(
                        m(
                            1,
                            n(1, 1),
                            m(2, uin),
                            m(3, "PicUp.DataUp"),
                            n(4, 0),
                            n(5, 0),
                            n(6, 1600001604),
                            n(7, 16),
                            n(8, command)
                        ),
                        m(
                            2,
                            n(1, 0),
                            n(2, file.size),
                            n(3, offset),
                            n(4, block.size),
                            m(6, connection.ticket),
                            m(8, digest("MD5", block)),
                            m(9, file.md5),
                            n(10, 0),
                            n(13, 0)
                        ), m(3, ext), n(4, 0), m(5, n(1, 8), n(3, 1600001604))
                    )
                    var failure: Exception? = null
                    for (server in connection.servers) {
                        try {
                            val request =
                                HttpRequest.newBuilder(URI("http://$server/cgi-bin/httpconn?htcmd=0x6FF0087&uin=$uin"))
                                    .timeout(Duration.ofSeconds(30)).header("Accept-Encoding", "identity")
                                    .POST(HttpRequest.BodyPublishers.ofByteArray(frame(head, block))).build()
                            val reply = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
                            reply.body().use { stream ->
                                check(reply.statusCode() == 200) { "QQ 上传 HTTP ${reply.statusCode()}" }
                                val data = stream.readNBytes(2 * 1024 * 1024 + 1)
                                check(data.size <= 2 * 1024 * 1024) { "QQ 上传回执过大" }
                                response(data)
                            }
                            failure = null
                            break
                        } catch (error: Exception) {
                            failure = error
                        }
                    }
                    failure?.let { throw it }
                    offset += block.size
                }
            }
            check(offset == file.size) { "上传期间媒体文件发生变化" }
        }
    }

    companion object {
        fun ip(value: ULong) = listOf(0, 8, 16, 24).joinToString(".") { ((value shr it) and 255u).toString() }
        fun frame(head: ByteArray, body: ByteArray) =
            cat(ByteBuffer.allocate(9).put(40).putInt(head.size).putInt(body.size).array(), head, body, byteArrayOf(41))

        fun response(data: ByteArray) {
            require(data.size >= 10 && data[0] == 40.toByte() && data.last() == 41.toByte()) { "QQ 媒体上传回执格式错误" }
            val header = ByteBuffer.wrap(data, 1, 8)
            val size = header.int
            val body = header.int
            require(size >= 0 && body >= 0 && 10L + size + body == data.size.toLong()) { "QQ 媒体上传回执不完整" }
            val head = data.copyOfRange(9, 9 + size)
            check(u(head, 3) == 0uL && u(b(head, 2), 5) == 0uL) {
                "QQ 媒体上传失败 (${u(head, 3)}/${
                    u(
                        b(head, 2),
                        5
                    )
                })"
            }
        }
    }
}
