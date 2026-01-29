@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode

/**
 * NapCatStreamApiExpand
 */
class NapCatStreamApiExpand {
}

/**
 * 下载文件流
 *
 * 以流式方式从网络或本地下载文件
 */
suspend fun NapCatBotApi.downloadFileStream(file: String? = null, fileId: String? = null, chunkSize: Long? = null): NapCatStreamApiExpand.String {
    return apiRequest("download_file_stream", mapOf("file" to file, "file_id" to fileId, "chunk_size" to chunkSize))
}

/**
 * 上传文件流
 *
 * 以流式方式上传文件数据到机器人
 */
suspend fun NapCatBotApi.uploadFileStream(streamId: String, chunkData: String? = null, chunkIndex: Long? = null, totalChunks: Long? = null, fileSize: Long? = null, expectedSha256: String? = null, isComplete: Boolean? = null, filename: String? = null, reset: Boolean? = null, verifyOnly: Boolean? = null, fileRetention: Long) {
    apiRequestUnit("upload_file_stream", mapOf("stream_id" to streamId, "chunk_data" to chunkData, "chunk_index" to chunkIndex, "total_chunks" to totalChunks, "file_size" to fileSize, "expected_sha256" to expectedSha256, "is_complete" to isComplete, "filename" to filename, "reset" to reset, "verify_only" to verifyOnly, "file_retention" to fileRetention))
}
