package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout

suspend fun BiliClient.login(push: suspend (url: String) -> Unit) {
    val qrcode = get(QRCODE_GENERATE) {
        // ...
    }
    push(qrcode["url"].asText())
    withTimeout(300_000) {
        while (isActive) {
            delay(3_000)
            val status = get(QRCODE_POLL) {
                parameter("qrcode_key", qrcode["key"].asText())
            }
            when (status["code"].asInt()) {
                0 -> return@withTimeout status
                86038 -> throw IllegalStateException(status["message"].asText())
                else -> Unit
            }
        }
        return@withTimeout null
    }
}