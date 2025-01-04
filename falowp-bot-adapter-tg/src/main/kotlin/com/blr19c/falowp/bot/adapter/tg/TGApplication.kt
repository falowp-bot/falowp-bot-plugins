package com.blr19c.falowp.bot.adapter.tg

import com.blr19c.falowp.bot.adapter.tg.api.TGBotApi
import com.blr19c.falowp.bot.adapter.tg.api.TGBotApiSupport
import com.blr19c.falowp.bot.adapter.tg.database.TGUserInfo
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapter.BotAdapter
import com.blr19c.falowp.bot.system.adapter.BotAdapterInterface
import com.blr19c.falowp.bot.system.adapter.BotAdapterRegister
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.MessageTypeEnum
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.systemConfigProperty
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.name.SetMyName
import org.telegram.telegrambots.meta.api.objects.File
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.io.Serializable
import java.net.URI


/**
 * 电报适配器
 */
@BotAdapter(name = "TG")
class TGApplication : BotAdapterInterface, Log {

    private lateinit var telegramBotsApi: TelegramBotsApi

    override suspend fun start(register: BotAdapterRegister) {
        telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
        telegramBotsApi.registerBot(createTelegramLongPollingBot())
        register.finish(this)
    }

    companion object {
        private lateinit var telegramLongPollingBot: TelegramLongPollingBot

        fun telegramLongPollingBot() = telegramLongPollingBot
    }

    private fun createTelegramLongPollingBot(): TelegramLongPollingBot {
        val defaultBotOptions = DefaultBotOptions()
        val proxy = adapterConfigProperty("tg.proxyUrl") { "" }
        if (proxy.isNotBlank()) {
            val proxyUrl = URI.create(proxy)
            defaultBotOptions.proxyHost = proxyUrl.host
            defaultBotOptions.proxyPort = proxyUrl.port
            defaultBotOptions.proxyType = DefaultBotOptions.ProxyType.HTTP
        }
        telegramLongPollingBot = TelegramLongPollingBotImpl(defaultBotOptions, adapterConfigProperty("tg.token"))
        return telegramLongPollingBot
    }

