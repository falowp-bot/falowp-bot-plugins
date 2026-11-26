package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import tools.jackson.databind.JsonNode

/**
 * 解析小程序、结构化分享和名片的展示字段
 */
internal object NativeShares {
    private fun JsonNode.value(vararg names: String): String = names.firstNotNullOfOrNull { name ->
        properties().firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
            ?.takeIf { it.isString || it.isNumber }?.asString()?.takeIf { it.isNotBlank() }
    }.orEmpty()

    fun decode(ark: JsonNode): JsonNode {
        val app = ark.value("app")
        val meta = ark.path("meta")
        val view = ark.value("view")
        val detail = meta.path(view).takeIf { it.isObject }
            ?: meta.values().firstOrNull { it.isObject } ?: j(null)
        val miniApp = app.startsWith("com.tencent.miniapp")
        val title = if (miniApp) detail.value("desc", "title") else detail.value("title", "nickname", "desc")
        return j(
            obj(
                "kind" to "share", "app" to app,
                "appId" to detail.value("appId").ifBlank { ark.value("bizSrc") },
                "appName" to detail.value(if (miniApp) "title" else "tag", "appName").ifBlank { app },
                "title" to title.ifBlank { ark.value("prompt", "desc") },
                "description" to detail.value("desc", "summary"),
                "url" to detail.value("qqDocUrl", "jumpUrl", "url"),
                "preview" to detail.value("preview", "avatar")
            )
        )
    }
}
