package com.blr19c.falowp.bot.adapter.qq

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApiSupport
import com.blr19c.falowp.bot.adapter.qq.core.client.QQClient
import com.blr19c.falowp.bot.adapter.qq.core.config.QQConfig
import com.blr19c.falowp.bot.adapter.qq.core.webhook.QQWebhook
import com.blr19c.falowp.bot.adapter.qq.core.websocket.QQGateway
import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.adapter.BotAdapter
import com.blr19c.falowp.bot.system.adapter.BotAdapterInterface
import com.blr19c.falowp.bot.system.adapter.BotAdapterRegister
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * QQ 官方单聊、群聊适配器，不注册频道能力
 */
@BotAdapter(name = "QQ")
class QQApplication : BotAdapterInterface {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 启动 QQ 接入并注册适配器
     * @param register 框架提供的适配器注册入口
     */
    override suspend fun start(register: BotAdapterRegister) {
        val config = QQConfig.load()
        val client = QQClient(config)
        QQBotApiSupport.configure(client, config.appId, config.groupMessageMode)
        AdapterApplication.botApiSupportRegister(QQBotApiSupport)
        when (config.mode) {
            "webhook" -> {
                QQWebhook(config) { QQBotApiSupport.dispatchMessage(it) }.register()
                register.finish(this)
            }

            "websocket" -> scope.launch {
                QQGateway(
                    client,
                    onEvent = { QQBotApiSupport.dispatchMessage(it) },
                    onSelf = { QQBotApiSupport.botSelf = it },
                    interactions = config.interactions,
                ).run { register.finish(this@QQApplication) }
            }
        }
    }
}
