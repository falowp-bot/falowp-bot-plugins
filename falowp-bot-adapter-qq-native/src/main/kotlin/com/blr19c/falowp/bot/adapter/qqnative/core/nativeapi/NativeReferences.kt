package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.history.HistoryKey
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MessageHistory
import tools.jackson.databind.JsonNode

/**
 * 在引用的原会话中读取原文，优先使用账号历史
 */
internal class NativeReferences(
    private val bridge: NativeBridge,
    private val history: MessageHistory,
    private val account: String
) {
    suspend fun read(peer: Peer, reply: JsonNode): JsonNode {
        require(peer.chatType in listOf(1, 2) && peer.peerUid.isNotBlank()) { "引用缺少原会话信息" }
        fun matches(row: JsonNode) = row.i("chatType") == peer.chatType && row.s("peerUid") == peer.peerUid
        val id = reply.s("replayMsgId").takeIf { it.isNotBlank() && it != "0" }
        if (id != null) {
            val stored = history.get(HistoryKey(account, peer.chatType, peer.peerUid, id))?.path("raw")
            if (stored != null && matches(stored) && stored.list("elements").isNotEmpty()) return stored
            bridge.msg("getMsgsByMsgId", peer, listOf(id)).list("msgList")
                .firstOrNull { matches(it) && it.s("msgId") == id && it.list("elements").isNotEmpty() }
                ?.let { return it }
        }
        val sequence = reply.s("replayMsgSeq").takeIf { it.isNotBlank() && it != "0" }
        if (sequence != null) bridge.msg("getSingleMsg", peer, sequence).list("msgList")
            .firstOrNull {
                matches(it) && it.s("msgSeq") == sequence && (id == null || it.s("msgId") == id) && it.list("elements")
                    .isNotEmpty()
            }
            ?.let { return it }
        val clientSeq = reply.s("replyMsgClientSeq").takeIf { it.isNotBlank() && it != "0" }
        val time = reply.s("replyMsgTime").takeIf { it.isNotBlank() && it != "0" }
        if (clientSeq != null && time != null) bridge.msg("getMsgByClientSeqAndTime", peer, clientSeq, time)
            .list("msgList")
            .firstOrNull {
                matches(it) && it.s("msgClientSeq") == clientSeq && it.s("msgTime") == time &&
                        (id == null || it.s("msgId") == id) && it.list("elements").isNotEmpty()
            }?.let { return it }
        error("无法读取引用原文：${peer.peerUid}/${id ?: sequence.orEmpty()}")
    }
}
