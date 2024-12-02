package com.blr19c.falowp.bot.plugins.bili.api.data

import com.fasterxml.jackson.annotation.JsonProperty

data class BiliLiveInfo(
    @field:JsonProperty("room_info")
    val roomInfo: RoomInfo,
    @field:JsonProperty("anchor_info")
    val anchorInfo: AnchorInfo,
)

data class RoomInfo(
    @field:JsonProperty("live_status")
    val liveStatus: Boolean,
    @field:JsonProperty("title")
    val title: String,
    @field:JsonProperty("cover")
    val cover: String,
    @field:JsonProperty("room_id")
    val roomId: String,
    @field:JsonProperty("uid")
    val uid: String,
)

data class AnchorInfo(
    @field:JsonProperty("base_info")
    val baseInfo: BaseInfo,
)

data class BaseInfo(
    @field:JsonProperty("face")
    val face: String,
    @field:JsonProperty("gender")
    val gender: String?,
    @field:JsonProperty("uname")
    val uname: String,
)