package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import java.net.URI
import java.nio.file.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds

/**
 * 准备发送媒体并解析接收媒体，负责缓存、下载和视频缩略图回退
 */
internal class NativeMedia(private val bridge: NativeBridge, private val configuration: JsonNode) {
    private val toolLock = Mutex()
    private var installed = false
    private val downloads = ConcurrentHashMap<String, CompletableDeferred<JsonNode>>()
    private val locks = Array(64) { Mutex() }
    fun complete(info: JsonNode) {
        if (info.i("thumbSize") == 0) downloads["${info.s("msgId")}/${info.s("msgElementId")}"]?.complete(info)
    }

    private suspend fun run(vararg arguments: String): String = withContext(Dispatchers.IO) {
        val output = Files.createTempFile("qq-native-process-", ".log")
        try {
            val process = ProcessBuilder(*arguments).redirectErrorStream(true).redirectOutput(output.toFile()).start()
            try {
                check(process.waitFor(90, TimeUnit.SECONDS)) { "媒体处理超时" }
                val text = Files.newInputStream(output).use { it.readNBytes(2 * 1024 * 1024).toString(Charsets.UTF_8) }
                check(process.exitValue() == 0) { "媒体处理失败：${text.takeLast(1500)}" }
                text
            } finally {
                if (process.isAlive) process.destroyForcibly()
            }
        } finally {
            Files.deleteIfExists(output)
        }
    }

    private suspend fun ffmpeg(vararg args: String): String {
        toolLock.withLock {
            if (!installed) {
                run("sh", configuration.s("installer"), configuration.s("tools"))
                installed = true
            }
        }
        return run(
            Path.of(configuration.s("tools"), "ffmpeg").toString(),
            "-nostdin",
            "-hide_banner",
            "-protocol_whitelist",
            "file,pipe",
            *args
        )
    }

    private suspend fun store(file: Path, kind: Int, extension: String): JsonNode = withContext(Dispatchers.IO) {
        val md5 = hashes(file).md5.hex()
        val filename = md5 + extension
        val target = bridge.msg(
            "getRichMediaFilePathForGuild",
            obj(
                "md5HexStr" to md5,
                "fileName" to filename,
                "elementType" to kind,
                "elementSubType" to 0,
                "thumbSize" to 0,
                "needCreate" to true,
                "downloadType" to 1,
                "file_uuid" to ""
            )
        ).asString("")
        check(target.isNotEmpty()) { "NTQQ 未分配媒体路径" }
        atomicCopy(file, Path.of(target))
        return@withContext j(
            obj(
                "fileName" to filename,
                "filePath" to target,
                "md5HexStr" to md5,
                "fileSize" to Files.size(Path.of(target)).toString()
            )
        )
    }

