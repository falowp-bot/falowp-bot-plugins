package com.blr19c.falowp.bot.plugins.bili.api.api

import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.system.json.safeString
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/**
 * 扫码登录并把二维码地址交给调用方
 */
suspend fun BiliClient.login(push: suspend (url: String) -> Unit) {
    val qrcode = get(QRCODE_GENERATE) {
    }
    push(qrcode["url"].safeString())
    withTimeout(300_000.milliseconds) {
        // 二维码状态不变时每三秒再问一次
        while (isActive) {
            delay(3_000.milliseconds)
            val status = get(QRCODE_POLL) {
                parameter("qrcode_key", qrcode["qrcode_key"].safeString())
            }
            when (status["code"].asInt()) {
                0 -> return@withTimeout status
                86038 -> throw IllegalStateException(status["message"].safeString())
                else -> Unit
            }
        }
        return@withTimeout null
    }
}
