package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand

import com.blr19c.falowp.bot.adapter.qqnative.adapter.QQNativeBotApi
import com.blr19c.falowp.bot.system.json.Json

/**
 * 调用扩展接口并转换返回类型
 */
internal suspend inline fun <reified T> QQNativeBotApi.expandRequest(
    operation: String,
    parameters: Map<String, Any?> = emptyMap()
): T =
    Json.convertValue<T>(expand(operation, parameters))

/**
 * 调用无返回值的扩展接口
 */
internal suspend fun QQNativeBotApi.expandUnit(operation: String, parameters: Map<String, Any?> = emptyMap()) {
    expand(operation, parameters)
}
