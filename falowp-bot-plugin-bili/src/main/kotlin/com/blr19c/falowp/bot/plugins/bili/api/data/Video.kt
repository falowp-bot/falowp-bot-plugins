package com.blr19c.falowp.bot.plugins.bili.api.data

import com.fasterxml.jackson.annotation.JsonProperty


data class BiliVideoInfo(
    @field:JsonProperty("cid")
    val cid: String,
    @field:JsonProperty("owner")
    val owner: BiliVideoOwner,
)

data class BiliVideoOwner(
    @field:JsonProperty("mid")
    val mid: String,
)

/**
 * 视频总结信息
 */
data class BiliVideoAiSummary(

    @field:JsonProperty("model_result")
    val modelResult: BiliVideoAiSummaryInfo,
) {
    fun support(): Boolean {
        return modelResult.outline.isNotEmpty()
    }
}

data class BiliVideoAiSummaryInfo(
    @field:JsonProperty("result_type")
    val resultType: String,

    @field:JsonProperty("summary")
    val summary: String,

    @field:JsonProperty("outline")
    private val _outline: List<BiliVideoAiSummaryOutline>?
) {
    val outline: List<BiliVideoAiSummaryOutline>
        get() {
            return _outline ?: emptyList()
        }
}

data class BiliVideoAiSummaryOutline(

    @field:JsonProperty("title")
    val title: String,

    @field:JsonProperty("part_outline")
    val part: List<BiliVideoAiSummaryOutlinePart>
)

data class BiliVideoAiSummaryOutlinePart(

    @field:JsonProperty("timestamp")
    val timestamp: Long,

    @field:JsonProperty("content")
    val content: String
)