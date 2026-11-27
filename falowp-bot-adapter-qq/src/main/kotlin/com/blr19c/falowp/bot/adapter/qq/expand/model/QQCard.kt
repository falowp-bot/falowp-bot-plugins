package com.blr19c.falowp.bot.adapter.qq.expand.model

import java.net.URI

/**
 * 图文卡片的标题、摘要、跳转地址和封面
 */
data class QQCard(
    /**
     * 卡片标题，1 至 50 字符
     */
    val title: String,
    /**
     * 卡片摘要，1 至 200 字符
     */
    val description: String,
    /**
     * 跳转链接，HTTPS 地址且不超过 1024 字符
     */
    val url: String,
    /**
     * 封面链接，HTTPS 地址且不超过 1024 字符
     */
    val pictureUrl: String
) {
    init {
        require(title.codePointCount(0, title.length) in 1..50) { "卡片标题必须为 1 至 50 字符" }
        require(description.codePointCount(0, description.length) in 1..200) { "卡片摘要必须为 1 至 200 字符" }
        listOf(url, pictureUrl).forEach {
            val uri = URI(it)
            require(it.length <= 1024 && uri.scheme == "https" && !uri.host.isNullOrBlank()) { "卡片链接必须是有效 HTTPS 地址，且不超过 1024 字符" }
        }
    }

    internal fun payload(): Map<String, Any> = mapOf(
        "type" to "tuwen", "content" to mapOf(
            "title" to title, "description" to description, "url" to url, "pic_url" to pictureUrl
        )
    )
}
