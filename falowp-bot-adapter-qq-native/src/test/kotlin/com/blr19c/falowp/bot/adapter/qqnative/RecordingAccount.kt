package com.blr19c.falowp.bot.adapter.qqnative

import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeAccount
import com.blr19c.falowp.bot.adapter.qqnative.core.contact.*
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import tools.jackson.databind.JsonNode

/**
 * 记录适配器交给账号的消息，其他操作默认失败
 */
internal class RecordingAccount(
    id: String, nickname: String,
    private val groups: List<String> = emptyList(),
    extension: suspend (String, Map<String, Any?>) -> JsonNode = { _, _ -> error("Unexpected extension") },
    private val onSend: (Sent) -> Unit = { error("Unexpected send") }
) : QQNativeContacts, QQNativeMessages {
    data class Sent(
        val to: QQNativeConversation, val chains: List<QQNativeOutgoingChain>,
        val replyTo: QQNativeMessage?, val forward: Boolean
    )

    val account = QQNativeAccount(id, this, this, nickname, extension)
    override suspend fun groups() = groups.distinct().map { QQNativeConversation.Group(it) }
    override suspend fun contains(id: String): Boolean = error("Unexpected contact lookup")
    override suspend fun send(
        to: QQNativeConversation, vararg chains: QQNativeOutgoingChain, replyTo: QQNativeMessage?, forward: Boolean
    ): List<QQNativeMessageReceipt> {
        onSend(Sent(to, chains.toList(), replyTo, forward))
        return listOf(QQNativeMessageReceipt("42", "3", "9", account.id))
    }

    override suspend fun forward(
        from: QQNativeConversation, to: QQNativeConversation, receipts: List<QQNativeMessageReceipt>
    ): QQNativeMessageReceipt = error("Unexpected forward")

    override suspend fun sendForwardCard(
        from: QQNativeConversation, id: String, to: QQNativeConversation, style: QQNativeForwardCardStyle
    ): QQNativeMessageReceipt = error("Unexpected card")

    override suspend fun readForward(
        from: QQNativeConversation, rootId: String, parentId: String
    ): List<QQNativeMessage> = error("Unexpected forward read")

    override suspend fun resolve(message: QQNativeMessage, reply: QQNativeReceivedPart.Reply): QQNativeMessage =
        error("Unexpected reference lookup")
}
