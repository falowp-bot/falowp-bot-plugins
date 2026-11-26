package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import com.blr19c.falowp.bot.adapter.qqnative.core.transport.WorkerCall
import kotlinx.coroutines.withTimeout
import tools.jackson.databind.JsonNode
import kotlin.time.Duration.Companion.milliseconds

/**
 * 封装 Kotlin 与 Electron 的唯一调用边界，无损传递原生数据
 */
internal class NativeBridge(private val call: WorkerCall) {
    suspend fun invoke(service: String, method: String, vararg args: Any?): JsonNode =
        call("native", mapOf("service" to service, "method" to method, "arguments" to args.toList()))

    suspend fun msg(method: String, vararg args: Any?) = invoke("msg", method, *args)
    suspend fun packet(command: String, payload: ByteArray): ByteArray = withTimeout(30_000.milliseconds) {
        val result = msg("sendSsoCmdReqByContend", command, binary(payload))
        check(!result.hasValue("result") || result.i("result") == 0) { "QQ 请求失败：$command (${result.i("result")})" }
        val data = if (result.path("rspbuffer").has($$"$buffer")) result.path("rspbuffer") else result.path("rsp")
        check(data.has($$"$buffer")) { "QQ 二进制回执缺失：$command" }
        data.binary()
    }

    suspend fun oidb(command: Int, sub: Int, body: ByteArray): ByteArray {
        val result = packet(
            "OidbSvcTrpcTcp.0x${command.toString(16).uppercase()}_$sub",
            cat(n(1, command), n(2, sub), m(4, body), n(12, 1))
        )
        check(u(result, 3) == 0uL) { "QQ 媒体请求失败 (${u(result, 3)}): ${s(result, 5)}" }
        val payload = b(result, 4)
        val head = b(payload, 1)
        check(u(head, 2) == 0uL) { "QQ 媒体处理失败 (${u(head, 2)}): ${s(head, 3)}" }
        return payload
    }
}

internal fun checkResult(result: JsonNode, action: String = "发送") {
    val code = when {
        result.has("errCode") -> result.i("errCode")
        result.path("result").isNumber -> result.i("result")
        else -> error("NTQQ $action 缺少结果码")
    }
    check(code == 0) { "NTQQ ${action}失败 ($code)：${result.s("errMsg")}" }
}

internal data class Peer(val chatType: Int, val peerUid: String, val guildId: String = "")
