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
 * @param file 文件路径或 URL
 * @param fileId 文件 ID
 * @param chunkSize 分块大小 (字节)
 */
suspend fun NapCatBotApi.downloadFileStream(file: String? = null, fileId: String? = null, chunkSize: Long? = null): NapCatStreamApiExpand.String {
    return apiRequest("download_file_stream", mapOf("file" to file, "file_id" to fileId, "chunk_size" to chunkSize))
}

/**
 * 上传文件流
 *
 * 以流式方式上传文件数据到机器人
 * @param streamId 流 ID
 * @param chunkData 分块数据 (Base64)
 * @param chunkIndex 分块索引
 * @param totalChunks 总分块数
 * @param fileSize 文件总大小
 * @param expectedSha256 期望的 SHA256
 * @param isComplete 是否完成
 * @param filename 文件名
 * @param reset 是否重置
 * @param verifyOnly 是否仅验证
 * @param fileRetention 文件保留时间 (毫秒)
 */
suspend fun NapCatBotApi.uploadFileStream(streamId: String, chunkData: String? = null, chunkIndex: Long? = null, totalChunks: Long? = null, fileSize: Long? = null, expectedSha256: String? = null, isComplete: Boolean? = null, filename: String? = null, reset: Boolean? = null, verifyOnly: Boolean? = null, fileRetention: Long) {
    apiRequestUnit("upload_file_stream", mapOf("stream_id" to streamId, "chunk_data" to chunkData, "chunk_index" to chunkIndex, "total_chunks" to totalChunks, "file_size" to fileSize, "expected_sha256" to expectedSha256, "is_complete" to isComplete, "filename" to filename, "reset" to reset, "verify_only" to verifyOnly, "file_retention" to fileRetention))
}
