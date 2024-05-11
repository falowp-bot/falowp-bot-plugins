package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.data.LoginSso
import com.blr19c.falowp.bot.plugins.bili.api.data.Qrcode
import com.blr19c.falowp.bot.plugins.bili.api.data.QrcodeStatus
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout

suspend fun BiliClient.login(push: suspend (url: String) -> Unit) {
    val qrcode = json<Qrcode>(QRCODE_GENERATE) {
        // ...
    }

    push(qrcode.url)

    val status = withTimeout(300_000) {
        while (isActive) {
            delay(3_000)
            val status = json<QrcodeStatus>(QRCODE_POLL) {
                parameter("qrcode_key", qrcode.key)
            }
            when (status.code) {
                0 -> return@withTimeout status
                86038 -> throw IllegalStateException(status.message)
                else -> Unit
            }
        }
        return@withTimeout null
    } ?: return

    val regex = "bili_jct=([^&]+)".toRegex()
    val matchResult = regex.find(status.url)
    val csrf = matchResult?.groupValues?.get(1) ?: return
    val sso = json<LoginSso>(QRCODE_SSO_LIST) {
        setBody(FormDataContent(Parameters.build { append("csrf", csrf) }))
    }
    for (ticketUrl in sso.sso) {
        try {
            json<String>(ticketUrl) {
                request { method = HttpMethod.Post }
            }
        } catch (e: Exception) {
            log().error("登录请求sso-ticket失败", e)
        }
    }
}