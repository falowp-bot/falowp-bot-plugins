package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * 跟踪原生发送回调，将发送请求与消息回执关联
 */
internal class NativeDelivery(private val bridge: NativeBridge, private val account: JsonNode) {
    private data class Pending(val peer: String, val result: CompletableDeferred<JsonNode>)

    private val pending = ConcurrentHashMap<String, Pending>()
    private val forwards = ConcurrentHashMap<String, Channel<JsonNode>>()
    private val locks = Array(64) { Mutex() }
    private val reader = ForwardReader(bridge, account.s("uid"))
    fun update(rows: List<JsonNode>) {
        for (row in rows) {
            val entry = pending[row.s("guildId")]
            if (entry != null && row.s("peerUid") == entry.peer && row.i("sendStatus") in listOf(0, 2, 3)) {
                if (row.i("sendStatus") == 0) entry.result.completeExceptionally(IllegalStateException("NTQQ 消息发送失败")) else entry.result.complete(
                    row
                )
            }
            if (row.s("senderUid") == account.s("uid") && row.i("sendStatus") == 2 && cardData(row) != null) forwards["${
                row.i(
                    "chatType"
                )
            }/${row.s("peerUid")}"]?.trySend(row)
        }
    }

    private fun receipt(row: JsonNode) = j(
        obj(
            "id" to row.s("msgId"),
            "seq" to row.s("msgSeq"),
            "time" to row.s("msgTime"),
            "sender" to account.s("id")
        )
    )

    suspend fun deliver(peer: Peer, elements: List<JsonNode>): JsonNode {
        val time = bridge.invoke("msf", "getServerTime")
        val marker = bridge.msg("generateMsgUniqueId", peer.chatType, time).asString()
        val completion = CompletableDeferred<JsonNode>()
        pending[marker] = Pending(peer.peerUid, completion)
        try {
            checkResult(bridge.msg("sendMsg", "0", peer.copy(guildId = marker), elements, nativeMap()))
            val row = withTimeout(30_000.milliseconds) { completion.await() }
            check(row.s("msgId").isNotBlank()) { "NTQQ 未确认消息发送成功" }
            return receipt(row)
        } finally {
            pending.remove(marker)
        }
    }

    suspend fun read(peer: Peer, rootId: String, parentId: String = rootId): List<JsonNode> {
        val root =
            bridge.msg("getMsgsByMsgId", peer, listOf(rootId)).list("msgList").firstOrNull { it.s("msgId") == rootId }
        val card = root?.let { cardData(it) }
        val resid = card?.path("meta")?.path("detail")?.s("resid").orEmpty()
        if (resid.isNotEmpty()) {
            val sections = reader.download(resid)
            var section = "MultiMsg"
            if (parentId != rootId) {
                val match = Regex("^forward:(.*):(\\d+)$").matchEntire(parentId)
                if (match != null) {
                    val parent = forwardRows(
                        sections,
                        URLDecoder.decode(match.groupValues[1], StandardCharsets.UTF_8),
                        peer
                    ).getOrNull(match.groupValues[2].toInt())
                    val inner =
                        checkNotNull(parent?.let { cardData(it) }) { "转发子记录不存在" }.path("meta").path("detail")
                    section = inner.s("uniseq")
                    if (!sections.containsKey(section)) return forwardRows(
                        reader.download(inner.s("resid")),
                        "MultiMsg",
                        peer
                    )
                } else return nativeRead(peer, rootId, parentId)
            }
            check(sections.containsKey(section)) { "转发子记录不存在" }
            return forwardRows(sections, section, peer)
        }
        return nativeRead(peer, rootId, parentId)
    }

    private fun forwardRows(sections: Map<String, List<ByteArray>>, section: String, peer: Peer) =
        ForwardReader.rows(sections, section, peer, account.s("uid"))

    private suspend fun nativeRead(peer: Peer, root: String, parent: String): List<JsonNode> {
        val result = bridge.msg("getMultiMsg", peer, root, parent)
        checkResult(result, "读取转发正文")
        return result.list("msgList").also { check(it.isNotEmpty()) { "NTQQ 转发正文为空" } }
    }

