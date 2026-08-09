package com.blr19c.falowp.bot.plugins.bili.api.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 用户空间里的基本资料
 */
data class BiliUserInfo(
    /**
     * 昵称
     */
    @field:JsonProperty("name")
    val name: String,
    /**
     * 用户 UID
     */
    @field:JsonProperty("mid")
    val mid: String,
    /**
     * 绑定的直播间
     */
    @field:JsonProperty("live_room")
    val liveRoom: LiveRoom?,
)

@Suppress("SpellCheckingInspection")
/**
 * 用户的直播间状态
 */
data class LiveRoom(
    /**
     * 直播间 ID
     */
    @field:JsonProperty("roomid")
    val roomId: String,
    /**
     * 是否有直播间
     */
    @field:JsonProperty("roomStatus")
    val roomStatus: Boolean,
    /**
     * 当前是否开播
     */
    @field:JsonProperty("liveStatus")
    val liveStatus: Boolean,
)
