package com.blr19c.falowp.bot.plugins.bili.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WbiImages(
    @SerialName("img_url")
    val imgUrl: String = "",
    @SerialName("sub_url")
    val subUrl: String = ""
)