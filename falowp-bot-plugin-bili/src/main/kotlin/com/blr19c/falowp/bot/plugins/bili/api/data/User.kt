package com.blr19c.falowp.bot.plugins.bili.api.data

import com.fasterxml.jackson.annotation.JsonProperty

data class BiliUserInfo(
    @field:JsonProperty("name")
    val name: String,
    @field:JsonProperty("mid")
    val mid: String,
    @field:JsonProperty("live_room")
    val liveRoom: LiveRoom?,
)

data class LiveRoom(
    @field:JsonProperty("roomid")
    val roomId: String,
    @field:JsonProperty("roomStatus")
    val roomStatus: Boolean,
    @field:JsonProperty("liveStatus")
    val liveStatus: Boolean,
)