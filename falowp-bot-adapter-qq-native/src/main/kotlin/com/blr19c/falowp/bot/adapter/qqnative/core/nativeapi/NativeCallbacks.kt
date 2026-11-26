package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import tools.jackson.databind.JsonNode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

/**
 * 在调用原生接口前注册等待器，因为回调可能先于请求确认到达
 */
internal class NativeCallbacks {
    private data class Waiter(
        val name: String,
        val accepts: (List<JsonNode>) -> Boolean,
        val reply: CompletableDeferred<List<JsonNode>>
    )

    private val pending = CopyOnWriteArrayList<Waiter>()

    // 部分原生回调不含请求 ID，因此将同名事件的请求串行化，
    // 避免一次响应错误地完成多个调用
    private val locks = ConcurrentHashMap<String, Mutex>()
    fun event(event: JsonNode) {
        val args = event.list("arguments")
        pending.filter { it.name == event.s("name") && it.accepts(args) }.forEach { it.reply.complete(args) }
    }

    suspend fun await(
        name: String,
        accepts: (List<JsonNode>) -> Boolean = { true },
        call: suspend () -> JsonNode
    ): List<JsonNode> = locks.computeIfAbsent(name) { Mutex() }.withLock {
        val waiter = Waiter(name, accepts, CompletableDeferred())
        pending.add(waiter)
        try {
            withTimeout(15_000.milliseconds) {
                val ack = call()
                if (ack.path("result").isNumber || ack.has("errCode")) checkResult(ack)
                waiter.reply.await()
            }
        } finally {
            pending.remove(waiter)
        }
    }
}
