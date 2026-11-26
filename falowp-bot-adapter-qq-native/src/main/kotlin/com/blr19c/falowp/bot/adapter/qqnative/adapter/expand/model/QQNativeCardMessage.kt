package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model

import com.blr19c.falowp.bot.system.api.SendMessage
import tools.jackson.databind.JsonNode

/**
 * 待发送的原生 Ark 卡片消息
 */
data class QQNativeCardMessage(val json: JsonNode) : SendMessage {
    init {
        require(json.isObject && json.path("app").asString("").isNotBlank()) { "分享卡片缺少 app" }
    }
}
