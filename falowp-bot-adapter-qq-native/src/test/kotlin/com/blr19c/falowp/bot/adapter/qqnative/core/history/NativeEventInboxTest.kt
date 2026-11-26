package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.*
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.QQNativeNotice
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import tools.jackson.databind.JsonNode
import java.nio.file.Files
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class NativeEventInboxTest {
    private fun event(id: String) = j(
        obj(
            "name" to "onRecvMsg", "arguments" to listOf(
                listOf(
                    obj(
                        "chatType" to 2,
                        "peerUid" to "42",
                        "msgId" to id,
                        "senderUin" to "11",
                        "senderUid" to "u_11",
                        "roleType" to 2,
                        "elements" to listOf(obj("textElement" to obj("content" to id)))
                    )
                )
            )
        )
    )

    @Test
    fun dedicatedNoticeIsPersistedBeforeStartAndReplayed(): Unit = runBlocking(Dispatchers.IO) {
        val directory = Files.createTempDirectory("qq-notice-inbox-")
        val account = j(obj("id" to "9", "uid" to "u_9"))
        val notices = Channel<QQNativeNotice>(Channel.UNLIMITED)
        val first = NativeRuntime(
            { _, _ -> error("No native calls expected before start") }, account,
            {}, { error("Not started") }, inbox = NativeEventInbox(directory), startImmediately = false
        )
        try {
            first.event(
                j(
                    obj(
                        "name" to "onInputStatusPush",
                        "arguments" to listOf(obj("fromUin" to "11", "eventType" to 1, "statusText" to "typing"))
                    )
                )
            )
            assertEquals(1, NativeEventInbox(directory).pending().size)
            first.close()
            NativeRuntime(
                { _, _ -> error("Profile unavailable") }, account, {}, { notices.trySend(it) },
                inbox = NativeEventInbox(directory)
            ).use {
                assertEquals("11", withTimeout(3000.milliseconds) { notices.receive() }.actor)
                withTimeout(3000.milliseconds) {
                    while (NativeEventInbox(directory).pending().isNotEmpty()) delay(10.milliseconds)
                }
            }
        } finally {
            first.close()
            notices.close()
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun queuedCallbacksSurviveCancellationWhileDatabaseConsumerIsBlocked(): Unit = runBlocking(Dispatchers.IO) {
        val directory = Files.createTempDirectory("qq-inbox-")
        val entered = CompletableDeferred<Unit>()
        val blocked = object : MessageHistory {
            override suspend fun find(account: String, message: String): List<HistoryKey> = emptyList()
            override suspend fun get(key: HistoryKey): JsonNode? = null
            override suspend fun put(key: HistoryKey, message: JsonNode) {
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        val account = j(obj("id" to "9", "uid" to "u_9"))
        val first = NativeRuntime(
            { _, _ -> error("No native calls expected") },
            account,
            { error("Consumer is blocked") },
            { error("Unexpected notice") },
            blocked,
            NativeEventInbox(directory)
        )
        try {
            first.event(event("first"))
            withTimeout(3000.milliseconds) { entered.await() }
            first.event(event("second"))
            assertEquals(
                2,
                NativeEventInbox(directory).pending().size,
                "Both callbacks must be durable before event() returns"
            )
            first.close()
            val emitted = Channel<String>(Channel.UNLIMITED)
            val replayed = NativeRuntime(
                { _, _ -> error("No native calls expected") },
                account,
                { emitted.trySend(it.receipt.id) },
                { error("Unexpected notice") },
                MemoryMessageHistory(),
                NativeEventInbox(directory)
            )
            try {
                assertEquals(
                    listOf("first", "second"),
                    withTimeout(3000.milliseconds) { listOf(emitted.receive(), emitted.receive()) })
                withTimeout(3000.milliseconds) {
                    while (NativeEventInbox(directory).pending().isNotEmpty()) delay(10.milliseconds)
                }
            } finally {
                replayed.close()
                emitted.close()
            }
        } finally {
            first.close()
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun lateAcknowledgementCannotDeleteNewEntriesAfterInboxReopens() {
        val directory = Files.createTempDirectory("qq-inbox-ack-")
        try {
            val old = NativeEventInbox(directory)
            val first = old.append(event("first"))
            old.acknowledge(first)
            val reopened = NativeEventInbox(directory)
            val second = reopened.append(event("second"))
            old.acknowledge(first)
            assertEquals(listOf(second.event), reopened.pending().map { it.event })
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun memoryModeDoesNotRetainEntriesAcrossReopen() {
        NativeEventInbox().append(event("memory"))
        assertTrue(NativeEventInbox().pending().isEmpty())
    }
}
