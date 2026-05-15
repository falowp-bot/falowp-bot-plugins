package com.blr19c.falowp.bot.plugins.ai.vo

data class AiChatHistoryVo(
    val id: Int,
    val sourceId: String,
    val userId: String,
    val role: String,
    val content: String,
    val createTime: Long,
)
