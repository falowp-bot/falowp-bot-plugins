package com.blr19c.falowp.bot.adapter.qq.op

import com.blr19c.falowp.bot.system.api.*
import kotlin.reflect.KClass

/**
 * 适配器Api
 */
abstract class OpBotApi<C>(receiveMessage: ReceiveMessage, originalClass: KClass<*>) :
    BotApi(receiveMessage, originalClass) {


    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        sendMessageChain.forEach {
            buildMessage(it, reference).forEach { message ->
                sendGroupMessage(sourceId, reference, message)
            }
        }
    }

    override suspend fun sendAllGroup(
        vararg sendMessageChain: SendMessageChain,
        reference: Boolean,
        forward: Boolean
    ) {
        for (channelId in allGroupId()) {
            sendMessageChain.forEach {
                buildMessage(it, reference).forEach { message ->
                    sendGroupMessage(channelId, reference, message)
                }
            }
        }
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        sendMessageChain.forEach {
            buildMessage(it, reference).forEach { message ->
                sendPrivateMessage(sourceId, reference, message)
            }
        }
    }

    abstract suspend fun buildSendMessage(chain: OpSendMessageChain): List<C>

    abstract suspend fun allGroupId(): Set<String>

    abstract suspend fun sendGroupMessage(sourceId: String, reference: Boolean, message: C)

    abstract suspend fun sendPrivateMessage(sourceId: String, reference: Boolean, message: C)

    data class OpSendMessageChain(
        val id: String,
        val messageReference: String?,
        val content: String,
        val atList: List<String>,
        val imageList: List<String>
    )

    private suspend fun buildMessage(sendMessageChain: SendMessageChain, reference: Boolean): List<C> {
        val messageReference = if (reference) receiveMessage.id else null
        val content = sendMessageChain.messageList.filterIsInstance<TextSendMessage>().joinToString("") { it.content }
        val atList = sendMessageChain.messageList.filterIsInstance<AtSendMessage>().map { it.at }.toList()
        val imageList = sendMessageChain.messageList
            .filterIsInstance<ImageSendMessage>()
            .map { it.image }.toList()
            .map { it.toUrl() }
        val chain = OpSendMessageChain(sendMessageChain.id, messageReference, content, atList, imageList)
        return buildSendMessage(chain)
    }
}