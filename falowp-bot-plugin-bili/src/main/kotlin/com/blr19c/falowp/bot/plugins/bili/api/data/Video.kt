package com.blr19c.falowp.bot.plugins.bili.api.data

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * 获取 AI 摘要时会用到的视频信息
 */
data class BiliVideoInfo(
    /**
     * 视频分 P 的 CID
     */
    @field:JsonProperty("cid")
    val cid: String,
    /**
     * 视频作者
     */
    @field:JsonProperty("owner")
    val owner: BiliVideoOwner,
)

/**
 * 视频作者
 */
data class BiliVideoOwner(
    /**
     * 作者 UID
     */
    @field:JsonProperty("mid")
    val mid: String,
)

/**
 * 视频 AI 摘要
 */
data class BiliVideoAiSummary(

    /**
     * 模型生成的摘要内容
     */
    @field:JsonProperty("model_result")
    val modelResult: BiliVideoAiSummaryInfo,
) {
    /**
     * 当前视频有没有可用的章节摘要
     */
    fun support(): Boolean {
        return modelResult.outline.isNotEmpty()
    }
}

/**
 * AI 摘要正文和章节
 */
data class BiliVideoAiSummaryInfo(
    /**
     * 摘要结果类型
     */
    @field:JsonProperty("result_type")
    val resultType: String,

    /**
     * 整段视频的摘要
     */
    @field:JsonProperty("summary")
    val summary: String,

    /**
     * 接口返回的章节列表
     */
    @field:JsonProperty("outline")
    private val _outline: List<BiliVideoAiSummaryOutline>?
) {
    /**
     * 没有章节时给调用方一个空列表
     */
    val outline: List<BiliVideoAiSummaryOutline>
        get() {
            return _outline ?: emptyList()
        }
}

/**
 * AI 摘要中的一个章节
 */
data class BiliVideoAiSummaryOutline(

    /**
     * 章节标题
     */
    @field:JsonProperty("title")
    val title: String,

    /**
     * 章节里的时间点摘要
     */
    @field:JsonProperty("part_outline")
    val part: List<BiliVideoAiSummaryOutlinePart>
)

/**
 * 章节中的一条时间点摘要
 */
data class BiliVideoAiSummaryOutlinePart(

    /**
     * 对应的视频秒数
     */
    @field:JsonProperty("timestamp")
    val timestamp: Long,

    /**
     * 这一段讲了什么
     */
    @field:JsonProperty("content")
    val content: String
)
