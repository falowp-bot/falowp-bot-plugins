package com.blr19c.falowp.bot.adapter.nc.api

import com.blr19c.falowp.bot.adapter.nc.expand.*
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.adapter.nc.message.NapCatSelf
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.cache.CacheMap
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.blr19c.falowp.bot.system.systemConfigProperty
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.days

/**
 * NapCatBotApi 定时支持工具
 */
object NapCatBotApiSupport : SchedulingBotApiSupport {

    val tempBot = NapCatBotApi(ReceiveMessage.empty(), this::class)

    val messageHistory = CacheMap<String, MessageHistory>(
        2.days,
        adapterConfigProperty("nc.messageCacheSize") { "50000" }.toLong()
    ) {
        return@CacheMap MessageHistory("", false)
    }

    private val messageHistoryRef by messageHistory

    data class MessageHistory(val messageId: String, val forward: Boolean)

    override suspend fun supportReceive(receiveId: String): Boolean {
        return tempBot.getGroupList().any { it.groupId == receiveId }
                || tempBot.getFriendList().any { it.userId == receiveId }
    }

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val message = ReceiveMessage.empty().copy(source = ReceiveMessage.Source.empty().copy(id = receiveId))
        return NapCatBotApi(message, originalClass)
    }

    /**
     * 将消息ID转换为 NapCat 消息ID
     */
    suspend fun convertMessageId(messageId: String): String {
        return messageHistoryRef(messageId).messageId.ifBlank { messageId }
    }

    /**
     * 获取用户权限
     */
    fun apiAuth(userId: String, role: String? = null): ApiAuth {
        if (systemConfigListProperty("administrator").contains(userId)) {
            return ApiAuth.ADMINISTRATOR
        }
        if (role == "owner" || role == "admin") {
            return ApiAuth.MANAGER
        }
        return ApiAuth.ORDINARY_MEMBER
    }

    /**
     * 获取用户头像
     */
    fun avatar(userId: String): ImageUrl {
        return ImageUrl("https://q1.qlogo.cn/g?b=qq&nk=$userId&s=640")
    }

    /**
     * 自身信息
     */
    suspend fun self(): NapCatSelf {
        return NapCatSelf(this.tempBot.getLoginInfo())
    }

    /**
     * 自身信息的发送人对象
     */
    suspend fun selfSender(): NapCatMessage.Sender {
        return NapCatMessage.Sender(
            this.self().id,
            systemConfigProperty("nickname"),
            systemConfigProperty("nickname")
        )
    }

    /**
     * 综合获取群成员/好友信息并转换为 ReceiveMessage.User
     */
    suspend fun getUserInfo(userId: String, groupId: String?): ReceiveMessage.User? {
        return groupId?.let { getGroupMemberInfo(it, userId) } ?: getFriendInfo(userId)
    }

    /**
     * 获取群成员信息转换为 ReceiveMessage.User
     */
    suspend fun getGroupMemberInfo(groupId: String, userId: String): ReceiveMessage.User {
        return tempBot.getGroupMemberInfo(groupId, userId).toUser()
    }

    /**
     * 获取群聊群主信息
     */
    suspend fun getGroupOwner(groupId: String): ReceiveMessage.User? {
        return tempBot.getGroupMemberList(groupId).find { it.role == "owner" }?.toUser()
    }

    /**
     * 获取好友信息转换为 ReceiveMessage.User
     */
    suspend fun getFriendInfo(userId: String): ReceiveMessage.User? {
        return tempBot.getFriendList().singleOrNull { it.userId == userId }?.toUser()
    }

    /**
     * 用户ID生成临时用户信息
     */
    fun userIdToUser(userId: String): ReceiveMessage.User {
        return ReceiveMessage.User(userId, "", apiAuth(userId), avatar(userId))
    }
}