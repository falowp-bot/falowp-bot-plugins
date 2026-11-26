package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model

import tools.jackson.databind.JsonNode

/**
 * 消息表情回应的分页结果，保留原生数据与后续请求游标
 */
data class QQNativeEmojiLikes(
    val result: Int,
    val emojiLikesList: List<JsonNode>,
    val cookie: String,
    val isLastPage: Boolean,
    val isFirstPage: Boolean
)
