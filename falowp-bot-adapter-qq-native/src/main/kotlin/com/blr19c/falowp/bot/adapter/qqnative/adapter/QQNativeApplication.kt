package com.blr19c.falowp.bot.adapter.qqnative.adapter

import com.blr19c.falowp.bot.adapter.qqnative.adapter.message.NativeMessages
import com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.nativeNotice
import com.blr19c.falowp.bot.adapter.qqnative.adapter.scheduling.NativeScheduling
import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeClient
import com.blr19c.falowp.bot.adapter.qqnative.ui.registerSessionRoutes
import com.blr19c.falowp.bot.system.adapter.*
import com.blr19c.falowp.bot.system.plugin.PluginManagement

/**
 * 注册 QQ Native 适配器、消息事件入口和定时任务支持
 */
@BotAdapter(name = "QQNative")
class QQNativeApplication : BotAdapterInterface {
    override suspend fun start(register: BotAdapterRegister) {
        registerSessionRoutes { client.status }
        AdapterApplication.botApiSupportRegister(NativeScheduling)
        client.start()
        register.finish(this)
    }

    internal companion object {
        val client = QQNativeClient({ event, account ->
            PluginManagement.message(NativeMessages.decode(event, account), QQNativeBotApi::class)
        }, ::nativeNotice)
    }
}
