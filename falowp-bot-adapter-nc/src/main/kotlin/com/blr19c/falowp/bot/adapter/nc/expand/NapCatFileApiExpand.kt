@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GetFileData
 */
data class GetFileData(
    @field:JsonProperty("file")
    val file: String,
    @field:JsonProperty("url")
    val url: String,
    @field:JsonProperty("file_size")
    val fileSize: String,
    @field:JsonProperty("file_name")
    val fileName: String,
    @field:JsonProperty("base64")
    val base64: String
)

/**
 * GetGroupFileUrlData
 */
data class GetGroupFileUrlData(
    @field:JsonProperty("url")
    val url: String
)

/**
 * GetImageData
 */
data class GetImageData(
    @field:JsonProperty("file")
    val file: String,
    @field:JsonProperty("url")
    val url: String,
    @field:JsonProperty("file_size")
    val fileSize: String,
    @field:JsonProperty("file_name")
    val fileName: String,
    @field:JsonProperty("base64")
    val base64: String
)

/**
 * GetPrivateFileUrlData
 */
data class GetPrivateFileUrlData(
    @field:JsonProperty("url")
    val url: String
)

/**
 * GetRecordData
 */
data class GetRecordData(
    @field:JsonProperty("file")
    val file: String,
    @field:JsonProperty("url")
    val url: String,
    @field:JsonProperty("file_size")
    val fileSize: String,
    @field:JsonProperty("file_name")
    val fileName: String,
    @field:JsonProperty("base64")
    val base64: String
)

/**
 * NapCatFileApiExpand
 */
class NapCatFileApiExpand

/**
 * 获取文件信息
 */
suspend fun NapCatBotApi.getFile(fileId: String? = null, file: String? = null): GetFileData {
    return apiRequest("get_file", mapOf("file_id" to fileId, "file" to file))
}

/**
 * 获取群文件链接
 */
suspend fun NapCatBotApi.getGroupFileUrl(groupId: Long, fileId: String): GetGroupFileUrlData {
    return apiRequest("get_group_file_url", mapOf("group_id" to groupId, "file_id" to fileId))
}

/**
 * 获取图片消息详情
 */
suspend fun NapCatBotApi.getImage(fileId: String? = null, file: String? = null): GetImageData {
    return apiRequest("get_image", mapOf("file_id" to fileId, "file" to file))
}

/**
 * 获取私聊文件链接
 */
suspend fun NapCatBotApi.getPrivateFileUrl(fileId: String): GetPrivateFileUrlData {
    return apiRequest("get_private_file_url", mapOf("file_id" to fileId))
}

/**
 * 获取语音消息详情
 */
suspend fun NapCatBotApi.getRecord(file: String? = null, fileId: String? = null, outFormat: String): GetRecordData {
    return apiRequest("get_record", mapOf("file" to file, "file_id" to fileId, "out_format" to outFormat))
}