    private class TelegramLongPollingBotImpl(
        botOptions: DefaultBotOptions,
        private val botToken: String,
    ) : TelegramLongPollingBot(botOptions, botToken), Log {

        private val meInfo by lazy { this.executeIgnoreException(GetMe())!! }

        init {
            this.executeIgnoreException(SetMyName(systemConfigProperty("nickname"), null))
        }

        override fun getBotUsername(): String {
            return meInfo.userName
        }

        override fun onUpdateReceived(update: Update) {
            if (update.hasMessage()) {
                val message = receiveMessage(update.message)
                PluginManagement.message(message, TGBotApi::class)
            }
        }

        private fun receiveMessage(message: Message): ReceiveMessage {
            return ReceiveMessage(
                message.messageId.toString(),
                receiveMessageMessageType(message),
                receiveMessageContent(message),
                receiveMessageSender(message),
                receiveMessageSource(message),
                receiveMessageSelf(),
                ReceiveMessage.Adapter("TG", message)
            )
        }

        private fun receiveMessageContent(message: Message): ReceiveMessage.Content {
            val atList = atMessage(message)
            return ReceiveMessage.Content(
                textMessage(message, atList),
                voiceMessage(message),
                atList,
                imageMessage(message),
                videoMessage(message),
                emptyList()
            ) { null }
        }

        private fun receiveMessageSender(message: Message): ReceiveMessage.User {
            TGUserInfo.saveOrUpdate(message)
            return ReceiveMessage.User(
                message.from.id.toString(),
                message.from.userName,
                apiAuth(message, message.from.id),
                getAvatar(message.from.id) ?: ImageUrl.empty()
            )
        }

        private fun receiveMessageSource(message: Message): ReceiveMessage.Source {
            return ReceiveMessage.Source(
                message.chat.id.toString(),
                when {
                    TGBotApiSupport.groupTypeList.contains(message.chat.type) -> SourceTypeEnum.GROUP
                    message.chat.type == "private" -> SourceTypeEnum.PRIVATE
                    else -> SourceTypeEnum.UNKNOWN
                }
            )
        }

        private fun receiveMessageMessageType(message: Message): MessageTypeEnum {
            if (message.hasVoice()) {
                return MessageTypeEnum.VOICE
            }
            if (message.hasVideo()) {
                return MessageTypeEnum.VIDEO
            }
            return MessageTypeEnum.MESSAGE
        }

        private fun receiveMessageSelf(): ReceiveMessage.Self {
            return ReceiveMessage.Self(meInfo.id.toString())
        }

        private fun textMessage(message: Message, atList: List<ReceiveMessage.User>): String {
            val originalText = message.text ?: ""
            val namesRegex = atList.joinToString("|") { "@${Regex.escape(it.nickname)}" }
            return originalText.replace(Regex(namesRegex), "").trim()
        }

        private fun voiceMessage(message: Message): URI? {
            if (!message.hasVoice()) return null
            return getFileUrl(message.voice.fileId)?.let { URI.create(it) }
        }

        private fun atMessage(message: Message): List<ReceiveMessage.User> {
            if (message.entities.isNullOrEmpty()) return emptyList()
            val atList = mutableListOf<ReceiveMessage.User>()
            for (entity in message.entities.filter { it.type == "mention" }) {
                val username = message.text.substring(entity.offset, entity.offset + entity.length).removePrefix("@")
                if (username == meInfo.userName) {
                    atList.add(
                        ReceiveMessage.User(
                            meInfo.id.toString(),
                            username,
                            apiAuth(message, meInfo.id),
                            getAvatar(meInfo.id) ?: ImageUrl.empty()
                        )
                    )
                    continue
                }
                val userInfo = TGUserInfo.queryByUserName(username, message) ?: continue
                val user = ReceiveMessage.User(
                    userInfo.userId.toString(),
                    username,
                    apiAuth(message, userInfo.userId),
                    getAvatar(userInfo.userId) ?: ImageUrl.empty()
                )
                atList.add(user)
            }
            return atList
        }

        private fun imageMessage(message: Message): List<ImageUrl> {
            if (!message.hasPhoto()) return emptyList()
            return message.photo.mapNotNull { photo -> getFileUrl(photo.fileId) }.map { ImageUrl(it) }
        }

        private fun videoMessage(message: Message): ReceiveMessage.Video? {
            if (!message.hasVideo()) return null
            val fileUrl = getFileUrl(message.video.fileId) ?: return null
            val thumbnail = getFileUrl(message.video.thumbnail.fileId) ?: return null
            return ReceiveMessage.Video(ImageUrl(thumbnail), URI.create(fileUrl), message.video.fileSize)
        }

        private fun apiAuth(message: Message, userId: Long): ApiAuth {
            if (systemConfigListProperty("administrator").contains(userId.toString())) {
                return ApiAuth.ADMINISTRATOR
            }
            val auth = this.executeIgnoreException(GetChatMember(message.chatId.toString(), userId))
            return when (auth?.status) {
                "creator", "administrator" -> ApiAuth.MANAGER
                else -> ApiAuth.ORDINARY_MEMBER
            }
        }

        private fun getAvatar(userId: Long): ImageUrl? {
            val avatarFileId = this.executeIgnoreException(GetUserProfilePhotos(userId, 0, 1))
                ?.photos?.firstOrNull()
                ?.maxBy { it.fileSize ?: 0 }
                ?.fileId
                ?: return null
            val fileUrl = this.getFileUrl(avatarFileId) ?: return null
            return ImageUrl(fileUrl)
        }

        private fun getFileUrl(fileId: String, fileSize: Long = 0): String? {
            if (fileSize / 1024.0 / 1024.0 > 20) {
                log().info("文件过大,TG-API不支持接收")
                return null
            }
            val file = this.executeIgnoreException(GetFile(fileId)) ?: return null
            return File.getFileUrl(this.botToken, file.filePath)
        }

        private fun <T : Serializable, Method : BotApiMethod<T>> executeIgnoreException(method: Method): T? {
            try {
                return this.execute(method)
            } catch (e: Exception) {
                log().error("TG execute 失败, method:{}", method, e)
                return null
            }
        }
    }
}