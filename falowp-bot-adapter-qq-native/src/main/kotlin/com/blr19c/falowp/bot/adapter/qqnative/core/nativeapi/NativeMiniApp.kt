package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import tools.jackson.databind.JsonNode
import java.util.UUID

internal suspend fun NativeBridge.miniApp(a: JsonNode): JsonNode {
    val template = when (a.s("type")) {
        "bili" -> listOf("1109937557", "cfc5f7b05b44b5956502edaecf9d2240", "51f90239b78a2e4994c11215f4c4ba15")
        "weibo" -> listOf("1109224783", "e482a3cc4e574d9b772e96ba6eec9ba2", "35bbb44dc68e65194cfacfb206b8f1f7")
        "" -> null
        else -> error("未知小程序模板")
    }
    val appId = template?.get(0) ?: a.s("appId")
    require(appId.isNotBlank()) { "缺少小程序 appId" }
    val body = m(
        4,
        m(1, m(2, byteArrayOf())),
        m(2, appId),
        m(3, a.s("title")),
        m(4, a.s("desc")),
        n(5, System.currentTimeMillis()),
        n(6, if (template != null) 1 else 1036),
        n(7, 1),
        n(8, 0),
        m(9, a.s("picUrl")),
        m(10, ""),
        m(11, a.s("jumpUrl")),
        m(12, template?.get(2)?.let { "https://miniapp.gtimg.cn/public/appicon/${it}_200.jpg" } ?: a.s("iconUrl")),
        n(13, 3),
        n(14, 0),
        m(15, template?.get(1) ?: UUID.randomUUID().toString().replace("-", "")),
        n(16, 0),
        m(17, a.s("webUrl")),
        m(18, byteArrayOf()),
        m(19, m(1, ""), m(2, "")),
        m(20, ""))
    val result = packet("LightAppSvc.mini_app_share.AdaptShareInfo", cat(m(2, "V1_PC_MINISDK_99.99.99_1_APP_A"), body))
    check(u(result, 2) == 0uL) { "小程序分享失败：${s(result, 3)}" }
    val raw = mapper.readTree(s(b(result, 4), 2))
    check(raw.s("appName").isNotBlank()) { "服务端未返回分享卡片" }
    return j(
        obj(
            "ver" to raw.path("ver"),
            "prompt" to raw.path("prompt"),
            "config" to raw.path("config"),
            "app" to raw.path("appName"),
            "view" to raw.path("appView"),
            "meta" to raw.path("metaData"),
            "miniappShareOrigin" to 3,
            "miniappOpenRefer" to "10002"
        )
    )
}
