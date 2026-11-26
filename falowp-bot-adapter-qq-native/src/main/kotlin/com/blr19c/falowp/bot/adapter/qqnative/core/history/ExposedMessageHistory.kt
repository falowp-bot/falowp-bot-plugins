package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.mapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import tools.jackson.databind.JsonNode
import java.time.Duration

/**
 * 消息原文表，由适配器在启动时创建
 */
@Suppress("ExposedReference")
internal object MessageHistoryTable : Table("qq-native-message") {
    val keyHash = varchar("key_hash", 64)
    val account = varchar("account", 64)
    val message = varchar("message", 64)
    val chatType = integer("chat_type")
    val peer = varchar("peer", 128)
    val savedAt = long("saved_at")
    val payload = registerColumn("payload", MessagePayloadColumnType())
    override val primaryKey = PrimaryKey(keyHash)

    init {
        index(false, account, message)
    }
}

/**
 * MySQL 使用长二进制字段，避免原文超过 64 KiB 时写入失败
 */
private class MessagePayloadColumnType(
    private val delegate: BlobColumnType = BlobColumnType()
) : IColumnType<ExposedBlob> by delegate {
    override fun sqlType(): String =
        if (currentDialect is MysqlDialect) "LONGBLOB" else delegate.sqlType()
}

/**
 * 使用 Exposed 保存消息原文，并定期清理过期记录
 */
internal class ExposedMessageHistory(
    private val database: Database,
    retentionDays: Int,
    private val now: () -> Long = System::currentTimeMillis
) : MessageHistory {
    private val retentionMillis = Duration.ofDays(retentionDays.toLong()).toMillis()
    private val lock = Mutex()
    private var nextCleanup = 0L

    init {
        require(retentionDays > 0)
        transaction(database) { SchemaUtils.create(MessageHistoryTable) }
    }

    override suspend fun find(account: String, message: String): List<HistoryKey> = withContext(Dispatchers.IO) {
        lock.withLock {
            suspendTransaction(database) {
                MessageHistoryTable.selectAll().where {
                    (MessageHistoryTable.account eq account) and (MessageHistoryTable.message eq message) and
                            (MessageHistoryTable.savedAt greaterEq now() - retentionMillis)
                }.map { HistoryKey(account, it[MessageHistoryTable.chatType], it[MessageHistoryTable.peer], message) }
            }
        }
    }

    override suspend fun get(key: HistoryKey): JsonNode? = withContext(Dispatchers.IO) {
        lock.withLock {
            suspendTransaction(database) {
                MessageHistoryTable.selectAll().where {
                    (MessageHistoryTable.keyHash eq key.hash) and
                            (MessageHistoryTable.savedAt greaterEq now() - retentionMillis)
                }.singleOrNull()?.let { mapper.readTree(it[MessageHistoryTable.payload].bytes) }
            }
        }
    }

    override suspend fun put(key: HistoryKey, message: JsonNode): Unit = withContext(Dispatchers.IO) {
        lock.withLock {
            val time = now()
            val cleanup = time >= nextCleanup
            suspendTransaction(database) {
                MessageHistoryTable.upsert {
                    it[keyHash] = key.hash
                    it[account] = key.account
                    it[MessageHistoryTable.message] = key.message
                    it[chatType] = key.chatType
                    it[peer] = key.peer
                    it[savedAt] = time
                    it[payload] = ExposedBlob(mapper.writeValueAsBytes(message))
                }
                if (cleanup) {
                    MessageHistoryTable.deleteWhere { savedAt less time - retentionMillis }
                }
            }
            if (cleanup) nextCleanup = time + Duration.ofHours(1).toMillis()
        }
    }
}
