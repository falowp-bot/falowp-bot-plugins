package com.blr19c.falowp.bot.adapter.qq.expand.model

import tools.jackson.databind.JsonNode

/**
 * 内嵌键盘，模板 ID 与自定义内容二选一
 */
data class QQKeyboard(
    /**
     * 平台键盘模板 ID，与 content 二选一
     */
    val id: String? = null,
    /**
     * 自定义键盘 JSON，使用官方 rows/buttons 结构，与 id 二选一
     */
    val content: JsonNode? = null
) {
    init {
        require((!id.isNullOrBlank()) xor (content != null)) { "QQ 键盘必须指定 id 或 content，且只能指定一种" }
        require(content == null || content.isObject) { "QQ 键盘 content 必须是对象" }
    }

    internal fun payload(): Map<String, Any> = if (content != null) mapOf("content" to content) else mapOf("id" to id!!)
}
