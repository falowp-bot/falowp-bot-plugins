package com.blr19c.falowp.bot.adapter.tg.api

import com.blr19c.falowp.bot.adapter.tg.TGApplication.Companion.telegramLongPollingBot
import com.blr19c.falowp.bot.adapter.tg.database.TGUserInfo
import com.blr19c.falowp.bot.system.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVoice
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.reflect.KClass

/**
 * 电报BotApi
 */
class TGBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) : BotApi(receiveMessage, originalClass) {

    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        for (groupId in TGUserInfo.queryAllGroup()) {
            sendMessageChain.forEach { sendTelegramMessage(it, groupId.toString()) }
        }
    }

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        sendMessageChain.forEach { sendTelegramMessage(it, sourceId) }
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        sendMessageChain.forEach { sendTelegramMessage(it, sourceId) }
    }


    private suspend fun sendTelegramMessage(
        sendMessageChain: SendMessageChain,
        sourceId: String
    ) = withContext(Dispatchers.IO) {
        for (sendMessage in sendMessageChain.messageList) {
            when (sendMessage) {
                is AtSendMessage -> {
                    val at = sendMessage.at.toLongOrNull()
                        ?.let { TGUserInfo.queryByUserId(it, null) }
                        ?.userName ?: sendMessage.at
                    val message = SendMessage.builder()
                        .text("@${at}")
                        .chatId(sourceId)
                        .build()
                    telegramLongPollingBot().execute(message)
                }

                is TextSendMessage -> {
                    val message = SendMessage.builder()
                        .text(sendMessage.content)
                        .chatId(sourceId)
                        .build()
                    telegramLongPollingBot().execute(message)
                }

                is VoiceSendMessage -> {
                    val voice = InputFile("voice").setMedia(sendMessage.voice.toURL().openStream(), "voice")
                    val message = SendVoice.builder()
                        .voice(voice)
                        .chatId(sourceId)
                        .build()
                    telegramLongPollingBot().execute(message)
                }

                is ImageSendMessage -> {
                    val imageBytes = Base64.getDecoder().decode(sendMessage.image.toBase64())
                    val imageInputStream = ByteArrayInputStream(imageBytes)
                    val photo = InputFile("photo").setMedia(imageInputStream, "photo")
                    val message = SendPhoto.builder()
                        .photo(photo)
                        .chatId(sourceId)
                        .build()
                    telegramLongPollingBot().execute(message)
                }

                is VideoSendMessage -> {
                    val voice = InputFile("video").setMedia(sendMessage.video.toURL().openStream(), "video")
                    val message = SendVoice.builder()
                        .voice(voice)
                        .chatId(sourceId)
                        .build()
                    telegramLongPollingBot().execute(message)
                }
            }
        }
    }
}