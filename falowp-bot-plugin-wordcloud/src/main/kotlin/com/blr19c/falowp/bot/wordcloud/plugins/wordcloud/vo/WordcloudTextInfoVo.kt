package com.blr19c.falowp.bot.wordcloud.plugins.wordcloud.vo

/**
 * 词云消息记录
 */
data class WordcloudTextInfoVo(
    val id: Int,
    /**
     * 发送的消息
     */
    val text: String,
    /**
     * 用户id
     */
    val userId: String,
    /**
     * 来源id
     */
    val sourceId: String,
    /**
     * 来源类型
     */
    val sourceType: String,
)

