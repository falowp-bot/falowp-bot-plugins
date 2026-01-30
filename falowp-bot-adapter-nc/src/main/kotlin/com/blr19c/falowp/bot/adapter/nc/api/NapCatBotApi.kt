package com.blr19c.falowp.bot.adapter.nc.api

import com.blr19c.falowp.bot.adapter.nc.expand.getGroupList
import com.blr19c.falowp.bot.adapter.nc.expand.sendForwardMsg
import com.blr19c.falowp.bot.adapter.nc.expand.sendGroupMsg
import com.blr19c.falowp.bot.adapter.nc.expand.sendPoke
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageDataType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageType.GROUP
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageType.PRIVATE
import com.blr19c.falowp.bot.system.api.*
import kotlin.reflect.KClass

/**
 * NapCatBotApi
 */
class NapCatBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) : BotApi(receiveMessage, originalClass) {

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        if (forward) {
            val messages = sendMessageChain.map { buildMessage(it) }
            this.sendForwardMsg(messageType = GROUP, groupId = sourceId, messages = messages)
            return
        }
        dealPoke(*sendMessageChain, messageType = GROUP, sourceId = sourceId)
        sendMessageChain.map { buildMessage(it) }.forEach {
            val message = buildReference(reference, it)
            if (message.isNotEmpty()) this.sendGroupMsg(sourceId, message)
        }
    }

    override suspend fun sendAllGroup(
        vararg sendMessageChain: SendMessageChain,
        reference: Boolean,
        forward: Boolean
    ) {
        for (groupId in this.getGroupList().map { it.groupId }) {
            sendGroup(*sendMessageChain, sourceId = groupId, reference = reference, forward = forward)
        }
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        if (forward) {
            val messages = sendMessageChain.map { buildMessage(it) }
            this.sendForwardMsg(messageType = PRIVATE, userId = sourceId, messages = messages)
            return
        }
        dealPoke(*sendMessageChain, messageType = PRIVATE, sourceId = sourceId)
        sendMessageChain.map { buildMessage(it) }.forEach {
            val message = buildReference(reference, it)
            if (message.isNotEmpty()) this.sendGroupMsg(sourceId, message)
        }
    }

    /**
     * 处理 POKE
     */
    private suspend fun dealPoke(
        vararg sendMessageChain: SendMessageChain,
        messageType: NapCatMessageType,
        sourceId: String
    ) {
        sendMessageChain.toList().flatMap { it.messageList }
            .filterIsInstance<NudgeSendMessage>()
            .forEach { sendMessage ->
                when (messageType) {
                    GROUP -> this.sendPoke(sourceId, sendMessage.id)
                    PRIVATE -> this.sendPoke(userId = sendMessage.id)
                    else -> {}
                }
            }
    }

    /**
     * 引用消息
     */
    private fun buildReference(reference: Boolean, messages: List<NapCatMessage.Message>): List<NapCatMessage.Message> {
        if (!reference || messages.isEmpty()) return messages
        val referenceMessage = NapCatMessage.Message(
            NapCatMessageDataType.REPLY,
            NapCatMessage.MessageData(id = this.receiveMessage.id)
        )
        return listOf(referenceMessage) + messages
    }

    /**
     * 单个消息
     */
    private suspend fun buildMessage(sendMessageChain: SendMessageChain): List<NapCatMessage.Message> {
        return sendMessageChain.messageList.mapNotNull { sendMessage ->
            when (sendMessage) {
                is AtSendMessage -> NapCatMessage.Message(
                    NapCatMessageDataType.AT,
                    NapCatMessage.MessageData(qq = sendMessage.at)
                )

                is TextSendMessage -> NapCatMessage.Message(
                    NapCatMessageDataType.TEXT,
                    NapCatMessage.MessageData(text = sendMessage.content)
                )

                is VoiceSendMessage -> NapCatMessage.Message(
                    NapCatMessageDataType.RECORD,
                    NapCatMessage.MessageData(file = sendMessage.voice.toASCIIString())
                )

                is ImageSendMessage -> NapCatMessage.Message(
                    NapCatMessageDataType.IMAGE,
                    NapCatMessage.MessageData(file = "base64://${sendMessage.image.toBase64()}")
                )

                is VideoSendMessage -> NapCatMessage.Message(
                    NapCatMessageDataType.VIDEO,
                    NapCatMessage.MessageData(file = sendMessage.video.toASCIIString())
                )

                else -> null
            }
        }
    }
}

/**
 * 转为 NapCatBotApi
 */
fun BotApi.nc(): NapCatBotApi {
    return this as? NapCatBotApi ?: NapCatBotApi(receiveMessage, originalClass)
}