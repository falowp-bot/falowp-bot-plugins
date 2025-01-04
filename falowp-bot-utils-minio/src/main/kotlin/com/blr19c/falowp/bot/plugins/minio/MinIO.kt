package com.blr19c.falowp.bot.plugins.minio

import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.expand.registerImageUrlToUrlFun
import com.blr19c.falowp.bot.system.plugin.PluginUtils
import com.blr19c.falowp.bot.system.pluginConfigProperty
import io.ktor.http.*
import io.minio.MinioClient
import io.minio.PutObjectArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.*

@PluginUtils
object MinIO {

    init {
        if (pluginConfigProperty("registerImageUrl") == "true") {
            registerImageUrlToUrlFun { uploadImage(it, "/temporary-image-url/${UUID.randomUUID()}.png") }
        }
    }

    private val minioClient by lazy {
        MinioClient.builder()
            .endpoint(pluginConfigProperty("url"))
            .credentials(pluginConfigProperty("accessKey"), pluginConfigProperty("secretKey"))
            .build()
    }

    /**
     * 上传照片并获取url
     */
    suspend fun uploadImage(imageUrl: ImageUrl, path: String): String {
        return nativeUpload(ByteArrayInputStream(imageUrl.toBytes()), path, "image/png")
    }

    /**
     * 上传文件并获取url
     */
    suspend fun uploadFile(file: File, path: String): String {
        return nativeUpload(file.inputStream(), path, withContext(Dispatchers.IO) {
            try {
                Files.probeContentType(file.toPath())
            } catch (e: Exception) {
                null
            }
        })
    }

    /**
     * 上传网络文件并获取url
     */
    suspend fun uploadFile(inputStream: InputStream, path: String, contentType: String?): String {
        return inputStream.use { nativeUpload(it, path, contentType) }
    }

    private suspend fun nativeUpload(data: InputStream, path: String, contentType: String?): String {
        withContext(Dispatchers.IO) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(pluginConfigProperty("bucket"))
                    .`object`(path)
                    .stream(data, data.available().toLong(), -1)
                    .contentType(contentType ?: ContentType.Application.OctetStream.toString())
                    .build()
            )
        }
        return pluginConfigProperty("url")
            .plus("/")
            .plus(pluginConfigProperty("bucket"))
            .plus("/")
            .plus(path)
            .encodeURLPath()
    }
}
