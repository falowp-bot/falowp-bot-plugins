package com.blr19c.falowp.bot.adapter.qq.core.state

/**
 * 有界 TTL 状态，用于入站去重及跨 BotApi 实例分配回复序号
 */
internal class QQState(private val clock: () -> Long = System::currentTimeMillis) {
    private data class Entry(val expires: Long, var sequence: Int = 0)

    private val entries = LinkedHashMap<String, Entry>()

    /**
     * 清理过期条目并原子分配指定键的下一个序号
     */
    @Synchronized
    fun next(key: String): Int {
        val now = clock()
        entries.entries.removeIf { it.value.expires <= now }
        val entry = entries.getOrPut(key) { Entry(now + 65 * 60 * 1000L) }
        while (entries.size > 10000) entries.remove(entries.keys.first())
        return ++entry.sequence
    }

    /**
     * 移除指定键，使分发失败的消息可以重新进入处理流程
     */
    @Synchronized
    fun forget(key: String) {
        entries.remove(key)
    }
}