    suspend fun element(part: QQNativeOutgoingPart): JsonNode = withContext(Dispatchers.IO) {
        val path = when (part) {
            is QQNativeOutgoingPart.File -> part.path
            is QQNativeOutgoingPart.Image -> part.path
            is QQNativeOutgoingPart.Voice -> part.path
            is QQNativeOutgoingPart.Video -> part.path
            else -> error("需要图片、语音或视频")
        }
        require(path.toString().isNotBlank()) { "缺少媒体文件路径" }
        if (part is QQNativeOutgoingPart.File) {
            require(part.name.isNotBlank() && Files.size(path) > 0) { "文件名不能为空，且文件大小必须大于零" }
            val meta = store(path, 3, "." + part.name.substringAfterLast('.', "bin"))
            return@withContext j(
                obj(
                    "elementType" to 3, "elementId" to "", "fileElement" to obj(
                        "fileName" to part.name,
                        "filePath" to meta.s("filePath"),
                        "fileSize" to meta.s("fileSize"),
                        "folderId" to part.folderId
                    )
                )
            )
        }
        if (part is QQNativeOutgoingPart.Image) {
            val meta = store(path, 2, part.extension)
            return@withContext j(
                obj(
                    "elementType" to 2,
                    "elementId" to "",
                    "picElement" to (mapper.convertValue(meta, Map::class.java) + obj(
                        "sourcePath" to meta.s("filePath"),
                        "original" to true,
                        "picType" to part.pictureType,
                        "picSubType" to 0,
                        "picWidth" to part.width,
                        "picHeight" to part.height,
                        "fileUuid" to "",
                        "fileSubId" to "",
                        "thumbFileSize" to 0
                    ))
                )
            )
        }
        val temp = Files.createTempDirectory("falowp-media-")
        try {
            if (part is QQNativeOutgoingPart.Voice) {
                val file = temp.resolve("voice.amr")
                val output = ffmpeg(
                    "-i",
                    path.toString(),
                    "-vn",
                    "-ac",
                    "1",
                    "-ar",
                    "8000",
                    "-c:a",
                    "libopencore_amrnb",
                    "-b:a",
                    "12.2k",
                    file.toString()
                )
                val meta = store(file, 4, ".amr")
                return@withContext j(
                    obj(
                        "elementType" to 4,
                        "elementId" to "",
                        "pttElement" to (mapper.convertValue(meta, Map::class.java) + obj(
                            "duration" to duration(output),
                            "formatType" to 1,
                            "voiceType" to 1,
                            "voiceChangeType" to 0,
                            "canConvert2Text" to true,
                            "waveAmplitudes" to emptyList<Int>(),
                            "fileSubId" to "",
                            "playState" to 1,
                            "autoConvertText" to 0,
                            "storeID" to 0,
                            "otherBusinessInfo" to obj("aiVoiceType" to 0)
                        ))
                    )
                )
            }
            val frame = temp.resolve("cover.png")
            val output = ffmpeg("-i", path.toString(), "-frames:v", "1", "-vf", "scale=320:-2", frame.toString())
            val meta = store(path, 5, path.fileName.toString().substringAfterLast('.', "mp4").let { ".$it" })
            val cover =
                Path.of(meta.s("filePath")).parent.parent.resolve("Thumb").resolve(meta.s("md5HexStr") + "_0.png")
            atomicCopy(frame, cover)
            val image = ImageIO.read(frame.toFile())
            val bytes = Files.readAllBytes(frame)
            val dims = Regex("Video:.*? (\\d+)x(\\d+)[, ]").find(output)
            return@withContext j(
                obj(
                    "elementType" to 5,
                    "elementId" to "",
                    "videoElement" to obj(
                        "fileName" to meta.s("fileName"),
                        "filePath" to meta.s("filePath"),
                        "fileSize" to meta.s("fileSize"),
                        "videoMd5" to meta.s("md5HexStr"),
                        "fileTime" to duration(output),
                        "fileWidth" to (dims?.groupValues?.get(1)?.toInt() ?: image.width),
                        "fileHeight" to (dims?.groupValues?.get(2)?.toInt() ?: image.height),
                        "thumbMd5" to digest("MD5", bytes).hex(),
                        "thumbSize" to bytes.size,
                        "thumbWidth" to image.width,
                        "thumbHeight" to image.height,
                        "thumbPath" to nativeMap(0 to cover.toString())
                    )
                )
            )
        } finally {
            temp.toFile().deleteRecursively()
        }
    }

