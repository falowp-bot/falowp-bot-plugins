package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.time.Duration
import kotlin.test.*

class MessageHistoryTest {
    private val key = HistoryKey("9", 2, "42", "500")
    private val withoutDb = object : ClassLoader(javaClass.classLoader) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name.startsWith("com.blr19c.falowp.bot.plugins.db")) throw ClassNotFoundException(name)
            return super.loadClass(name, resolve)
        }
    }

    @Test
    fun autoWithoutOptionalModulePersistsAcrossReopenAndIsolatesAccountsAndPeers(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("qq-history-")
        try {
            val first = HistoryStores.open(HistorySettings(), root, withoutDb)
            first.put(key, j(obj("text" to "原消息😀")))
            first.put(key.copy(account = "10"), j(obj("text" to "另一个账号")))
            first.put(key.copy(peer = "43"), j(obj("text" to "另一个群")))
            val reopened = HistoryStores.open(HistorySettings(), root, withoutDb)
            assertEquals("原消息😀", reopened.get(key)?.s("text"))
            assertEquals("另一个账号", reopened.get(key.copy(account = "10"))?.s("text"))
            assertEquals("另一个群", reopened.get(key.copy(peer = "43"))?.s("text"))
            assertNull(reopened.get(key.copy(chatType = 1)))
            assertEquals(setOf(key, key.copy(peer = "43")), reopened.find("9", "500").toSet())
            assertEquals(listOf(key.copy(account = "10")), reopened.find("10", "500"))
            assertTrue(reopened.find("unknown", "500").isEmpty())
            reopened.put(key, j(obj("text" to "更新")))
            assertEquals("更新", first.get(key)?.s("text"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun disabledPersistenceDoesNotLoadOptionalModuleOrCreateFiles(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("qq-history-off-")
        try {
            val neverLoad = object : ClassLoader() {
                override fun loadClass(name: String): Class<*> = error("Persistence should be disabled")
            }
            val memory = HistoryStores.open(HistorySettings(enabled = false), root, neverLoad)
            memory.put(key, j(obj("text" to "memory")))
            assertNotNull(memory.get(key))
            assertNull(HistoryStores.open(HistorySettings(enabled = false), root, neverLoad).get(key))
            assertEquals(0, Files.list(root).use { it.count() })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun autoUsesActualUtilsDbAndRejectsMissingDataSourceWithoutSQLiteFallback(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("qq-history-db-")
        try {
            val store = HistoryStores.open(HistorySettings(), root)
            store.put(key, j(obj("text" to "x".repeat(100_000))))
            val named = HistoryStores.open(HistorySettings(backend = "db", database = "nativeHistoryTest"), root)
            assertEquals(100_000, named.get(key)?.s("text")?.length)
            assertFails { HistoryStores.open(HistorySettings(backend = "db", database = "missing"), root) }
            assertFalse(Files.exists(root.resolve("messages.sqlite")))
            assertEquals(0, Files.list(root.resolve("history-pending")).use { it.count() })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun sqliteRetentionExpiresAndCleansRowsAndSharedDatabaseStaysUsable(): Unit = runBlocking(Dispatchers.IO) {
        val root = Files.createTempDirectory("qq-history-retention-")
        try {
            val url = "jdbc:sqlite:${root.resolve("history.sqlite")}"
            var time = 1_700_000_000_000L
            val local = Database.connect(url, driver = "org.sqlite.JDBC")
            val history = ExposedMessageHistory(local, 1) { time }
            history.put(key, j(obj("text" to "old")))
            time += Duration.ofDays(2).toMillis()
            assertNull(history.get(key))
            assertTrue(history.find("9", "500").isEmpty())
            history.put(key.copy(message = "new"), j(obj("text" to "new")))
            assertEquals(1L, transaction(local) { MessageHistoryTable.selectAll().count() })
            val database = Database.connect("jdbc:h2:mem:sharedConnector;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
            ExposedMessageHistory(database, 1).put(key, j(obj("text" to "shared")))
            assertEquals(1L, transaction(database) { MessageHistoryTable.selectAll().count() })
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
