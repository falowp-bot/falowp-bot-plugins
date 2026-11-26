package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tools.jackson.databind.JsonNode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * 调用配置的音乐签名服务，不向签名服务传递 QQ 登录凭据
 */
internal object NativeMusic {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
    suspend fun card(args: JsonNode): JsonNode = withContext(Dispatchers.IO) {
        require(
            args.s("type") in setOf("qq", "163", "kugou", "kuwo", "migu") && args.s("id").isNotBlank()
        ) { "音乐平台或歌曲 ID 无效" }
        val url = args.s("sign_url")
        require(url.isNotBlank()) { "请配置 qqNative.musicSignUrl，或在 musicMessage 中传入签名服务地址" }
        val uri = URI(url)
        require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) { "音乐签名服务地址无效" }
        val request =
            HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        j(
                            obj(
                                "type" to args.s("type"),
                                "id" to args.s("id")
                            )
                        ).toString()
                    )
                ).build()
        val result = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(result.statusCode() in 200..299) { "音乐签名服务请求失败 (${result.statusCode()})" }
        val response = mapper.readTree(result.body())
        val card = if (response.isString) mapper.readTree(response.asString()) else response
        check(
            card.isObject && card.s("app").isNotBlank() && card.hasValue("meta")
        ) { "音乐签名服务未返回有效 Ark 卡片" }
        card
    }
}
