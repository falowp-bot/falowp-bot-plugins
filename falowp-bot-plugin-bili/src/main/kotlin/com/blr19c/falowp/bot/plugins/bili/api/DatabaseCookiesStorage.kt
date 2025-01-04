package com.blr19c.falowp.bot.plugins.bili.api

import com.blr19c.falowp.bot.plugins.bili.database.BiliCookie
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.json.Json
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

object DatabaseCookiesStorage : CookiesStorage, Log {

    private val container by lazy {
        transaction {
            BiliCookie.selectAll()
                .map { Json.readObj<Cookie>(it[BiliCookie.cookie]) }
                .toMutableList()
        }
    }
    private val oldestCookie = AtomicLong(0L)
    private val mutex = Mutex()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit = mutex.withLock {
        if (cookie.name.isBlank()) return@withLock
        container.removeAll { it.name == cookie.name && it.matches(requestUrl) }
        container.add(cookie.fillDefaults(requestUrl))
        cookie.expires?.timestamp?.let { expires ->
            if (oldestCookie.get() > expires) {
                oldestCookie.set(expires)
            }
        }
        transaction {
            BiliCookie.deleteAll()
            container.map { Json.toJsonString(it) }
                .forEach { cookieJson -> BiliCookie.insert { it[BiliCookie.cookie] = cookieJson } }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val now = getTimeMillis()
        if (now >= oldestCookie.get()) cleanup(now)
        return@withLock container.filter { it.matches(requestUrl) }
    }

    suspend fun getAll(): List<Cookie> = mutex.withLock { container }

    override fun close() {

    }

    private fun cleanup(timestamp: Long) {
        container.removeAll { cookie ->
            val expires = cookie.expires?.timestamp ?: return@removeAll false
            expires < timestamp
        }
        val newOldest = container.fold(Long.MAX_VALUE) { acc, cookie ->
            cookie.expires?.timestamp?.let { min(acc, it) } ?: acc
        }
        oldestCookie.set(newOldest)
    }


    private fun Cookie.matches(requestUrl: Url): Boolean {
        val domain = domain?.toLowerCasePreservingASCIIRules()?.trimStart('.')
            ?: error("Domain field should have the default value")

        val path = with(path) {
            val current = path ?: error("Path field should have the default value")
            if (current.endsWith('/')) current else "$path/"
        }

        val host = requestUrl.host.toLowerCasePreservingASCIIRules()
        val requestPath = let {
            val pathInRequest = requestUrl.encodedPath
            if (pathInRequest.endsWith('/')) pathInRequest else "$pathInRequest/"
        }

        if (host != domain && (hostIsIp(host) || !host.endsWith(".$domain"))) {
            return false
        }

        if (path != "/" &&
            requestPath != path &&
            !requestPath.startsWith(path)
        ) {
            return false
        }

        return !(secure && !requestUrl.protocol.isSecure())
    }

    private fun Cookie.fillDefaults(requestUrl: Url): Cookie {
        var result = this

        if (result.path?.startsWith("/") != true) {
            result = result.copy(path = requestUrl.encodedPath)
        }

        if (result.domain.isNullOrBlank()) {
            result = result.copy(domain = requestUrl.host)
        }

        return result
    }
}
