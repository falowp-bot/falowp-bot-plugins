package com.blr19c.falowp.bot.adapter.qq.api

import com.blr19c.falowp.bot.adapter.qq.QQApplication.Companion.token
import com.blr19c.falowp.bot.adapter.qq.op.OpAttachment
import com.blr19c.falowp.bot.adapter.qq.op.OpReceiveMessage
import com.blr19c.falowp.bot.adapter.qq.op.channel.OpChannelReceiveMessage
import com.blr19c.falowp.bot.adapter.qq.op.channel.OpChannelUser
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.cache.CacheMap
import com.blr19c.falowp.bot.system.cache.CacheReference
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.expand.toImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.web.bodyAsArrayNode
import com.blr19c.falowp.bot.system.web.bodyAsJsonNode
import com.blr19c.falowp.bot.system.web.webclient
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.ArrayNode
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.reflect.KClass
import kotlin.streams.asSequence
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * 频道API支持
 */
object ChannelBotApiSupport : SchedulingBotApiSupport, Log {
    /**
     * 自己的id
     */
    val selfId by CacheReference(1.days) { selfId() }

    /**
     * 所有子频道列表
     */
    val channelIdList by CacheReference(1.hours) { channelIdList() }

    /**
     * 所有频道列表
     */
    val guildIdList by CacheReference(1.hours) { guildsIdList() }

    /**
     * 频道用户信息
     */
    val userInfoMap by CacheMap<Pair<String, String>, OpChannelUser>(1.hours) { (guildId, userId) ->
        loadUserInfo(guildId, userId)
    }

    private val messageTypeReference = object : TypeReference<OpReceiveMessage<OpChannelReceiveMessage>>() {}

    override suspend fun supportReceive(receiveId: String): Boolean {
        return channelIdList.contains(receiveId)
    }

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val message = ReceiveMessage.empty().copy(source = ReceiveMessage.Source.empty().copy(id = receiveId))
        return ChannelBotApi(message, originalClass)
    }

    suspend fun dispatchMessage(readBytes: ByteArray) {
        val opReceiveMessage = Json.readObj(readBytes, messageTypeReference)
        log().info("频道适配器接收到消息:{}", opReceiveMessage)
        val atList = opReceiveMessage.d.content.at
        val guildId = opReceiveMessage.d.guildId
        val message = opReceiveMessage.d.content.message.trim().substringAfter("/")
        val imageList = opReceiveMessage.d.attachments
        val content = ReceiveMessage.Content(
            message,
            null,
            atList(guildId, atList),
            imageList(imageList),
            emptyList()
        ) { null }
        val sender = ReceiveMessage.User(
            opReceiveMessage.d.author.id,
            opReceiveMessage.d.member.nick ?: opReceiveMessage.d.author.username,
            apiAuth(opReceiveMessage.d.member.roles, opReceiveMessage.d.author.id),
            opReceiveMessage.d.author.avatar.toImageUrl()
        )
        val sourceType = if (opReceiveMessage.t.isDirect()) SourceTypeEnum.PRIVATE else SourceTypeEnum.GROUP
        val source = ReceiveMessage.Source(opReceiveMessage.d.channelId, sourceType)
        val self = ReceiveMessage.Self(selfId)
        val messageId = opReceiveMessage.d.id
        val messageType = MessageTypeEnum.MESSAGE
        val adapter = ReceiveMessage.Adapter("QQ-CHANNEL", opReceiveMessage)
        val receiveMessage = ReceiveMessage(messageId, messageType, content, sender, source, self, adapter)
        PluginManagement.message(receiveMessage, ChannelBotApi::class)
    }

    private fun imageList(imageList: List<OpAttachment>?): List<ImageUrl> {
        imageList ?: return emptyList()
        return imageList.filter { it.contentType == "image/jpeg" }
            .map { it.url.toImageUrl() }
            .toList()
    }

    private suspend fun atList(guildId: String, atList: List<String>): List<ReceiveMessage.User> {
        return atList.map {
            val userInfo = userInfo(guildId, it)
            ReceiveMessage.User(
                it,
                userInfo.username,
                apiAuth(userInfo.roles, userInfo.id),
                userInfo.avatar.toImageUrl()
            )
        }.toList()
    }

    private fun apiAuth(roles: List<String>?, id: String): ApiAuth {
        if (systemConfigListProperty("administrator").contains(id)) {
            return ApiAuth.ADMINISTRATOR
        }
        roles ?: return ApiAuth.ORDINARY_MEMBER
        val adminRole = listOf("2", "4", "5")
        if (roles.any { adminRole.contains(it) }) {
            return ApiAuth.MANAGER
        }
        return ApiAuth.ORDINARY_MEMBER
    }


    private suspend fun userInfo(guildId: String, userId: String): OpChannelUser {
        return userInfoMap(guildId to userId)
    }

    private suspend fun guildsIdList(): List<String> {
        return webclient().get(adapterConfigProperty("qq.apiUrl") + "/users/@me/guilds") {
            header(HttpHeaders.Authorization, token)
        }.bodyAsArrayNode()
            .map { it["id"].asText() }
            .toList()
    }

    private suspend fun channelIdList(): List<String> {
        return guildIdList.map {
            webclient().get(adapterConfigProperty("qq.apiUrl") + "/guilds/${it}/channels") {
                header(HttpHeaders.Authorization, token)
            }.bodyAsArrayNode().map { data -> data["id"].asText() }
        }.flatMap { it.stream().asSequence() }.toList()
    }


    private suspend fun selfId(): String {
        return webclient().get(adapterConfigProperty("qq.apiUrl") + "/users/@me") {
            header(HttpHeaders.Authorization, token)
        }.bodyAsJsonNode()["id"].asText()
    }

    private suspend fun loadUserInfo(guildId: String, userId: String): OpChannelUser {
        val jsonNode =
            webclient().get(adapterConfigProperty("qq.apiUrl") + "/guilds/${guildId}/members/$userId") {
                header(HttpHeaders.Authorization, token)
            }.bodyAsJsonNode()
        val opUser = Json.readObj(jsonNode["user"], OpChannelUser::class)
        val nick = jsonNode["nick"].asText()
        val roles = (jsonNode["roles"] as ArrayNode).map { it.asText() }
        return opUser.copy(nick = nick, roles = roles)
    }
}