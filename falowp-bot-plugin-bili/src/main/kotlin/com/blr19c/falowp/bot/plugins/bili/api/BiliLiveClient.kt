package com.blr19c.falowp.bot.plugins.bili.api

import com.blr19c.falowp.bot.plugins.bili.api.data.BiliLiveStreamMessage
import com.blr19c.falowp.bot.system.json.Json
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.InflaterInputStream
import kotlin.time.Duration.Companion.milliseconds

/**
 * B 站直播信息流客户端
 */
class BiliLiveClient internal constructor(
    private val client: HttpClient,
    private val roomId: Long,
    private val uid: Long,
    private val token: String,
    private val host: String,
    private val wssPort: Int,
) {

    /**
     * 连接信息流并持续接收消息
     *
     * 取消当前协程即可断开连接
     */
    @Suppress("SpellCheckingInspection")
    suspend fun connect(onMessage: suspend (BiliLiveStreamMessage) -> Unit) {
        client.webSocket(urlString = "wss://$host:$wssPort/sub") {
            val authBody = Json.toJsonString(
                mapOf(
                    "uid" to uid,
                    "roomid" to roomId,
                    "protover" to 2,
                    "platform" to "web",
                    "type" to 2,
                    "key" to token,
                )
            ).encodeToByteArray()
            send(
                Frame.Binary(
                    true, ByteBuffer.allocate(16 + authBody.size)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putInt(16 + authBody.size)
                        .putShort(16)
                        .putShort(1)
                        .putInt(7)
                        .putInt(1)
                        .put(authBody)
                        .array()
                )
            )

            val heartbeat = launch {
                while (true) {
                    delay(30_000.milliseconds)
                    send(
                        Frame.Binary(
                            true, ByteBuffer.allocate(16)
                                .order(ByteOrder.BIG_ENDIAN)
                                .putInt(16)
                                .putShort(16)
                                .putShort(1)
                                .putInt(2)
                                .putInt(1)
                                .array()
                        )
                    )
                }
            }
            try {
                for (frame in incoming) {
                    if (frame !is Frame.Binary) continue
                    val pending = ArrayDeque<ByteArray>()
                    pending.add(frame.readBytes())
                    while (pending.isNotEmpty()) {
                        val bytes = pending.removeFirst()
                        var offset = 0
                        while (offset + 16 <= bytes.size) {
                            val header = ByteBuffer.wrap(bytes, offset, 16).order(ByteOrder.BIG_ENDIAN)
                            val packetSize = header.int
                            val headerSize = header.short.toInt() and 0xffff
                            val version = header.short.toInt() and 0xffff
                            val operation = header.int
                            header.int // sequence
                            require(headerSize in 16..packetSize && offset + packetSize <= bytes.size) {
                                "无效的直播弹幕数据包"
                            }
                            val body = bytes.copyOfRange(offset + headerSize, offset + packetSize)
                            if (version == 2) {
                                if (offset + packetSize < bytes.size) {
                                    pending.addFirst(bytes.copyOfRange(offset + packetSize, bytes.size))
                                }
                                pending.addFirst(InflaterInputStream(ByteArrayInputStream(body)).use { it.readAllBytes() })
                                break
                            }
                            when (operation) {
                                8 -> {
                                    val reply = Json.readJsonNode(body)
                                    check(reply.path("code").asInt() == 0) {
                                        "直播弹幕流认证失败: ${reply.path("code").asInt()}"
                                    }
                                }

                                5 -> {
                                    val message = BiliLiveStreamMessage.from(Json.readJsonNode(body))
                                    onMessage(message)
                                    if (message.command == "PREPARING") {
                                        close(CloseReason(CloseReason.Codes.NORMAL, "直播已结束"))
                                        return@webSocket
                                    }
                                }
                            }
                            offset += packetSize
                        }
                    }
                }
            } finally {
                heartbeat.cancel()
            }
        }
    }
}
