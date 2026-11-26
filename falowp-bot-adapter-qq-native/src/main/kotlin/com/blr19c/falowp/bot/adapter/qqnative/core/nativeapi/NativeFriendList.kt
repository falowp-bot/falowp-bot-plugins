package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import tools.jackson.databind.JsonNode
import java.util.Base64

/**
 * 读取单向好友列表，按服务端返回的游标续拉
 */
internal class NativeFriendList(private val bridge: NativeBridge, private val account: String) {
    suspend fun page(cookie: String, count: Int): JsonNode {
        require(count in 1..99) { "单页数量必须在 1 到 99 之间" }
        val payload = cat(
            Wire.n(2, 0), Wire.m(
                3, mapper.writeValueAsString(
                    obj(
                        "uint64_uin" to account, "uint64_top" to 0, "uint32_req_num" to count, "bytes_cookies" to cookie
                    )
                )
            )
        )
        val response = bridge.packet("MQUpdateSvc_com_qq_ti.web.OidbSvc.0xe17_0", payload)
        check(Wire.u(response, 3) == 0uL) { "单向好友查询失败：${Wire.s(response, 5)}" }
        val reply = mapper.readTree(Wire.s(response, 4))
        check(!reply.hasValue("ErrorCode") || reply.i("ErrorCode") == 0) {
            "单向好友查询失败 (${reply.s("ErrorCode")})：${reply.s("ErrorInfo")}"
        }
        check(!reply.hasValue("ActionStatus") || reply.s("ActionStatus") == "OK") { "单向好友查询未成功" }
        check(reply.path("rpt_block_list").isArray) { "单向好友响应缺少列表" }
        val rows = reply.list("rpt_block_list")
        fun decode(value: String) = text(Base64.getDecoder().decode(value))
        val friends = rows.map {
            check(it.s("uint64_uin").isNotBlank()) { "单向好友响应缺少 QQ 号" }
            obj(
                "uin" to it.s("uint64_uin"), "uid" to it.s("str_uid"),
                "nick_name" to decode(it.s("bytes_nick")), "age" to it.i("uint32_age"),
                "source" to decode(it.s("bytes_source"))
            )
        }
        val next = reply.s("bytes_cookies")
        // 满页且没有游标字段时，不能把截断结果当成完整列表
        val complete = next.isBlank() && (reply.has("bytes_cookies") || rows.size < count)
        return j(obj("friends" to friends, "nextCookie" to next, "complete" to complete, "raw" to reply))
    }

    suspend fun all(): JsonNode {
        val friends = linkedMapOf<String, JsonNode>()
        val visited = mutableSetOf<String>()
        var cookie = ""
        while (true) {
            check(visited.add(cookie)) { "单向好友分页游标重复" }
            val page = page(cookie, 99)
            page.list("friends").forEach { friends[it.s("uin")] = it }
            if (page.path("complete").asBoolean()) return j(friends.values.toList())
            cookie = page.s("nextCookie")
            check(cookie.isNotBlank()) { "单向好友列表已满页，但服务端未提供续拉游标，请使用 getUnidirectionalFriendPage 获取本页数据" }
        }
    }
}