    suspend fun read(message: JsonNode, element: JsonNode, data: JsonNode): String = withContext(Dispatchers.IO) {
        val root = Path.of(configuration.s("accountPath")).toRealPath()
        fun local(candidate: String): Path? = runCatching {
            Path.of(candidate).toRealPath()
                .takeIf { it.startsWith(root) && Files.isRegularFile(it) && Files.size(it) > 0 }
        }.getOrNull()
        local(data.s("sourcePath").ifEmpty { data.s("filePath") })?.let { return@withContext it.toUri().toString() }
        val key = "${message.s("msgId")}/${element.s("elementId")}"
        locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            val hash = digest("SHA-256", "${message.i("chatType")}/${message.s("peerUid")}/$key".toByteArray()).hex()
            val file = root.resolve("falowp-downloads").resolve(hash)
            local(file.toString())?.let { return@withLock it.toUri().toString() }
            Files.createDirectories(file.parent)
            val temp = Path.of("$file.${UUID.randomUUID()}")
            val completed = CompletableDeferred<JsonNode>()
            downloads[key] = completed
            try {
                val reply = bridge.msg(
                    "downloadRichMedia",
                    obj(
                        "fileModelId" to "0",
                        "downSourceType" to 0,
                        "downloadSourceType" to 0,
                        "triggerType" to 1,
                        "msgId" to message.s("msgId"),
                        "chatType" to message.i("chatType"),
                        "peerUid" to message.s("peerUid"),
                        "elementId" to element.s("elementId"),
                        "thumbSize" to 0,
                        "downloadType" to 1,
                        "filePath" to temp.toString()
                    )
                )
                check(!reply.hasValue("result") || reply.i("result") == 0) { "NTQQ 媒体下载失败" }
                val event = withTimeout(90_000.milliseconds) { completed.await() }
                check(event.i("fileErrCode") == 0) { "NTQQ 媒体下载未完成" }
                val actual = checkNotNull(
                    local(
                        event.s("filePath").ifEmpty { temp.toString() })
                ) { "NTQQ 未返回可读取的媒体文件" }
                if (actual != temp) Files.copy(actual, temp, StandardCopyOption.REPLACE_EXISTING)
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
                file.toUri().toString()
            } finally {
                downloads.remove(key, completed)
                Files.deleteIfExists(temp)
            }
        }
    }

    /**
     * 缩略图缺失或损坏时返回空值，保留仍可读取的视频
     */
    suspend fun videoThumbnail(data: JsonNode, videoUri: String): String? = try {
        withContext(Dispatchers.IO) {
            val root = Path.of(configuration.s("accountPath")).toRealPath()
            fun local(path: Path): Path? = runCatching {
                path.toRealPath().takeIf { it.startsWith(root) && Files.isRegularFile(it) && Files.size(it) > 0 }
            }.getOrNull()

            fun image(path: Path): Path? = local(path)?.takeIf {
                runCatching {
                    if (Files.size(it) > 16 * 1024 * 1024) return@runCatching false
                    ImageIO.createImageInputStream(it.toFile()).use { input ->
                        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return@use false
                        try {
                            reader.input = input
                            reader.getWidth(0) > 0 && reader.getHeight(0) > 0
                        } finally {
                            reader.dispose()
                        }
                    }
                }.getOrDefault(false)
            }
            for ((_, value) in data.path("thumbPath").mapEntries()
                .sortedBy { it.first.toIntOrNull() ?: Int.MAX_VALUE }) {
                val path = runCatching { Path.of(value.asString()) }.getOrNull() ?: continue
                image(path)?.let { return@withContext it.toUri().toString() }
            }
            val uri = URI(videoUri)
            if (uri.scheme != "file") return@withContext null
            val video = local(Path.of(uri)) ?: return@withContext null
            val identity = "thumbnail/$video/${Files.size(video)}/${Files.getLastModifiedTime(video).toMillis()}"
            val key = digest("SHA-256", identity.toByteArray()).hex()
            locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
                val target = root.resolve("falowp-thumbnails").resolve("$key.png")
                image(target)?.let { return@withLock it.toUri().toString() }
                Files.createDirectories(target.parent)
                val temporary = Files.createTempFile(target.parent, ".frame-", ".png")
                try {
                    ffmpeg("-y", "-i", video.toString(), "-frames:v", "1", "-vf", "scale=320:-2", temporary.toString())
                    checkNotNull(image(temporary)) { "视频封面生成结果无效" }
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    target.toUri().toString()
                } finally {
                    Files.deleteIfExists(temporary)
                }
            }
        }
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        LoggerFactory.getLogger(javaClass).warn("QQ 视频缩略图不可用，保留视频正文：{}", error.javaClass.simpleName)
        null
    }

    companion object {
        fun duration(output: String): Int {
            val match = checkNotNull(Regex("Duration:\\s*(\\d+):(\\d+):([\\d.]+)").find(output)) { "无法读取媒体时长" }
            return round(match.groupValues[1].toDouble() * 3600 + match.groupValues[2].toDouble() * 60 + match.groupValues[3].toDouble()).toInt()
                .coerceAtLeast(1)
        }

        private fun atomicCopy(source: Path, target: Path) {
            Files.createDirectories(target.parent)
            val temp = Path.of("$target.${UUID.randomUUID()}")
            try {
                Files.copy(source, temp)
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(temp)
            }
        }
    }
}
