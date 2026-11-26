package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.*
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import tools.jackson.databind.JsonNode

/**
 * 校验账号绑定，直接调用当前会话的联系人、消息和扩展服务
 */
internal class NativeAccount(
    private val contacts: NativeContacts,
    private val messages: NativeMessages,
    private val delivery: NativeDelivery,
    private val extensions: NativeExtensions,
    private val checkActive: () -> Unit
) : QQNativeContacts, QQNativeMessages {
    override suspend fun groups(): List<QQNativeConversation.Group> {
        checkActive()
        return contacts.groups().map { QQNativeConversation.Group(it) }
    }

    override suspend fun contains(id: String): Boolean {
        checkActive()
        return contacts.has(id)
    }

    override suspend fun send(
        to: QQNativeConversation, vararg chains: QQNativeOutgoingChain,
        replyTo: QQNativeMessage?, forward: Boolean
    ): List<QQNativeMessageReceipt> {
        checkActive()
        return messages.send(to, chains.toList(), replyTo, forward).map { it.receipt() }
    }

    private suspend fun peer(conversation: QQNativeConversation) =
        messages.peer(conversation is QQNativeConversation.Group, conversation.id)

    override suspend fun forward(
        from: QQNativeConversation, to: QQNativeConversation, receipts: List<QQNativeMessageReceipt>
    ): QQNativeMessageReceipt {
        checkActive()
        require(receipts.isNotEmpty()) { "合并转发缺少消息" }
        return delivery.forward(peer(from), peer(to), receipts.map { it.id }).receipt()
    }

    override suspend fun sendForwardCard(
        from: QQNativeConversation, id: String, to: QQNativeConversation, style: QQNativeForwardCardStyle
    ): QQNativeMessageReceipt {
        checkActive()
        return delivery.named(peer(from), id, peer(to), j(style)).receipt()
    }

    override suspend fun readForward(
        from: QQNativeConversation, rootId: String, parentId: String
    ): List<QQNativeMessage> {
        checkActive()
        return delivery.read(peer(from), rootId, parentId).map { messages.decode(it).message() }
    }

    override suspend fun resolve(message: QQNativeMessage, reply: QQNativeReceivedPart.Reply): QQNativeMessage? {
        checkActive()
        if (reply.embedded) return reply.original
        return messages.reference(message, reply)?.message()
    }

    suspend fun expand(operation: String, parameters: Map<String, Any?>): JsonNode {
        checkActive()
        return extensions.call(operation, j(parameters))
    }
}
