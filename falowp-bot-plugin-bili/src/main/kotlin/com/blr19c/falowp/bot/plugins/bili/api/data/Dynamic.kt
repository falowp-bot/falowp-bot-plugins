package com.blr19c.falowp.bot.plugins.bili.api.data

import com.fasterxml.jackson.annotation.JsonProperty

data class BiliDynamicInfo(
    @field:JsonProperty("card")
    val dynamic: DynamicInfo,
)

data class BiliSpaceDynamicInfo(
    @field:JsonProperty("items")
    val items: List<SpaceDynamicInfo>,
)

data class DynamicInfo(
    @field:JsonProperty("card")
    val card: String,
)

data class SpaceDynamicInfo(
    @field:JsonProperty("id_str")
    val id: String,
    @field:JsonProperty("type")
    val type: String
)
