@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * NapCatStreamTransferExpand
 */
class NapCatStreamTransferExpand {

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
 * 清理流式传输临时文件
 */
suspend fun NapCatBotApi.cleanStreamTempFile() {
    apiRequestUnit("clean_stream_temp_file")
}

/**
 * 下载图片文件流
 */
suspend fun NapCatBotApi.downloadFileImageStream(
    file: String? = null,
    fileId: String? = null,
    chunkSize: Long? = null
): NapCatStreamTransferExpand.File {
    return apiRequest(
        "download_file_image_stream",
        mapOf("file" to file, "file_id" to fileId, "chunk_size" to chunkSize)
    )
}

/**
 * 下载语音文件流
 */
suspend fun NapCatBotApi.downloadFileRecordStream(
    file: String? = null,
    fileId: String? = null,
    chunkSize: Long? = null,
    outFormat: String? = null
): NapCatStreamTransferExpand.File {
    return apiRequest(
        "download_file_record_stream",
        mapOf("file" to file, "file_id" to fileId, "chunk_size" to chunkSize, "out_format" to outFormat)
    )
}

/**
 * 测试下载流
 */
suspend fun NapCatBotApi.testDownloadStream(error: Boolean = false): JsonNode {
    return apiRequest("test_download_stream", mapOf("error" to error))
}
