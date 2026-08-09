package com.blr19c.falowp.bot.plugins.bili.api

import com.blr19c.falowp.bot.plugins.bili.database.BiliCookie
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.json.Json
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * 把 B 站登录 Cookie 保存在数据库里
 */
object DatabaseCookiesStorage : CookiesStorage, Log {

    /**
     * 第一次使用时从数据库加载 Cookie
     */
    private val container by lazy {
        multiTransaction {
            BiliCookie.selectAll()
                .map { Json.readObj<Cookie>(it[BiliCookie.cookie]) }
                .toMutableList()
        }
    }
    private val oldestCookie = AtomicLong(0L)
    private val mutex = Mutex()

    /**
     * 更新 Cookie 后同步写回数据库
     */
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit = mutex.withLock {
        if (cookie.name.isBlank()) return@withLock
        container.removeAll { it.name == cookie.name && it.matches(requestUrl) }
        container.add(cookie.fillDefaults(requestUrl))
        cookie.expires?.timestamp?.let { expires ->
            if (oldestCookie.get() > expires) {
                oldestCookie.set(expires)
            }
        }
        multiTransaction {
            BiliCookie.deleteAll()
            container.map { Json.toJsonString(it) }
                .forEach { cookieJson -> BiliCookie.insert { it[BiliCookie.cookie] = cookieJson } }
        }
    }

    /**
     * 获取能用于当前地址的 Cookie
     */
    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val now = getTimeMillis()
        if (now >= oldestCookie.get()) cleanup(now)
        return@withLock container.filter { it.matches(requestUrl) }
    }

    /**
     * 获取当前保存的全部 Cookie
     */
    suspend fun getAll(): List<Cookie> = mutex.withLock { container }

    /**
     * 获取提交表单时要用的 CSRF Token
     */
    suspend fun csrfToken(): String? = mutex.withLock {
        container.firstOrNull { it.name == "bili_jct" }
            ?.value
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * 获取当前登录的 B 站账号 UID
     */
    suspend fun uid(): String? = mutex.withLock {
        container.firstOrNull { it.name == "DedeUserID" }
            ?.value
            ?.takeIf { it.isNotBlank() }
    }

    override fun close() {

    }

    /**
     * 清掉已经过期的 Cookie
     */
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


    /**
     * 判断 Cookie 能不能发给当前地址
     */
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

    /**
     * 补上响应里省略的域名和路径
     */
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
