package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model

import com.blr19c.falowp.bot.system.api.SendMessage
import tools.jackson.databind.JsonNode
import java.net.URI

/**
 * 待发送的离线文件，folderId 仅用于群文件目录
 */
data class QQNativeFileMessage(val uri: URI, val name: String, val folderId: String = "") : SendMessage {
    init {
        require(name.isNotBlank()) { "文件名不能为空" }
    }
}

/**
 * 商城表情，id、packageId 和 key 从接收消息读取
 */
data class QQNativeMarketFaceMessage(
    val id: String, val packageId: Long, val key: String,
    val name: String = "[商城表情]"
) : SendMessage {
    init {
        require(id.matches(Regex("[a-fA-F0-9]{32}")) && packageId > 0 && key.isNotBlank()) { "商城表情参数无效" }
    }
}

/**
 * 骰子或猜拳消息，result 为空时由 QQ 生成结果
 */
data class QQNativeGameMessage(val dice: Boolean, val result: Int? = null) : SendMessage {
    init {
        require(result == null || result in 1..if (dice) 6 else 3) { "游戏结果超出范围" }
    }
}

/**
 * 已读取的原始消息元素，仅在绑定账号内使用
 */
class QQNativeOriginalMessage internal constructor(val accountId: String, val message: JsonNode) : SendMessage
