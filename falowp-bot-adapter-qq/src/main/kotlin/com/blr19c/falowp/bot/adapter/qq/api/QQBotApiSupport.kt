package com.blr19c.falowp.bot.adapter.qq.api

import com.blr19c.falowp.bot.adapter.qq.op.OpReceiveMessage
import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQReceiveMessage
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.MessageTypeEnum
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.fasterxml.jackson.core.type.TypeReference
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass

object QQBotApiSupport : SchedulingBotApiSupport, Log {

    private val messageTypeReference = object : TypeReference<OpReceiveMessage<OpQQReceiveMessage>>() {}

    /**
     * 所有群聊列表
     */
    val groupIdList = CopyOnWriteArraySet<String>()

    override suspend fun supportReceive(receiveId: String): Boolean {
        return false
    }

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val message = ReceiveMessage.empty().copy(source = ReceiveMessage.Source.empty().copy(id = receiveId))
        return QQBotApi(message, originalClass)
    }

    fun dispatchMessage(readBytes: ByteArray) {
        val opReceiveMessage = Json.readObj(readBytes, messageTypeReference)
        val messageId = opReceiveMessage.d.id
        val messageType = MessageTypeEnum.MESSAGE
        val message = opReceiveMessage.d.content.trim().substringAfter("/")
        val content = ReceiveMessage.Content(
            message,
            null,
            emptyList(),
            emptyList(),
            emptyList()
        ) { null }
        val sender = ReceiveMessage.User(
            opReceiveMessage.d.author.id,
            "",
            ApiAuth.ORDINARY_MEMBER,
            ImageUrl.empty()
        )
        val sourceType = if (opReceiveMessage.t.isDirect()) SourceTypeEnum.PRIVATE else SourceTypeEnum.GROUP
        val source = ReceiveMessage.Source(opReceiveMessage.d.groupId ?: opReceiveMessage.d.author.id, sourceType)
        val self = ReceiveMessage.Self("")
        val receiveMessage = ReceiveMessage(messageId, messageType, content, sender, source, self)
        opReceiveMessage.d.groupId?.let { groupIdList.add(it) }
        PluginManagement.message(receiveMessage, QQBotApi::class)
    }
}