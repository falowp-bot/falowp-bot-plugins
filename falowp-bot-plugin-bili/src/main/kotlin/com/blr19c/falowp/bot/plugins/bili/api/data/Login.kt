package com.blr19c.falowp.bot.plugins.bili.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Qrcode(
    @SerialName("url")
    val url: String,
    @SerialName("qrcode_key")
    val key: String
)

@Serializable
data class QrcodeStatus(
    @SerialName("url")
    val url: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
)

@Serializable
data class LoginSso(
    @SerialName("sso")
    val sso: List<String>
)