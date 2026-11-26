package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.mapper
import tools.jackson.databind.JsonNode
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.StandardOpenOption.*
import java.util.UUID

/**
 * 在原生回调读取线程中同步提交本地日志，不等待 JDBC 或原生 RPC
 */
internal class NativeEventInbox(private val directory: Path? = null) {
    data class Entry(val path: Path?, val event: JsonNode)

    private var sequence = 0L

    init {
        directory?.let {
            Files.createDirectories(it)
            sequence = paths().maxOfOrNull { path -> path.fileName.toString().substringBefore("-").toLong() } ?: 0L
        }
    }

    private fun paths(): List<Path> = directory?.let {
        Files.newDirectoryStream(it, "*.json").use { paths -> paths.toList().sortedBy { p -> p.fileName.toString() } }
    } ?: emptyList()

    @Synchronized
    fun pending(): List<Entry> = paths().map { path ->
        Entry(path, Files.newInputStream(path).use { mapper.readTree(it) })
    }

    @Synchronized
    fun append(event: JsonNode): Entry {
        val copy = event.deepCopy()
        val dir = directory ?: return Entry(null, copy)
        val destination = dir.resolve("%020d-%s.json".format(++sequence, UUID.randomUUID()))
        val temporary = Files.createTempFile(dir, ".event-", ".tmp")
        try {
            FileChannel.open(temporary, WRITE).use { channel ->
                val buffer = ByteBuffer.wrap(mapper.writeValueAsBytes(copy))
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE)
            syncDirectory()
        } finally {
            Files.deleteIfExists(temporary)
        }
        return Entry(destination, copy)
    }

    @Synchronized
    fun acknowledge(entry: Entry) {
        entry.path?.let {
            Files.deleteIfExists(it)
            syncDirectory()
        }
    }

    private fun syncDirectory() {
        directory?.let { FileChannel.open(it, READ).use { channel -> channel.force(true) } }
    }
}
