package com.blr19c.falowp.bot.plugins.minio

import com.blr19c.falowp.bot.system.expand.registerImageUrlToUrlFun
import com.blr19c.falowp.bot.system.plugin.PluginUtils
import com.blr19c.falowp.bot.system.pluginConfigProperty
import io.ktor.http.*
import io.minio.MinioClient
import io.minio.PutObjectArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.*

@PluginUtils
object MinIO {

    init {
        if (pluginConfigProperty("registerImageUrl") == "true") {
            registerImageUrlToUrlFun { tempImageUrl(it) }
        }
    }

    private val minioClient by lazy {
        MinioClient.builder()
            .endpoint(pluginConfigProperty("url"))
            .credentials(pluginConfigProperty("accessKey"), pluginConfigProperty("secretKey"))
            .build()
    }

    /**
     * 上传临时照片并获取url
     */
    suspend fun tempImageUrl(byteArray: ByteArray): String {
        val name = UUID.randomUUID().toString() + ".png"
        withContext(Dispatchers.IO) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(pluginConfigProperty("tempBucket"))
                    .`object`(name)
                    .stream(ByteArrayInputStream(byteArray), byteArray.size.toLong(), -1)
                    .contentType("image/png")
                    .build()
            )
        }
        return pluginConfigProperty("url")
            .plus("/")
            .plus(pluginConfigProperty("tempBucket"))
            .plus("/")
            .plus(name)
            .encodeURLPath()
    }

    /**
     * 上传永久照片并获取url
     */
    suspend fun permanentImageUrl(byteArray: ByteArray, path: String = ""): String {
        val name = "$path/${UUID.randomUUID()}.png"
        withContext(Dispatchers.IO) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(pluginConfigProperty("permanentBucket"))
                    .`object`(name)
                    .stream(ByteArrayInputStream(byteArray), byteArray.size.toLong(), -1)
                    .contentType("image/png")
                    .build()
            )
        }
        return pluginConfigProperty("url")
            .plus("/")
            .plus(pluginConfigProperty("permanentBucket"))
            .plus("/")
            .plus(name)
            .encodeURLPath()
    }
}