    suspend fun named(source: Peer, id: String, destination: Peer, display: JsonNode): JsonNode {
        val row = bridge.msg("getMsgsByMsgId", source, listOf(id)).list("msgList").firstOrNull { it.s("msgId") == id }
        val card = checkNotNull(row?.let { cardData(it) }) { "原消息缺少可用的合并转发资源" }.deepCopy() as ObjectNode
        read(source, id)
        val detail = card.path("meta").path("detail") as ObjectNode
        if (display.hasValue("title")) detail.put("source", display.s("title"))
        if (display.hasValue("summary")) detail.put("summary", display.s("summary"))
        if (display.hasValue("preview")) detail.set(
            "news",
            j(display.list("preview").map { obj("text" to it.asString()) })
        )
        if (display.hasValue("prompt")) {
            card.put("desc", display.s("prompt"))
            card.put("prompt", display.s("prompt"))
        }
        return deliver(
            destination,
            listOf(
                j(
                    obj(
                        "elementType" to 10,
                        "elementId" to "",
                        "arkElement" to obj("bytesData" to mapper.writeValueAsString(card))
                    )
                )
            )
        )
    }

    suspend fun forward(source: Peer, destination: Peer, ids: List<String>): JsonNode {
        val key = "${destination.chatType}/${destination.peerUid}"
        return locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            val rows = bridge.msg("getMsgsByMsgId", source, ids).list("msgList").associateBy { it.s("msgId") }
            require(ids.isNotEmpty() && ids.all { rows.containsKey(it) }) { "转发源消息不存在" }
            val expected = ids.map { contentKey(rows.getValue(it)) }
            val channel = Channel<JsonNode>(64)
            forwards[key] = channel
            try {
                val nodes = ids.map {
                    obj(
                        "msgId" to it,
                        "senderShowName" to rows.getValue(it).s("sendMemberName")
                            .ifEmpty { rows.getValue(it).s("sendNickName") })
                }
                checkResult(
                    bridge.msg(
                        "multiForwardMsgWithComment",
                        nodes,
                        source,
                        destination,
                        emptyList<Any>(),
                        nativeMap()
                    ), "合并转发"
                )
                withTimeout(30_000.milliseconds) {
                    var matched: JsonNode? = null
                    while (matched == null) {
                        val row = channel.receive()
                        // 比较原生输入和输出形式；服务端 Protobuf 视图可能将 Ark 转换为 XML
                        val children =
                            runCatching {
                                nativeRead(
                                    destination,
                                    row.s("msgId"),
                                    row.s("msgId")
                                )
                            }.onFailure { if (it is CancellationException) throw it }.getOrNull()
                        if (children?.map { contentKey(it) } == expected) matched = row
                    }
                    receipt(matched)
                }
            } finally {
                forwards.remove(key, channel)
                channel.close()
            }
        }
    }

    companion object {
        fun cardData(row: JsonNode): JsonNode? {
            for (element in row.list("elements")) {
                val native = element.path("multiForwardMsgElement")
                if (native.s("resId").isNotEmpty() || native.s("fileName").isNotEmpty()) return ForwardUpload.card(
                    native.s("resId"),
                    native.s("fileName"),
                    native.i("msgCount")
                )
                val card = runCatching { mapper.readTree(element.path("arkElement").s("bytesData")) }.getOrNull()
                if (card?.s("app") == "com.tencent.multimsg" && card.path("meta").path("detail").s("resid")
                        .isNotEmpty()
                ) return card
            }
            return null
        }

        fun contentKey(row: JsonNode) = row.list("elements").map { e ->
            listOf(
                e.s("elementType"),
                listOf(
                    "textElement" to "content",
                    "faceElement" to "faceIndex",
                    "picElement" to "md5HexStr",
                    "pttElement" to "md5HexStr",
                    "videoElement" to "videoMd5"
                ).firstNotNullOfOrNull { (a, b) -> e.path(a).takeIf { it.hasValue(b) }?.s(b) }.orEmpty()
            )
        }
    }
}
