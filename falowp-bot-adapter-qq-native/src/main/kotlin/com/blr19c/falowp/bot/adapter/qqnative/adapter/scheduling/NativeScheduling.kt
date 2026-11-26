package com.blr19c.falowp.bot.adapter.qqnative.adapter.scheduling

import com.blr19c.falowp.bot.adapter.qqnative.adapter.QQNativeApplication
import com.blr19c.falowp.bot.adapter.qqnative.adapter.QQNativeBotApi
import com.blr19c.falowp.bot.adapter.qqnative.adapter.message.NativeMessages
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import kotlin.reflect.KClass

/**
 * 为定时任务提供当前 QQ 账号信息，供框架构建 BotApi 上下文
 */
internal object NativeScheduling : SchedulingBotApiSupport {
    override suspend fun supportReceive(receiveId: String): Boolean {
        val client = QQNativeApplication.client
        val account = client.status.accountId ?: return false
        return client.account(account).contacts.contains(receiveId)
    }

    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val client = QQNativeApplication.client
        val account = client.account(client.status.accountId.orEmpty())
        return QQNativeBotApi(
            ReceiveMessage.empty().copy(
                self = NativeMessages.self(account),
                adapter = ReceiveMessage.Adapter("QQNative", Unit)
            ), originalClass, account
        )
    }
}
