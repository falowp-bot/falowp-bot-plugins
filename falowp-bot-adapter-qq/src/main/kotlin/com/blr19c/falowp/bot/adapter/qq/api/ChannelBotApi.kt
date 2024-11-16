package com.blr19c.falowp.bot.adapter.qq.api

import com.blr19c.falowp.bot.adapter.qq.QQApplication.Companion.token
import com.blr19c.falowp.bot.adapter.qq.op.OpBotApi
import com.blr19c.falowp.bot.adapter.qq.op.OpException
import com.blr19c.falowp.bot.adapter.qq.op.channel.OpChannelMessageContent
import com.blr19c.falowp.bot.adapter.qq.op.channel.OpChannelSendMessage
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlin.reflect.KClass

/**
 * 频道Api
 */
class ChannelBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) :
    OpBotApi<OpChannelSendMessage>(receiveMessage, originalClass) {

    override suspend fun buildSendMessage(chain: OpSendMessageChain): List<OpChannelSendMessage> {
        val reference = chain.messageReference?.let { OpChannelSendMessage.Reference(it) }
        val content = chain.content
        var contentMessage = emptyList<OpChannelSendMessage>()
        if (content.isNotBlank()) {
            val message = OpChannelMessageContent(content, chain.atList, emptyList())
            contentMessage = listOf(OpChannelSendMessage(message, null, reference, receiveMessage.id))
        }
        return contentMessage + chain.imageList.map {
            OpChannelSendMessage(
                OpChannelMessageContent("", chain.atList, emptyList()),
                it,
                reference,
                receiveMessage.id
            )
        }
    }

    override suspend fun allGroupId(): Set<String> {
        return ChannelBotApiSupport.channelIdList.toSet()
    }

    override suspend fun sendGroupMessage(
        sourceId: String,
        reference: Boolean,
        message: OpChannelSendMessage
    ) {
        val requestBody = Json.toJsonString(message)
        log().info("频道适配器发送群组消息:{}", message)
        val res = webclient().post(adapterConfigProperty("qq.apiUrl") + "/channels/$sourceId/messages") {
            setBody(requestBody)
            header(HttpHeaders.Authorization, token)
        }.body<String>()
        log().info("频道适配器发送群组消息返回结果:{}", res)
    }

    override suspend fun sendPrivateMessage(
        sourceId: String,
        reference: Boolean,
        message: OpChannelSendMessage
    ) {
        throw OpException("频道适配器不支持私聊")
    }
}