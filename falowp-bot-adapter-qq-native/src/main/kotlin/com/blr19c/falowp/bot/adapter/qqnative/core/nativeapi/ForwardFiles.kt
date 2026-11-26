package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tools.jackson.databind.JsonNode
import java.net.URI
import java.nio.ByteBuffer
import java.nio.file.Files

/**
 * 为合并转发上传群文件或私聊离线文件
 */
internal class ForwardFiles(private val bridge: NativeBridge, private val account: JsonNode) {
    private val highway = Highway(bridge, account.s("id"))

    private suspend fun packet(command: Int, sub: Int, body: ByteArray): ByteArray {
        val result = bridge.packet(
            "OidbSvcTrpcTcp.0x${command.toString(16).uppercase()}_$sub",
            cat(n(1, command), n(2, sub), m(4, body), n(12, if (command == 0xe37) 0 else 1))
        )
        check(u(result, 3) == 0uL) { "文件请求失败：${s(result, 5)}" }
        return b(result, 4).also { check(it.isNotEmpty()) { "QQ 未返回文件回执" } }
    }

    private fun checked(body: ByteArray, code: Int, wording: Int): ByteArray {
        check(body.isNotEmpty() && u(body, code) == 0uL) { "文件操作失败：${s(body, wording)}" }
        return body
    }

    private suspend fun upload(
        file: FileHashes, name: String, group: Long, id: String,
        key: ByteArray, checkKey: ByteArray, host: String, port: Long
    ) {
        check(key.isNotEmpty() && checkKey.isNotEmpty() && host.isNotBlank() && port in 1..65535) { "QQ 文件上传凭据不完整" }
        val business = cat(
            n(100, account.s("id").toLong()),
            if (group != 0L) cat(n(200, group), n(400, group)) else byteArrayOf()
        )
        val extension = cat(
            n(1, 100), n(2, 1), n(3, 0), m(
                100,
                m(100, business),
                m(
                    200,
                    n(100, file.size),
                    m(200, file.md5),
                    m(300, checkKey),
                    m(400, file.md5),
                    m(600, id),
                    m(700, key)
                ),
                m(300, n(100, 3), m(200, "100"), n(300, 3), m(400, "1.1.1"), n(600, 4)),
                m(400, m(100, name)), m(500, m(200, m(1, n(1, 1), m(2, host)), n(2, port)))
            ),
            n(200, if (group != 0L) 0 else 1)
        )
        highway.upload(file, if (group != 0L) 71 else 95, extension)
    }

    /**
     * 按转发记录中的文件凭据换取下载地址
     */
    @Suppress("HttpUrlsUsage") // 私聊文件服务返回 HTTP 端口
    suspend fun resolve(file: JsonNode): JsonNode {
        val group = file.s("group")
        val url = if (group.isNotBlank() && group != "0") {
            val result = checked(
                b(
                    packet(
                        0x6d6, 2, m(
                            3, n(1, group.toLong()), n(2, 7),
                            n(3, file.l("busId")), m(4, file.s("id"))
                        )
                    ), 3
                ), 1, 3
            )
            val host = s(result, 5)
            val key = b(result, 6).hex()
            check(host.matches(Regex("[A-Za-z0-9.-]+")) && key.isNotEmpty()) { "QQ 未返回群文件下载地址" }
            "https://$host/ftn_handler/$key/?fname="
        } else {
            val result = checked(
                b(
                    packet(
                        0xe37, 1200, cat(
                            n(1, 1200),
                            n(2, 1),
                            m(
                                14,
                                m(10, account.s("uid")),
                                m(20, file.s("id")),
                                n(30, 2),
                                m(60, file.s("hash")),
                                n(601, 0)
                            ),
                            n(101, 3),
                            n(102, 103),
                            n(200, 1),
                            m(99999, byteArrayOf(0xc0.toByte(), 0x85.toByte(), 0x2c, 1))
                        )
                    ), 14
                ), 10, 20
            )
            val download = b(result, 30)
            val host = s(download, 20)
            val location = URI(s(download, 50))
            val path = location.rawPath.orEmpty() + (location.rawQuery?.let { "?$it" } ?: "")
            val port = u(download, 40).toLong()
            check(host.matches(Regex("[A-Za-z0-9.-]+")) && port in 1..65535 && path.startsWith("/")) { "QQ 未返回私聊文件下载地址" }
            "http://$host:$port$path${if (location.rawQuery == null) "?" else "&"}isthumb=0"
        }
        return j(
            obj(
                "kind" to "file",
                "uri" to url,
                "id" to file.s("id"),
                "name" to file.s("name"),
                "size" to file.l("size")
            )
        )
    }

