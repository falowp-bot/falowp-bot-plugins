package com.blr19c.falowp.bot.adapter.qq.core.media

import com.blr19c.falowp.bot.adapter.qq.core.client.QQClient
import com.blr19c.falowp.bot.system.json.safeString
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import tools.jackson.databind.JsonNode
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.time.Duration.Companion.seconds

/**
 * 按平台配置上传文件分片，完成确认后交由调用方合并
 */
internal class QQChunkUploader(private val client: QQClient) {
    suspend fun upload(path: Path, target: String, type: Int): String = withContext(Dispatchers.IO) {
        val size = Files.size(path)
        require(size in 1..200L * 1024 * 1024) { "文件大小必须在 1 字节至 200 MiB 之间" }
        val prepared = client.request("$target/upload_prepare", metadata(path, size, type))
        val id = prepared.path("upload_id").safeString()
        check(id.isNotBlank()) { "预上传响应缺少 upload_id" }
        val parts = readParts(prepared, size)
        val config = prepared.path("upload_config")
        val limit = Semaphore(config.path("concurrency").asInt(1).coerceIn(1, 4))
        coroutineScope {
            parts.map { part ->
                async {
                    limit.withPermit { uploadPart(path, target, id, part, config) }
                }
            }.awaitAll()
        }
        id
    }

    private fun metadata(path: Path, size: Long, type: Int): Map<String, Any> {
        val md5 = MessageDigest.getInstance("MD5")
        val sha1 = MessageDigest.getInstance("SHA-1")
        val prefix = MessageDigest.getInstance("MD5")
        var total = 0L
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                md5.update(buffer, 0, count)
                sha1.update(buffer, 0, count)
                val prefixCount = minOf(count.toLong(), (10002432L - total).coerceAtLeast(0)).toInt()
                prefix.update(buffer, 0, prefixCount)
                total += count
            }
        }
        check(total == size) { "上传期间文件大小发生变化" }
        return mapOf(
            "file_type" to type, "file_size" to size.toString(), "file_name" to path.fileName.toString(),
            "md5" to hex(md5.digest()), "sha1" to hex(sha1.digest()), "md5_10m" to hex(prefix.digest())
        )
    }

    private fun readParts(prepared: JsonNode, size: Long): List<Part> {
        val nodes = prepared.path("parts")
        check(nodes.isArray) { "预上传响应缺少 parts" }
        // 空列表表示平台已命中文件，可直接合并
        if (nodes.isEmpty) return emptyList()
        val defaultSize = prepared.path("block_size").safeString().toLongOrNull() ?: 0
        var offset = 0L
        val parts = nodes.sortedBy { it.path("index").asInt(-1) }.mapIndexed { index, node ->
            check(node.path("index").asInt(-1) == index) { "分片序号不连续" }
            val block = node.path("block_size").safeString().toLongOrNull() ?: defaultSize
            check(block > 0 && offset < size) { "分片大小无效" }
            val length = minOf(block, size - offset)
            check(length <= 200L * 1024 * 1024) { "分片过大" }
            val url = node.path("presigned_url").safeString()
            val uri = java.net.URI(url)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) { "分片地址必须为 HTTPS" }
            Part(index, offset, length.toInt(), url).also { offset += length }
        }
        check(offset == size) { "分片未覆盖整个文件" }
        return parts
    }

    private suspend fun uploadPart(path: Path, target: String, id: String, part: Part, config: JsonNode) {
        val bytes = withContext(Dispatchers.IO) {
            RandomAccessFile(path.toFile(), "r").use { file ->
                file.seek(part.offset)
                ByteArray(part.size).also { file.readFully(it) }
            }
        }
        putWithRetry(part.url, bytes, config)
        client.request(
            "$target/upload_part_finish", mapOf(
                "upload_id" to id, "part_index" to part.index,
                "block_size" to bytes.size.toString(), "md5" to hex(MessageDigest.getInstance("MD5").digest(bytes))
            )
        )
    }

    private suspend fun putWithRetry(url: String, bytes: ByteArray, config: JsonNode) {
        val timeout = config.path("retry_timeout").asLong(300).coerceIn(1, 300).seconds
        val retryDelay = config.path("retry_delay").asLong(1).coerceIn(1, 60).seconds
        withTimeout(timeout) {
            while (true) {
                try {
                    client.uploadPart(url, bytes)
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    delay(retryDelay)
                }
            }
        }
    }

    private fun hex(bytes: ByteArray): String = HexFormat.of().formatHex(bytes)

    private data class Part(val index: Int, val offset: Long, val size: Int, val url: String)
}
