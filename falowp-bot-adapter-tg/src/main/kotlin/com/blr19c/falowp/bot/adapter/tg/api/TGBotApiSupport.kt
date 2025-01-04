package com.blr19c.falowp.bot.adapter.tg.api

import com.blr19c.falowp.bot.adapter.tg.database.TGUserInfo
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import kotlin.reflect.KClass

/**
 * 电报API支持
 */
object TGBotApiSupport : SchedulingBotApiSupport, Log {

    val groupTypeList = listOf("group", "supergroup", "channel")

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val message = ReceiveMessage.empty().copy(source = ReceiveMessage.Source.empty().copy(id = receiveId))
        return TGBotApi(message, originalClass)
    }

    override suspend fun supportReceive(receiveId: String): Boolean {
        val sourceId = receiveId.toLongOrNull() ?: return false
        return TGUserInfo.queryByUserId(sourceId) != null || TGUserInfo.queryBySourceId(sourceId).isNotEmpty()
    }
}