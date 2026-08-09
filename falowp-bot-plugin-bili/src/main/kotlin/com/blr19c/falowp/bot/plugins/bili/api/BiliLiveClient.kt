package com.blr19c.falowp.bot.plugins.bili.api

import com.blr19c.falowp.bot.plugins.bili.api.data.BiliFansMedal
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliLiveMessageData
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliLiveMessageType
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliLiveStreamMessage
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.InflaterInputStream
import kotlin.collections.ArrayDeque
import kotlin.time.Duration.Companion.milliseconds

/**
 * B 站直播信息流客户端
 */
@Suppress("SpellCheckingInspection")
class BiliLiveClient internal constructor(
    private val client: HttpClient,
    private val roomId: Long,
    private val uid: Long,
    private val token: String,
    private val host: String,
    private val wssPort: Int,
) {

    private fun parseMessage(raw: tools.jackson.databind.JsonNode): BiliLiveStreamMessage {
        val command = raw.path("cmd").safeString()
        val type = BiliLiveMessageType.from(command)
        val data = raw.path("data")
        val messageData = when (type) {
            BiliLiveMessageType.DANMU -> raw.path("info").let { info ->
                val medal = info.path(3)
                BiliLiveMessageData.Danmu(
                    info.path(2).path(0).asLong(), info.path(2).path(1).safeString(), info.path(1).safeString(),
                    medal(medal.path(0).asInt(), medal.path(1).safeString(), medal.path(12).asLong(), 0),
                )
            }

            BiliLiveMessageType.GIFT -> BiliLiveMessageData.Gift(
                data.path("uid").asLong(), data.path("uname").safeString(), data.path("giftName").safeString(),
                data.path("num").asLong(data.path("batch_combo_num").asLong()), data.path("price").asLong(),
                data.path("total_coin").asLong(), data.path("coin_type").safeString(),
                medal(data.path("medal_info")),
            )

            BiliLiveMessageType.ENTER -> parseEnter(data)

            BiliLiveMessageType.GUARD -> BiliLiveMessageData.Guard(
                data.path("uid").asLong(),
                data.path("username").safeString().ifBlank { data.path("uname").safeString() },
                data.path("guard_level").asInt(), data.path("num").asInt(), data.path("price").asLong(),
            )

            BiliLiveMessageType.SUPER_CHAT -> BiliLiveMessageData.SuperChat(
                data.path("uid").asLong(), data.path("user_info").path("uname").safeString(),
                data.path("message").safeString(), data.path("price").asInt(), medal(data.path("medal_info")),
            )

            else -> null
        }
        return BiliLiveStreamMessage(type, command, messageData, raw)
    }

    private fun parseEnter(data: tools.jackson.databind.JsonNode): BiliLiveMessageData.Enter? {
        val userName = data.path("uname").safeString()
        if (userName.isNotBlank()) return BiliLiveMessageData.Enter(
            data.path("uid").asLong(), userName, data.path("msg_type").asInt(1), medal(data.path("fans_medal")),
        )

        val pb = data.path("pb").safeString()
        if (pb.isBlank()) return null
        var userId = 0L
        var protobufUserName = ""
        var msgType = 0
        var fansMedal: BiliFansMedal? = null
        val reader = ProtobufReader(Base64.getDecoder().decode(pb))
        while (true) {
            val field = reader.next() ?: break
            when (field.number) {
                1 -> userId = field.varInt ?: userId
                2 -> protobufUserName = field.bytes?.decodeToString() ?: protobufUserName
                5 -> msgType = field.varInt?.toInt() ?: msgType
                9 -> fansMedal = field.bytes?.let(::protobufMedal)
            }
        }
        return protobufUserName.takeIf(String::isNotBlank)
            ?.let { BiliLiveMessageData.Enter(userId, it, msgType, fansMedal) }
    }

    private fun medal(node: tools.jackson.databind.JsonNode): BiliFansMedal? = medal(
        node.path("medal_level").asInt(node.path("level").asInt()),
        node.path("medal_name").safeString().ifBlank { node.path("name").safeString() },
        node.path("target_id").asLong(node.path("ruid").asLong()),
        node.path("anchor_roomid").asLong(node.path("roomid").asLong()),
    )

    private fun medal(level: Int, name: String, anchorId: Long, anchorRoomId: Long): BiliFansMedal? =
        if (level == 0 && name.isBlank() && anchorId == 0L && anchorRoomId == 0L) null
        else BiliFansMedal(level, name, anchorId, anchorRoomId)

    private fun protobufMedal(bytes: ByteArray): BiliFansMedal? {
        var level = 0
        var name = ""
        var anchorId = 0L
        var anchorRoomId = 0L
        val reader = ProtobufReader(bytes)
        while (true) {
            val field = reader.next() ?: break
            when (field.number) {
                1 -> anchorId = field.varInt ?: anchorId
                2 -> level = field.varInt?.toInt() ?: level
                3 -> name = field.bytes?.decodeToString() ?: name
                12 -> anchorRoomId = field.varInt ?: anchorRoomId
            }
        }
        return medal(level, name, anchorId, anchorRoomId)
    }

    private data class ProtobufField(val number: Int, val varInt: Long? = null, val bytes: ByteArray? = null) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ProtobufField

            if (number != other.number) return false
            if (varInt != other.varInt) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = number
            result = 31 * result + varInt.hashCode()
            result = 31 * result + (bytes?.contentHashCode() ?: 0)
            return result
        }
    }

    /** 只实现直播消息当前使用到的 protobuf wire types。 */
    private class ProtobufReader(private val bytes: ByteArray) {
        private var index = 0

        fun next(): ProtobufField? {
            if (index >= bytes.size) return null
            val tag = varint().toInt()
            val number = tag ushr 3
            return when (val wireType = tag and 7) {
                0 -> ProtobufField(number, varInt = varint())
                1 -> skip(8).let { ProtobufField(number) }
                2 -> {
                    val length = varint().toInt()
                    require(length >= 0 && index + length <= bytes.size) { "无效的直播 protobuf" }
                    ProtobufField(number, bytes = bytes.copyOfRange(index, index + length)).also { index += length }
                }

                5 -> skip(4).let { ProtobufField(number) }
                else -> throw IllegalArgumentException("不支持的 protobuf wire type: $wireType")
            }
        }

        private fun varint(): Long {
            var value = 0L
            var shift = 0
            while (index < bytes.size && shift < 64) {
                val current = bytes[index++].toInt() and 0xff
                value = value or ((current and 0x7f).toLong() shl shift)
                if (current and 0x80 == 0) return value
                shift += 7
            }
            throw IllegalArgumentException("无效的直播 protobuf")
        }

        private fun skip(length: Int) {
            require(index + length <= bytes.size) { "无效的直播 protobuf" }
            index += length
        }
    }

    /**
     * 连接信息流并持续接收消息
     *
     * 取消当前协程即可断开连接
     */
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
                                    val message = parseMessage(Json.readJsonNode(body))
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
