package com.blr19c.falowp.bot.adapter.nc.api

import com.blr19c.falowp.bot.adapter.nc.expand.getFriendList
import com.blr19c.falowp.bot.adapter.nc.expand.getGroupList
import com.blr19c.falowp.bot.adapter.nc.expand.getGroupMemberInfo
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.blr19c.falowp.bot.system.systemConfigListProperty
import kotlin.reflect.KClass

/**
 * NapCatBotApi 定时支持工具
 */
object NapCatBotApiSupport : SchedulingBotApiSupport {

    val tempBot = NapCatBotApi(ReceiveMessage.empty(), this::class)

    override suspend fun supportReceive(receiveId: String): Boolean {
        return tempBot.getGroupList().any { it.groupId == receiveId }
                || tempBot.getFriendList().any { it.userId == receiveId }
    }

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val message = ReceiveMessage.empty().copy(source = ReceiveMessage.Source.empty().copy(id = receiveId))
        return NapCatBotApi(message, originalClass)
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
     * 获取好友信息转换为 ReceiveMessage.User
     */
    suspend fun getFriendInfo(userId: String): ReceiveMessage.User? {
        return tempBot.getFriendList().singleOrNull { it.userId == userId }?.toUser()
    }
}