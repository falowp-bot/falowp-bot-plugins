package com.blr19c.falowp.bot.adapter.cq.api

import com.blr19c.falowp.bot.adapter.cq.expand.avatar
import com.blr19c.falowp.bot.adapter.cq.expand.getFriendList
import com.blr19c.falowp.bot.adapter.cq.expand.getGroupList
import com.blr19c.falowp.bot.adapter.cq.expand.getGroupMemberInfo
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.ReceiveMessage.User
import com.blr19c.falowp.bot.system.cache.CacheMap
import com.blr19c.falowp.bot.system.cache.CacheReference
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import com.blr19c.falowp.bot.system.systemConfigListProperty
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

object GoCqHttpBotApiSupport : SchedulingBotApiSupport {
    val tempBot = GoCQHttpBotApi(ReceiveMessage.empty(), this::class)

    val groupIdList by CacheReference(1.hours) { tempBot.getGroupList().map { it.groupId } }
    val friendList by CacheReference(1.hours) { tempBot.getFriendList() }

    val messageIdToCQMessageIdMap: LoadingCache<String, List<String>> = CacheBuilder.newBuilder()
        .maximumSize(adapterConfigProperty("cq.cacheMessageIdSize") { "10000" }.toLong())
        .expireAfterWrite(3.days.toJavaDuration())
        .build(CacheLoader.from { _ -> emptyList() })

    val allUserInfo by CacheMap<Pair<String, String?>, User>(1.hours) { (userId, groupId) ->
        if (groupId == null) {
            val user = friendList.singleOrNull { it.userId == userId } ?: return@CacheMap User.empty()
            return@CacheMap User(user.userId, user.nickname, apiAuth(user.userId), tempBot.avatar(user.userId))
        }
        if (!groupIdList.contains(groupId)) return@CacheMap User.empty()
        val user = tempBot.getGroupMemberInfo(groupId, userId)
        return@CacheMap User(user.userId, user.nickname, apiAuth(user.userId, user.role), tempBot.avatar(user.userId))
    }

    override suspend fun supportReceive(receiveId: String): Boolean {
        return groupIdList.contains(receiveId) || friendList.any { it.userId == receiveId }
    }

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val message = ReceiveMessage.empty().copy(source = ReceiveMessage.Source.empty().copy(id = receiveId))
        return GoCQHttpBotApi(message, originalClass)
    }

    override fun order(): Int {
        return Int.MAX_VALUE
    }

    suspend fun userInfo(userId: String, groupId: String?): User? {
        val user = allUserInfo(userId to groupId)
        return if (user == User.empty()) null else user
    }

    fun apiAuth(userId: String, role: String? = null): ApiAuth {
        if (systemConfigListProperty("administrator").contains(userId)) {
            return ApiAuth.ADMINISTRATOR
        }
        if (role == "owner" || role == "admin") {
            return ApiAuth.MANAGER
        }
        return ApiAuth.ORDINARY_MEMBER
    }
}
