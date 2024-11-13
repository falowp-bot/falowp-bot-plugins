package com.blr19c.falowp.bot.adapter.qq.api

import com.blr19c.falowp.bot.adapter.qq.QQApplication.Companion.token
import com.blr19c.falowp.bot.adapter.qq.api.QQBotApiSupport.groupIdList
import com.blr19c.falowp.bot.adapter.qq.op.OpBotApi
import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQMessageTypeEnum.*
import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQSendMessage
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
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
        val message = chain.content
        val contentMessage = OpQQSendMessage(index.incrementAndGet(), TEXT, message, null, reference, receiveMessage.id)
        val imageMessage = chain.imageList
            .map { OpQQSendMessage(index.incrementAndGet(), MEDIA, null, it, reference, receiveMessage.id) }
        return listOf(contentMessage) + imageMessage
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
}