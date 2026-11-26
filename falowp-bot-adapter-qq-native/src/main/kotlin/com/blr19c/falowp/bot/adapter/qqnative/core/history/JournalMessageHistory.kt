package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.StandardOpenOption.*

/**
 * 先将记录提交到本地日志，再写入数据库；数据库故障时保留记录，并在启动或后续写入时重放
 */
internal class JournalMessageHistory(
    private val delegate: MessageHistory,
    private val directory: Path,
    private val retentionDays: Int,
    private val now: () -> Long = System::currentTimeMillis
) : MessageHistory {
    private val lock = Mutex()
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        Files.createDirectories(directory)
    }

    override suspend fun put(key: HistoryKey, message: JsonNode) = withContext(Dispatchers.IO) {
        lock.withLock {
            val entry = j(
                obj(
                    "account" to key.account, "chatType" to key.chatType, "peer" to key.peer,
                    "message" to key.message, "savedAt" to now(), "payload" to message
                )
            )
            val temporary = Files.createTempFile(directory, ".pending-", ".tmp")
            try {
                FileChannel.open(temporary, WRITE).use { channel ->
                    val bytes = ByteBuffer.wrap(mapper.writeValueAsBytes(entry))
                    while (bytes.hasRemaining()) channel.write(bytes)
                    channel.force(true)
                }
                Files.move(temporary, file(key), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                FileChannel.open(directory, READ).use { it.force(true) }
            } finally {
                Files.deleteIfExists(temporary)
            }
            flushLocked()
        }
    }

    override suspend fun get(key: HistoryKey): JsonNode? = withContext(Dispatchers.IO) {
        lock.withLock {
            // 数据库不可用期间，本地日志可能保存了更新的记录
            val pending = file(key).takeIf(Files::exists)?.let { read(it) }?.path("payload")
            if (pending != null) return@withLock pending
            delegate.get(key)
        }
    }

    override suspend fun find(account: String, message: String): List<HistoryKey> = withContext(Dispatchers.IO) {
        lock.withLock {
            val pending = Files.newDirectoryStream(directory, "*.json").use { files ->
                files.mapNotNull { path ->
                    val entry = read(path) ?: return@mapNotNull null
                    if (entry.s("account") == account && entry.s("message") == message)
                        HistoryKey(account, entry.i("chatType"), entry.s("peer"), message) else null
                }
            }
            val stored = try {
                delegate.find(account, message)
            } catch (error: Exception) {
                if (error is CancellationException || pending.isEmpty()) throw error
                log.warn("QQNative 数据库查询失败，使用本地待重放记录定位消息", error)
                emptyList()
            }
            (pending + stored).distinct()
        }
    }

    suspend fun replay() = withContext(Dispatchers.IO) { lock.withLock { flushLocked() } }

    private fun file(key: HistoryKey) = directory.resolve(key.hash + ".json")
    private fun read(path: Path): JsonNode? {
        val entry = Files.newInputStream(path).use { mapper.readTree(it) }
        if (entry.l("savedAt") < now() - retentionDays * 86_400_000L) {
            Files.deleteIfExists(path)
            return null
        }
        return entry
    }

    private suspend fun flushLocked() = withContext(Dispatchers.IO) {
        Files.newDirectoryStream(directory, "*.json").use { files ->
            for (path in files) {
                val entry = read(path) ?: continue
                val key = HistoryKey(entry.s("account"), entry.i("chatType"), entry.s("peer"), entry.s("message"))
                try {
                    delegate.put(key, entry.path("payload"))
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    log.warn("QQNative 数据库写入失败，原文已保存在本地待重放记录中", error)
                    break
                }
                Files.delete(path)
            }
        }
    }
}
