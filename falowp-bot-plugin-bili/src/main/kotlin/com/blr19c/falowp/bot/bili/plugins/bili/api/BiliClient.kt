package com.blr19c.falowp.bot.bili.plugins.bili.api

import com.blr19c.falowp.bot.bili.plugins.bili.api.api.JSON_IGNORE
import com.blr19c.falowp.bot.bili.plugins.bili.api.api.SPACE
import com.blr19c.falowp.bot.bili.plugins.bili.api.api.WBI
import com.blr19c.falowp.bot.bili.plugins.bili.api.data.WbiImages
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.cache.CacheReference
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Duration.Companion.hours

open class BiliClient(private val timeout: Long = 15_000L) : Closeable, Log {
    companion object {
        val Json = Json {
            prettyPrint = true
            ignoreUnknownKeys = System.getProperty(JSON_IGNORE, "true").toBoolean()
            isLenient = true
            allowStructuredMapKeys = true
        }

        val DefaultIgnore: suspend (Throwable) -> Boolean = { it is IOException }

        internal fun getMixinKey(ae: String): String {
            val oe = arrayOf(
                46,
                47,
                18,
                2,
                53,
                8,
                23,
                32,
                15,
                50,
                10,
                31,
                58,
                3,
                45,
                35,
                27,
                43,
                5,
                49,
                33,
                9,
                42,
                19,
                29,
                28,
                14,
                39,
                12,
                38,
                41,
                13,
                37,
                48,
                7,
                16,
                24,
                55,
                40,
                61,
                26,
                17,
                0,
                1,
                60,
                51,
                30,
                4,
                22,
                25,
                54,
                21,
                56,
                59,
                6,
                63,
                57,
                62,
                11,
                36,
                20,
                34,
                44,
                52
            )
            return buildString {
                for (i in oe) {
                    append(ae[i])
                    if (length >= 32) break
                }
            }
        }

        fun load(): BiliClient {
            return object : BiliClient() {
                override val ignore: suspend (Throwable) -> Boolean = { cause ->
                    when (cause) {
                        is ConnectTimeoutException,
                        is SocketTimeoutException -> {
                            log().warn("Ignore ${cause.message}")
                            true
                        }

                        is java.net.NoRouteToHostException,
                        is java.net.UnknownHostException -> false

                        is java.io.IOException -> {
                            log().warn("Ignore ", cause)
                            true
                        }

                        else -> false
                    }
                }

                override val mutex: BiliApiMutex = BiliApiMutex(interval = 10 * 1000)
            }

        }
    }

    override fun close() = clients.forEach { it.close() }

    protected open val ignore: suspend (exception: Throwable) -> Boolean = DefaultIgnore

    private val storage = DatabaseCookiesStorage

    val AcceptAllCookiesStorage.container: MutableList<Cookie> by reflect()

    val salt by CacheReference(1.hours) { salt() }

    protected open fun client() = HttpClient(CIO) {
        defaultRequest {
            header(HttpHeaders.Origin, SPACE)
            header(HttpHeaders.Referrer, SPACE)
        }
        expectSuccess = true
        install(ContentNegotiation) {
            json(json = Json)
        }
        install(HttpTimeout) {
            socketTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            requestTimeoutMillis = null
        }
        install(HttpCookies) {
            storage = this@BiliClient.storage
        }
        install(UserAgent) {
            agent =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36"
        }
        ContentEncoding()
    }

    protected open val clients = MutableList(3) { client() }

    protected open var index = 0

    protected open val mutex = BiliApiMutex(10 * 1000L)

    suspend fun <T> useHttpClient(block: suspend (HttpClient, BiliApiMutex) -> T): T = supervisorScope {
        var cause: Throwable? = null
        while (isActive) {
            try {
                return@supervisorScope block(clients[index], mutex)
            } catch (throwable: Throwable) {
                cause = throwable
                if (isActive && ignore(throwable)) {
                    index = (index + 1) % clients.size
                } else {
                    throw throwable
                }
            }
        }
        throw CancellationException(null, cause)
    }

    private suspend fun salt(): String {
        val body = useHttpClient { http, _ ->
            http.get(WBI).body<JsonObject>()
        }
        val data = body.getValue("data") as JsonObject
        val images = Json.decodeFromJsonElement<WbiImages>(data.getValue("wbi_img"))
        val a = images.imgUrl.substringAfter("wbi/").substringBefore(".")
        val b = images.subUrl.substringAfter("wbi/").substringBefore(".")
        return getMixinKey(a + b)
    }
}