    suspend fun encode(part: QQNativeOutgoingPart.File, peer: Peer): EncodedPart {
        require(part.name.isNotBlank()) { "文件名不能为空" }
        val file = withContext(Dispatchers.IO) { hashes(part.path) }
        val preview = "[文件]${part.name}"
        if (peer.chatType == 2) {
            val group = peer.peerUid.toLong()
            val result = checked(
                b(
                    packet(
                        0x6d6, 0, m(
                            1,
                            n(1, group), n(2, 4), n(3, 102), n(4, 6), m(5, part.folderId.ifBlank { "/" }),
                            m(6, part.name), m(7, "/${part.name}"), n(8, file.size), m(9, file.sha1),
                            m(10, byteArrayOf()), m(11, file.md5), n(15, 1)
                        )
                    ), 1
                ), 1, 2
            )
            val id = s(result, 7)
            check(id.isNotBlank()) { "QQ 未返回群文件标识" }
            if (u(result, 10) == 0uL) upload(
                file,
                part.name,
                group,
                id,
                b(result, 9),
                b(result, 8),
                s(result, 4),
                u(result, 14).toLong()
            )
            val detail = cat(
                n(1, 6), m(2, part.name), m(
                    7, m(
                        2, n(1, 102), m(2, id), n(3, file.size),
                        m(4, part.name), m(6, file.sha1), m(7, ""), m(8, file.md5)
                    )
                )
            )
            check(detail.size <= 65535) { "群文件消息元数据过大" }
            val tlv = cat(ByteBuffer.allocate(3).put(1).putShort(detail.size.toShort()).array(), detail)
            return EncodedPart(listOf(m(5, n(1, 24), m(2, tlv))), preview)
        }
        require(part.folderId.isBlank()) { "私聊文件不能指定群目录" }
        val firstMd5 = withContext(Dispatchers.IO) {
            Files.newInputStream(file.path).use { digest("MD5", it.readNBytes(10 * 1024 * 1024)) }
        }
        val sender = account.s("uid")
        val result = checked(
            b(
                packet(
                    0xe37, 1700, cat(
                        n(1, 1700), n(2, 0), m(
                            19,
                            m(10, sender), m(20, peer.peerUid), n(30, file.size), m(40, part.name), m(50, firstMd5),
                            m(60, file.sha1), m(70, "/"), m(110, file.md5), m(120, byteArrayOf())
                        ),
                        n(101, 3), n(102, 1), n(200, 1)
                    )
                ), 19
            ), 10, 20
        )
        val id = s(result, 90)
        val hash = s(result, 200)
        check(id.isNotBlank() && hash.isNotBlank()) { "QQ 未返回私聊文件标识" }
        if (u(result, 110) == 0uL) upload(
            file,
            part.name,
            0,
            id,
            b(result, 220),
            file.sha1,
            s(result, 60),
            u(result, 80).toLong()
        )
        val confirm = checked(
            b(
                packet(
                    0xe37, 800, cat(
                        n(1, 800), n(2, 0), m(
                            10,
                            m(10, sender), m(20, peer.peerUid), m(30, id), m(40, hash)
                        ), n(101, 3), n(102, 1), n(200, 1)
                    )
                ), 10
            ), 10, 20
        )
        val metadata = b(confirm, 30)
        check(metadata.isNotEmpty()) { "QQ 未确认私聊文件元数据" }
        val content = cat(
            m(
                1, n(1, 0), m(3, id), m(4, file.md5), m(5, part.name), n(6, file.size),
                n(9, 1), n(50, 0), n(55, System.currentTimeMillis() / 1000 + 7 * 24 * 60 * 60), m(57, hash)
            ),
            m(
                6, m(
                    2, n(1, u(metadata, 110).toLong()), m(4, id), m(5, part.name), n(6, u(metadata, 3).toLong()),
                    m(7, b(metadata, 101)), m(8, b(metadata, 100)), n(9, u(metadata, 130).toLong()),
                    m(14, hash), m(15, sender), m(16, peer.peerUid)
                )
            )
        )
        return EncodedPart(emptyList(), preview, content)
    }
}
