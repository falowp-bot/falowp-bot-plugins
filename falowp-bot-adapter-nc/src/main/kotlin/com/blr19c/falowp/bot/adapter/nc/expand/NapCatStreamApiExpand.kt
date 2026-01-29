@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * NapCatStreamApiExpand 流式API
 */
class NapCatStreamApiExpand {
    /**
     * FileStream
     */
    data class FileStream(
        /**
         * 文件路径或 URL
         */
        val file: String,

        /**
         * 文件 ID
         */
        val fileId: String,

        /**
         * 分块大小（字节）
         */
        val chunkSize: Long
    )

    /**
     * 文件
     */
    data class File(
        /**
         * 路径
         */
        @field:JsonProperty("file")
        val file: String
    )
}

/**
 * 下载文件流
 *
 * 以流式方式从网络或本地下载文件
 *
 * @param file 文件路径或URL
 * @param fileId 文件ID
 * @param chunkSize 分块大小
 */
suspend fun NapCatBotApi.downloadFileStream(
    file: String? = null,
    fileId: String? = null,
    chunkSize: Long? = null
): NapCatStreamApiExpand.FileStream {
    return apiRequest("download_file_stream", mapOf("file" to file, "file_id" to fileId, "chunk_size" to chunkSize))
}

/**
 * 上传文件流
 *
 * 以流式方式上传文件数据到机器人
 *
 * @param streamId 流ID
 * @param chunkData 分块数据
 * @param chunkIndex 分块索引
 * @param totalChunks 分块总数
 * @param fileSize 文件大小
 * @param expectedSha256 期望的SHA256
 * @param isComplete 是否完成
 * @param filename 文件名
 * @param reset 是否重置
 * @param verifyOnly 是否仅校验
 * @param fileRetention 文件保留时长
 */
suspend fun NapCatBotApi.uploadFileStream(
    streamId: String,
    chunkData: String? = null,
    chunkIndex: Long? = null,
    totalChunks: Long? = null,
    fileSize: Long? = null,
    expectedSha256: String? = null,
    isComplete: Boolean? = null,
    filename: String? = null,
    reset: Boolean? = null,
    verifyOnly: Boolean? = null,
    fileRetention: Long
) {
    apiRequestUnit(
        "upload_file_stream",
        mapOf(
            "stream_id" to streamId,
            "chunk_data" to chunkData,
            "chunk_index" to chunkIndex,
            "total_chunks" to totalChunks,
            "file_size" to fileSize,
            "expected_sha256" to expectedSha256,
            "is_complete" to isComplete,
            "filename" to filename,
            "reset" to reset,
            "verify_only" to verifyOnly,
            "file_retention" to fileRetention
        )
    )
}

/**
 * 清理流式传输临时文件
 */
suspend fun NapCatBotApi.cleanStreamTempFile() {
    apiRequestUnit("clean_stream_temp_file")
}

/**
 * 下载图片文件流
 *
 * @param file 文件路径或URL
 * @param fileId 文件ID
 * @param chunkSize 分块大小
 */
suspend fun NapCatBotApi.downloadFileImageStream(
    file: String? = null,
    fileId: String? = null,
    chunkSize: Long? = null
): NapCatStreamApiExpand.File {
    return apiRequest(
        "download_file_image_stream",
        mapOf("file" to file, "file_id" to fileId, "chunk_size" to chunkSize)
    )
}

/**
 * 下载语音文件流
 *
 * @param file 文件路径或URL
 * @param fileId 文件ID
 * @param chunkSize 分块大小
 * @param outFormat 输出格式
 */
suspend fun NapCatBotApi.downloadFileRecordStream(
    file: String? = null,
    fileId: String? = null,
    chunkSize: Long? = null,
    outFormat: String? = null
): NapCatStreamApiExpand.File {
    return apiRequest(
        "download_file_record_stream",
        mapOf("file" to file, "file_id" to fileId, "chunk_size" to chunkSize, "out_format" to outFormat)
    )
}

/**
 * 测试下载流
 *
 * @param error 是否触发错误
 */
suspend fun NapCatBotApi.testDownloadStream(error: Boolean = false): JsonNode {
    return apiRequest("test_download_stream", mapOf("error" to error))
}
