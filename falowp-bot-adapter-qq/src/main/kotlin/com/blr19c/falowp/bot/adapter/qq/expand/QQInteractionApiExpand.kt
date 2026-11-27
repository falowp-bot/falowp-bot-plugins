package com.blr19c.falowp.bot.adapter.qq.expand

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.expand.event.QQInteractionEvent
import io.ktor.http.*
import tools.jackson.databind.JsonNode

/**
 * 回应按钮或菜单交互，每个交互只能回应一次
 * @param event 待回应的交互事件，每个交互只能回应一次
 * @param code 响应码：0 成功，1 失败，2 频繁，3 重复，4 无权限，5 仅管理员
 * @return 平台原始 JSON 响应
 */
suspend fun QQBotApi.respondInteraction(event: QQInteractionEvent, code: Int = 0): JsonNode {
    require(event.id.isNotBlank()) { "交互 ID 不能为空" }
    require(code in 0..5) { "交互响应码必须为 0 至 5" }
    return apiRequest("/interactions/${event.id.encodeURLPathPart()}", mapOf("code" to code), HttpMethod.Put)
}
