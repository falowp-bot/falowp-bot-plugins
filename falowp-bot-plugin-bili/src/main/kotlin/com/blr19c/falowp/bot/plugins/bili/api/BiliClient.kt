package com.blr19c.falowp.bot.plugins.bili.api

import com.blr19c.falowp.bot.plugins.bili.api.WBI.wbiParams
import com.blr19c.falowp.bot.plugins.bili.api.api.ORIGIN
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.web.bodyAsJsonNode
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class BiliClient : Log {

    private val storage = DatabaseCookiesStorage

    private val client = longTimeoutWebclient().config {
        defaultRequest {
            header(HttpHeaders.Origin, ORIGIN)
            header(HttpHeaders.Referrer, ORIGIN)
            header(HttpHeaders.AcceptLanguage, "en,zh-CN;q=0.9,zh;q=0.8")
        }
        install(HttpCookies) {
            storage = this@BiliClient.storage
        }
        expectSuccess = true
    }

    suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit = {}): JsonNode {
        return client.get(url) { block() }.bodyAsJsonNode()["data"]
    }

    suspend fun getText(url: String, block: HttpRequestBuilder.() -> Unit = {}): String {
        return client.get(url) { block() }.bodyAsText()
    }

    suspend fun post(url: String, block: HttpRequestBuilder.() -> Unit = {}): JsonNode {
        return client.post(url) { block() }.bodyAsJsonNode()["data"]
    }

    suspend fun wbiGet(url: String, block: MutableMap<String, String>.() -> Unit = {}): JsonNode {
        val build = mutableMapOf<String, String>()
        build.block()
        val params = wbiParams(build)
        return client.get(url) {
            params.forEach { (k, v) -> parameter(k, v) }
        }.bodyAsJsonNode()["data"]
    }
}