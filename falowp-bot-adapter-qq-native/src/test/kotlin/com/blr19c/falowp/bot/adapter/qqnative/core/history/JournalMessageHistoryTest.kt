package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import tools.jackson.databind.JsonNode
import java.nio.file.Files
import kotlin.test.*

class JournalMessageHistoryTest {
    private val key = HistoryKey("9", 2, "42", "500")

    @Test
    fun failedDatabaseWritesSurviveRestartAndReplayTheLatestRawAndDecodedPayload(): Unit = runBlocking(Dispatchers.IO) {
        val directory = Files.createTempDirectory("qq-pending-")
        val stored = MemoryMessageHistory()
        var offline = true
        val database = object : MessageHistory {
            override suspend fun find(account: String, message: String): List<HistoryKey> {
                check(!offline) { "simulated database outage" }
                return stored.find(account, message)
            }

            override suspend fun get(key: HistoryKey) = stored.get(key)
            override suspend fun put(key: HistoryKey, message: JsonNode) {
                check(!offline) { "simulated database outage" }
                stored.put(key, message)
            }
        }
        try {
            val first = JournalMessageHistory(database, directory, 30)
            first.put(key, j(obj("raw" to "original")))
            first.put(key, j(obj("raw" to "original", "decoded" to "text")))
            assertNull(stored.get(key))
            val restarted = JournalMessageHistory(database, directory, 30)
            assertEquals("original", restarted.get(key)?.s("raw"))
            assertEquals("text", restarted.get(key)?.s("decoded"))
            assertEquals(listOf(key), restarted.find("9", "500"))
            offline = false
            restarted.replay()
            assertEquals("text", stored.get(key)?.s("decoded"))
            assertEquals(listOf(key), restarted.find("9", "500"))
            assertEquals(0, Files.list(directory).use { it.count() })
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun expiredPendingRecordsAreNotReplayed(): Unit = runBlocking(Dispatchers.IO) {
        val directory = Files.createTempDirectory("qq-pending-expired-")
        val failed = object : MessageHistory {
            override suspend fun find(account: String, message: String): List<HistoryKey> = emptyList()
            override suspend fun get(key: HistoryKey): JsonNode? = null
            override suspend fun put(key: HistoryKey, message: JsonNode): Unit = error("offline")
        }
        try {
            JournalMessageHistory(failed, directory, 1) { 100_000_000L }.put(key, j(obj("text" to "old")))
            val database = MemoryMessageHistory()
            JournalMessageHistory(database, directory, 1) { 300_000_000L }.replay()
            assertNull(database.get(key))
            assertEquals(0, Files.list(directory).use { it.count() })
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
