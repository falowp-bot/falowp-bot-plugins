package com.blr19c.falowp.bot.adapter.qq.api

import com.blr19c.falowp.bot.adapter.qq.QQApplication.Companion.token
import com.blr19c.falowp.bot.adapter.qq.api.QQBotApiSupport.groupIdList
import com.blr19c.falowp.bot.adapter.qq.op.OpBotApi
import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQMessageTypeEnum.MEDIA
import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQMessageTypeEnum.TEXT
import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQSendMessage
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.web.bodyAsJsonNode
import com.blr19c.falowp.bot.system.web.webclient
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * qqApi
 */
class QQBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) :
    OpBotApi<OpQQSendMessage>(receiveMessage, originalClass) {
    private val index = AtomicInteger()

    override suspend fun buildSendMessage(chain: OpSendMessageChain): List<OpQQSendMessage> {
        val reference = chain.messageReference?.let { OpQQSendMessage.Reference(it) }
        val content = chain.content
        var contentMessage = emptyList<OpQQSendMessage>()
        if (content.isNotBlank()) {
            val message = OpQQSendMessage(index.incrementAndGet(), TEXT, content, null, reference, receiveMessage.id)
            contentMessage = listOf(message)
        }
        val imageMessage = chain.imageList
            .map { OpQQSendMessage(index.incrementAndGet(), MEDIA, "", toImageUrl(it), reference, receiveMessage.id) }
        return contentMessage + imageMessage
    }

    override suspend fun allGroupId(): Set<String> {
        return groupIdList
    }

    override suspend fun sendGroupMessage(
        sourceId: String,
        reference: Boolean,
        message: OpQQSendMessage
    ) {
        val requestBody = Json.toJsonString(message)
        log().info("QQ适配器发送群组消息:{}", requestBody)
        val res = webclient().post(adapterConfigProperty("qq.apiUrl") + "/v2/groups/${sourceId}/messages") {
            setBody(requestBody)
            header(HttpHeaders.Authorization, token)
        }.body<String>()
        log().info("QQ适配器发送群组消息返回结果:{}", res)
    }

    override suspend fun sendPrivateMessage(
        sourceId: String,
        reference: Boolean,
        message: OpQQSendMessage
    ) {
        val requestBody = Json.toJsonString(message)
        log().info("QQ适配器发送私聊消息:{}", requestBody)
        val res = webclient().post(adapterConfigProperty("qq.apiUrl") + "/v2/users/${sourceId}/messages") {
            setBody(requestBody)
            header(HttpHeaders.Authorization, token)
        }.body<String>()
        log().info("QQ适配器发送私聊消息返回结果:{}", res)
    }

    private suspend fun toImageUrl(imageUrl: String): JsonNode {
        val sourceId = this.receiveMessage.source.id
        val url = if (SourceTypeEnum.GROUP == this.receiveMessage.source.type) "/v2/groups/${sourceId}/files"
        else "/v2/users/${sourceId}/files"
        return webclient().post(adapterConfigProperty("qq.apiUrl") + url) {
            setBody(
                Json.toJsonString(
                    mapOf(
                        "file_type" to 1,
                        "url" to imageUrl,
                        "srv_send_msg" to false
                    )
                )
            )
            header(HttpHeaders.Authorization, token)
        }.bodyAsJsonNode()
    }
}