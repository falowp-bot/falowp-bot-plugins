package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

/**
 * 将远程输入缓存为本地文件，原生操作或相册上传完成后再清理
 */
internal suspend fun <T> withExtensionFile(input: String, action: suspend (Path) -> T): T =
    withContext(Dispatchers.IO) {
        require(input.isNotBlank()) { "缺少文件地址" }
        if (!input.contains("://") && !input.startsWith("file:")) return@withContext action(Path.of(input).toRealPath())
        if (input.startsWith("file:")) return@withContext action(Path.of(URI(input)).toRealPath())
        val temp = Files.createTempFile("qq-extension-", ".media")
        try {
            if (input.startsWith("base64://")) Files.write(
                temp,
                Base64.getDecoder().decode(input.removePrefix("base64://"))
            )
            else {
                val uri = URI(input)
                require(uri.scheme in listOf("http", "https")) { "不支持的文件协议" }
                uri.toURL().openConnection().apply {
                    connectTimeout = 15_000
                    readTimeout = 60_000
                }.getInputStream().use { source -> Files.newOutputStream(temp).use { source.copyTo(it) } }
            }
            action(temp)
        } finally {
            Files.deleteIfExists(temp)
        }
    }
