package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tools.jackson.databind.JsonNode
import java.net.HttpCookie
import java.net.URI
import java.net.URLEncoder
import java.net.http.*
import java.time.Duration

/**
 * 封装需要 QQ 登录凭据的网页接口；凭据不写入日志或界面状态
 */
internal class ExtensionWeb(private val bridge: NativeBridge, private val self: String) {
    private val http =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NEVER)
            .build()

    suspend fun pskey(domain: String): String {
        val result = bridge.invoke("tip", "getPskey", listOf(domain), true)
        return result.path("domainPskeyMap").mapEntries().firstOrNull { it.first == domain }?.second?.asString()
            ?.takeIf { it.isNotBlank() } ?: error("QQ 未返回 $domain 的票据")
    }

    suspend fun cookies(domain: String): Map<String, String> = withContext(Dispatchers.IO) {
        require(Regex("[a-zA-Z0-9.-]+\\.qq\\.com|qq\\.com").matches(domain)) { "域名必须属于 qq.com" }
        val ticket = bridge.invoke("ticket", "forceFetchClientKey", "")
        checkResult(ticket)
        val target = "https://$domain/$self/infocenter"
        val uri = URI(
            "https://ssl.ptlogin2.qq.com/jump?ptlang=1033&clientuin=$self&clientkey=${encode(ticket.s("clientKey"))}&u1=${
                encode(target)
            }&keyindex=19%27"
        )
        val reply = http.send(
            HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build(),
            HttpResponse.BodyHandlers.discarding()
        )
        check(reply.statusCode() in 200..399) { "QQ 登录票据交换失败 (${reply.statusCode()})" }
        val values =
            reply.headers().allValues("set-cookie").flatMap { HttpCookie.parse(it) }.associate { it.name to it.value }
                .toMutableMap()
        if (values["p_skey"].isNullOrBlank()) values["p_skey"] = pskey(domain)
        values.putIfAbsent("uin", "o$self")
        values.putIfAbsent("p_uin", "o$self")
        values
    }

    suspend fun request(
        url: String,
        body: ByteArray? = null,
        contentType: String = "application/json",
        cookies: Map<String, String>
    ): JsonNode = withContext(Dispatchers.IO) {
        val builder = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(60))
            .header("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
        if (body == null) builder.GET() else builder.header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
        val result = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
        check(result.statusCode() in 200..299) { "QQ Web 请求失败 (${result.statusCode()})" }
        val json = mapper.readTree(result.body())
        for (key in listOf(
            "ret",
            "retcode",
            "ec",
            "code"
        )) if (json.hasValue(key)) check(json.i(key) == 0) { "QQ Web 返回错误：${json.s(key)} ${json.s("msg")}" }
        json
    }

    companion object {
        fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)
        fun bkn(key: String): Int {
            var hash = 5381
            key.forEach { hash += (hash shl 5) + it.code }
            return hash and Int.MAX_VALUE
        }

        fun csrf(domain: String, cookies: Map<String, String>): Int {
            val qzone = domain == "qzone.qq.com" || domain.endsWith(".qzone.qq.com")
            val key = (if (qzone) cookies["p_skey"]?.takeIf { it.isNotBlank() } else null) ?: cookies["skey"]
            return bkn(checkNotNull(key?.takeIf { it.isNotBlank() }) { "QQ 未返回 CSRF 票据" })
        }
    }
}
