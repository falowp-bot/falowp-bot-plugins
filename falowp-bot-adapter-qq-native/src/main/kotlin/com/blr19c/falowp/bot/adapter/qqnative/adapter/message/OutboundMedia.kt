package com.blr19c.falowp.bot.adapter.qqnative.adapter.message

import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.*
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.system.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * 管理待发送的临时媒体文件，待工作进程复制到 NTQQ 媒体目录后再清理
 */
internal class OutboundMedia : AutoCloseable {
    private val temporary = mutableListOf<Path>()
    private fun temp(suffix: String) = Files.createTempFile("falowp-qq-", suffix).also { temporary.add(it) }

    private suspend fun local(uri: URI): Path = withContext(Dispatchers.IO) {
        when (uri.scheme?.lowercase()) {
            null -> Path.of(uri.path).toAbsolutePath()
            "file" -> Path.of(uri)
            "http", "https" -> {
                val suffix = uri.path.substringAfterLast('.', "").takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) }
                val dest = temp(suffix?.let { ".$it" } ?: ".media")
                uri.toURL().openConnection().apply {
                    connectTimeout = 10000
                    readTimeout = 60000
                }
                    .getInputStream().use { input -> Files.newOutputStream(dest).use(input::copyTo) }
                dest
            }

            else -> error("不支持的媒体地址类型：${uri.scheme}")
        }
    }

    suspend fun encode(part: SendMessage): QQNativeOutgoingPart = when (part) {
        is QQNativeFileMessage -> QQNativeOutgoingPart.File(local(part.uri), part.name, part.folderId)
        is QQNativeMarketFaceMessage -> QQNativeOutgoingPart.MarketFace(part.id, part.packageId, part.key, part.name)
        is QQNativeGameMessage -> QQNativeOutgoingPart.Game(part.dice, part.result)
        is QQNativeOriginalMessage -> QQNativeOutgoingPart.Original(part.accountId, part.message.deepCopy())
        is QQNativeCardMessage -> QQNativeOutgoingPart.Ark(part.json.toString())
        MessagePartition -> QQNativeOutgoingPart.Partition
        is TextSendMessage -> QQNativeOutgoingPart.Text(part.content)
        is AtSendMessage -> QQNativeOutgoingPart.Mention(part.at)
        is NudgeSendMessage -> QQNativeOutgoingPart.Nudge(part.id)
        is EmojiSendMessage -> QQNativeOutgoingPart.Emoji(part.id, part.type, part.display)
        is VoiceSendMessage -> QQNativeOutgoingPart.Voice(local(part.voice))
        is VideoSendMessage -> QQNativeOutgoingPart.Video(local(part.video))
        is ImageSendMessage -> withContext(Dispatchers.IO) {
            val image = if (part.image.info.startsWith("file:", ignoreCase = true)) {
                Path.of(URI(part.image.info))
            } else {
                temp(".image").also { part.image.toFile(it.toFile()) }
            }
            ImageIO.createImageInputStream(image.toFile()).use { stream ->
                val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: error("无法读取图片格式")
                try {
                    reader.input = stream
                    val format = reader.formatName.lowercase()
                    QQNativeOutgoingPart.Image(
                        image, ".$format",
                        when (format) {
                            "png" -> 1001
                            "gif" -> 2000
                            "bmp" -> 1005
                            else -> 1000
                        },
                        reader.getWidth(0), reader.getHeight(0)
                    )
                } finally {
                    reader.dispose()
                }
            }
        }

        else -> error("QQNative 不支持扩展消息 ${part.javaClass.name}")
    }

    override fun close() {
        temporary.forEach { Files.deleteIfExists(it) }
    }
}
