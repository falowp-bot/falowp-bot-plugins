package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.mapper
import tools.jackson.databind.JsonNode
import java.security.MessageDigest

/**
 * 历史记录使用账号、会话和消息 ID 共同定位，避免不同账号或会话中的消息 ID 冲突
 */
internal data class HistoryKey(val account: String, val chatType: Int, val peer: String, val message: String) {
    val hash: String
        get() = MessageDigest.getInstance("SHA-256")
            .digest(mapper.writeValueAsBytes(listOf(account, chatType.toString(), peer, message)))
            .joinToString("") { "%02x".format(it) }
}

/**
 * 按账号及会话隔离的消息原文存储接口
 */
internal interface MessageHistory {
    suspend fun get(key: HistoryKey): JsonNode?
    suspend fun put(key: HistoryKey, message: JsonNode)

    /**
     * 在当前账号内查找消息所属会话，保留冲突结果供调用方校验
     */
    suspend fun find(account: String, message: String): List<HistoryKey>
}

/**
 * 容量受限的内存原文缓存；读写时复制节点以隔离调用方修改
 */
internal class MemoryMessageHistory(private val capacity: Int = 8192) : MessageHistory {
    private val entries = linkedMapOf<HistoryKey, JsonNode>()
    override suspend fun find(account: String, message: String): List<HistoryKey> = synchronized(entries) {
        entries.keys.filter { it.account == account && it.message == message }
    }

    override suspend fun get(key: HistoryKey): JsonNode? = synchronized(entries) { entries[key]?.deepCopy() }
    override suspend fun put(key: HistoryKey, message: JsonNode) = synchronized(entries) {
        entries.remove(key)
        entries[key] = message.deepCopy()
        if (entries.size > capacity) entries.remove(entries.keys.first())
        Unit
    }
}
