package com.blr19c.falowp.bot.adapter.qqnative.adapter

import com.blr19c.falowp.bot.adapter.qqnative.adapter.message.*
import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeAccount
import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessage
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingChain
import com.blr19c.falowp.bot.system.api.*
import tools.jackson.databind.JsonNode
import kotlin.reflect.KClass

/**
 * 框架 BotApi 的 QQ Native 实现，沿用接收消息中的账号和插件上下文
 */
class QQNativeBotApi(message: ReceiveMessage, originalClass: KClass<*>) : BotApi(message, originalClass) {

    private var boundAccount: QQNativeAccount? = null

    internal constructor(message: ReceiveMessage, originalClass: KClass<*>, account: QQNativeAccount) : this(
        message,
        originalClass
    ) {
        require(message.self.id == account.id)
        boundAccount = account
    }

    private val account get() = boundAccount ?: QQNativeApplication.client.account(receiveMessage.self.id)
    internal fun nativeAccount() = account

    internal suspend fun expand(operation: String, parameters: Map<String, Any?>): JsonNode =
        account.extensionCall(operation, parameters)

    override suspend fun self(): BotSelf = NativeMessages.self(account)

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) =
        deliver(listOf(QQNativeConversation.Group(sourceId)), sendMessageChain, reference, forward)

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) =
        deliver(listOf(QQNativeConversation.Private(sourceId)), sendMessageChain, reference, forward)

    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        deliver(account.contacts.groups(), sendMessageChain, reference, forward)
    }

    private suspend fun deliver(
        targets: List<QQNativeConversation>, chains: Array<out SendMessageChain>, reference: Boolean, forward: Boolean
    ) {
        if (chains.isEmpty() || targets.isEmpty()) return
        val sender = account
        val messages = sender.messages
        val replyTo = if (reference) receiveMessage.adapter.data as? QQNativeMessage else null
        OutboundMedia().use { media ->
            val outgoing =
                chains.map { chain -> QQNativeOutgoingChain(chain.messageList.map { media.encode(it) }) }.toTypedArray()
            for (target in targets) {
                if (forward) {
                    messages.send(target, *outgoing, replyTo = replyTo, forward = true)
                } else {
                    for (chain in outgoing) {
                        messages.send(target, chain, replyTo = replyTo)
                    }
                }
            }
        }
    }
}
