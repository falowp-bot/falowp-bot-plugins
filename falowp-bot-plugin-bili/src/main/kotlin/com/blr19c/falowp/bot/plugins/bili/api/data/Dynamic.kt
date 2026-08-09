package com.blr19c.falowp.bot.plugins.bili.api.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 旧版动态详情接口的返回内容
 */
data class BiliDynamicInfo(
    /**
     * 动态详情
     */
    @field:JsonProperty("card")
    val dynamic: DynamicInfo,
)

/**
 * 用户空间动态列表
 */
data class BiliSpaceDynamicInfo(
    /**
     * 这一页的动态
     */
    @field:JsonProperty("items")
    val items: List<SpaceDynamicInfo>,
)

/**
 * 旧版动态详情
 */
data class DynamicInfo(
    /**
     * JSON 格式的动态卡片
     */
    @field:JsonProperty("card")
    val card: String,
)

/**
 * 用户空间里的一条动态
 */
data class SpaceDynamicInfo(
    /**
     * 动态 ID
     */
    @field:JsonProperty("id_str")
    val id: String,
    /**
     * 动态类型
     */
    @field:JsonProperty("type")
    val type: String
)
