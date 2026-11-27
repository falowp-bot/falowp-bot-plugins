package com.blr19c.falowp.bot.adapter.qq.core.config

import com.blr19c.falowp.bot.system.adapterConfigProperty

/**
 * QQ 适配器配置
 * @property appId 开放平台应用 ID
 * @property secret 仅用于服务端鉴权和 Webhook 签名的 AppSecret
 * @property apiUrl REST API 基础地址，不包含末尾斜杠
 * @property tokenUrl Access Token 获取地址
 * @property mode 事件接入方式：websocket 或 webhook
 * @property webhookPath HTTP 回调路径
 * @property gatewayUrl WebSocket 接入点查询地址
 * @property interactions 是否订阅 QQ 交互事件
 * @property groupMessageMode 本地群消息过滤模式
 */
internal data class QQConfig(
    val appId: String,
    val secret: String,
    val apiUrl: String = "https://api.bot.qq.com",
    val tokenUrl: String = "https://api.bot.qq.com/app/getAppAccessToken",
    val mode: String = "websocket",
    val webhookPath: String = "/qq/webhook",
    val gatewayUrl: String = "$apiUrl/gateway",
    val groupMessageMode: QQGroupMessageMode = QQGroupMessageMode.ALL,
    val interactions: Boolean = true,
) {
    init {
        require(appId.isNotBlank()) { "请配置 bot.adapter.qq.appId" }
        require(secret.isNotBlank()) { "请配置 bot.adapter.qq.secret（AppSecret），不再支持旧 Token" }
        require(mode in setOf("websocket", "webhook")) { "qq.mode 仅支持 websocket 或 webhook" }
        require(webhookPath.startsWith("/")) { "qq.webhookPath 必须以 / 开头" }
    }

    companion object {
        /**
         * 从框架配置的 bot.adapter.qq 节点读取并校验配置
         */
        fun load(): QQConfig {
            val api = adapterConfigProperty("qq.apiUrl") { "https://api.bot.qq.com" }.trimEnd('/')
            return QQConfig(
                adapterConfigProperty("qq.appId"),
                adapterConfigProperty("qq.secret") { "" },
                api,
                adapterConfigProperty("qq.tokenUrl") { "https://api.bot.qq.com/app/getAppAccessToken" },
                adapterConfigProperty("qq.mode") { "websocket" }.lowercase(),
                adapterConfigProperty("qq.webhookPath") { "/qq/webhook" },
                adapterConfigProperty("qq.gatewayUrl") { "$api/gateway" },
                QQGroupMessageMode.valueOf(adapterConfigProperty("qq.groupMessageMode") { "all" }.trim().uppercase()),
                adapterConfigProperty("qq.interactions") { "true" }.toBooleanStrict(),
            )
        }
    }
}
