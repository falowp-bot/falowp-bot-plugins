package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.system.adapterConfigProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.nio.file.Files
import java.nio.file.Path

internal data class HistorySettings(
    val enabled: Boolean = true,
    val backend: String = "auto",
    val database: String = "",
    val sqlitePath: String = "messages.sqlite",
    val retentionDays: Int = 30
) {
    init {
        require(backend in setOf("auto", "db", "sqlite", "memory"))
        require(retentionDays > 0)
    }

    companion object {
        fun configured(): HistorySettings {
            fun value(name: String, default: String) = adapterConfigProperty("qqNative.history.$name") { default }
            return HistorySettings(
                value("enabled", "true").toBooleanStrict(), value("backend", "auto"),
                value("database", ""), value("sqlitePath", "messages.sqlite"), value("retentionDays", "30").toInt()
            )
        }
    }
}

/**
 * 根据配置选择共享数据库或本地存储，并按需包装持久化补偿日志
 */
internal object HistoryStores {
    private const val DB_CLASS = "com.blr19c.falowp.bot.plugins.db.Database"
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun open(
        settings: HistorySettings,
        root: Path,
        loader: ClassLoader = javaClass.classLoader
    ): MessageHistory = withContext(Dispatchers.IO) {
        if (!settings.enabled || settings.backend == "memory") return@withContext MemoryMessageHistory()
        val dbPresent = try {
            Class.forName(DB_CLASS, false, loader)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
        val backend = if (settings.backend == "auto") if (dbPresent) "db" else "sqlite" else settings.backend
        val database = when (backend) {
            "db" -> {
                check(dbPresent) { "history.backend=db 需要引入 falowp-bot-utils-db" }
                SharedHistoryDatabase.resolve(settings.database)
            }

            "sqlite" -> {
                val configured = Path.of(settings.sqlitePath)
                val path =
                    (if (configured.isAbsolute) configured else root.resolve(configured)).toAbsolutePath().normalize()
                Files.createDirectories(path.parent)
                val config = SQLiteConfig().apply {
                    busyTimeout = 5000
                    setJournalMode(SQLiteConfig.JournalMode.WAL)
                }
                val source = SQLiteDataSource(config).apply { url = "jdbc:sqlite:$path" }
                Database.connect(source)
            }

            else -> error("未知消息记录存储：$backend")
        }
        val store = JournalMessageHistory(
            ExposedMessageHistory(database, settings.retentionDays),
            root.resolve("history-pending"), settings.retentionDays
        )
        store.replay()
        log.info("QQNative 消息记录：{}，保留 {} 天", backend, settings.retentionDays)
        store
    }

}
