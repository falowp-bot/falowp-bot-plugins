package com.blr19c.falowp.bot.adapter.nc.api

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
        sendMessageChain.map { buildMessage(it, GROUP, sourceId) }.forEach {
            if (it.isNotEmpty()) this.sendGroupMsg(sourceId, it)
        }
    }

    override suspend fun sendAllGroup(
        vararg sendMessageChain: SendMessageChain,
        reference: Boolean,
        forward: Boolean
    ) {
        for (groupId in NapCatBotApiSupport.groupList.map { it.groupId }) {
            sendGroup(*sendMessageChain, sourceId = groupId, reference = reference, forward = forward)
        }
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {


        TODO("Not yet implemented")
    }

    /**
     * 单个消息
     */
    private suspend fun buildMessage(
        sendMessageChain: SendMessageChain,
        messageType: NapCatMessageType,
        sourceId: String
    ): List<NapCatMessage.Message> {
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

                is PokeSendMessage -> {
                    when (messageType) {
                        GROUP -> this.sendPoke(sourceId, sendMessage.poke)
                        PRIVATE -> this.sendPoke(userId = sendMessage.poke)
                        else -> {}
                    }
                    null
                }

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
    return this as NapCatBotApi
}