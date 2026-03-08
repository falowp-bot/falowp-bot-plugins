package com.blr19c.falowp.bot.adapter.nc.api

import com.blr19c.falowp.bot.adapter.nc.expand.*
import com.blr19c.falowp.bot.adapter.nc.expand.NapCatGoCqHttpExpand.Nested
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageDataType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageType.GROUP
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageType.PRIVATE
import com.blr19c.falowp.bot.adapter.nc.message.expand.NapCatSendMessage
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
        if (forward && sendMessageChain.size == 1) {
            val messages = sendMessageChain.flatMap { buildMessage(it).splitIndependent() }
            this.sendForwardMsg(messageType = GROUP, groupId = sourceId, messages = messages)
                .saveHistory(*sendMessageChain, forward = true)
            return
        }
        if (forward) {
            this.sendMultiForwardMsg(
                messageType = GROUP,
                groupId = sourceId,
                messageNested = multiForwardNestedBuild(*sendMessageChain)
            ).saveHistory(*sendMessageChain, forward = true)
            return
        }
        dealPoke(*sendMessageChain, messageType = GROUP, sourceId = sourceId)
        sendMessageChain.map { it to buildMessage(it).splitIndependent() }.forEach {
            it.second.forEach { messages ->
                val message = buildReference(reference, messages)
                if (message.isNotEmpty()) this.sendGroupMsg(sourceId, message).saveHistory(it.first)
            }
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
        if (forward && sendMessageChain.size == 1) {
            val messages = sendMessageChain.flatMap { buildMessage(it).splitIndependent() }
            this.sendForwardMsg(messageType = PRIVATE, userId = sourceId, messages = messages)
                .saveHistory(*sendMessageChain, forward = true)
            return
        }
        if (forward) {
            this.sendMultiForwardMsg(
                messageType = PRIVATE,
                userId = sourceId,
                messageNested = multiForwardNestedBuild(*sendMessageChain)
            ).saveHistory(*sendMessageChain, forward = true)
            return
        }
        dealPoke(*sendMessageChain, messageType = PRIVATE, sourceId = sourceId)
        sendMessageChain.map { it to buildMessage(it).splitIndependent() }.forEach {
            it.second.forEach { messages ->
                val message = buildReference(reference, messages)
                if (message.isNotEmpty()) this.sendGroupMsg(sourceId, message).saveHistory(it.first)
            }
        }
    }

    override suspend fun self(): BotSelf {
        return NapCatBotApiSupport.self()
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

                is EmojiSendMessage -> NapCatMessage.Message(
                    NapCatMessageDataType.FACE,
                    NapCatMessage.MessageData(
                        id = sendMessage.id,
                        type = sendMessage.type,
                        summary = sendMessage.display
                    )
                )

                is NapCatSendMessage -> sendMessage.message

                else -> null
            }
        }
    }

    /**
     * 多层合并转发构建
     */
    private suspend fun multiForwardNestedBuild(vararg sendMessageChain: SendMessageChain): Nested.Many<NapCatMessage.Message> {
        val messages = sendMessageChain.map { buildMessage(it).ignoreIndependent() }
        val messageNested = messages.map { innerList -> Nested.Many(innerList.map { msg -> Nested.One(msg) }) }
        return Nested.Many(messageNested)
    }

    /**
     * 忽略消息拆分
     */
    private fun List<NapCatMessage.Message>.ignoreIndependent(): List<NapCatMessage.Message> {
        return this.filter { it.type != NapCatMessageDataType.SPLIT_INDEPENDENT }.toList()
    }

    /**
     * 独立消息拆分
     */
    private fun List<NapCatMessage.Message>.splitIndependent(): List<List<NapCatMessage.Message>> {
        val result = mutableListOf<MutableList<NapCatMessage.Message>>()
        var buffer = mutableListOf<NapCatMessage.Message>()

        fun flush() {
            if (buffer.isNotEmpty()) {
                result.add(buffer)
                buffer = mutableListOf()
            }
        }

        fun single(message: NapCatMessage.Message) {
            flush()
            if (message.type != NapCatMessageDataType.SPLIT_INDEPENDENT)
                result.add(mutableListOf(message))
        }

        for (message in this) {
            if (message.type.independent()) {
                single(message)
            } else {
                buffer.add(message)
            }
        }
        flush()
        return result
    }
}

/**
 * 转为 NapCatBotApi
 */
fun BotApi.nc(): NapCatBotApi {
    return this as? NapCatBotApi ?: NapCatBotApi(receiveMessage, originalClass)
}