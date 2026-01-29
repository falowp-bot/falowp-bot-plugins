@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatFileApiExpand
 */
class NapCatFileApiExpand {
    /**
     * File
     */
    data class File(
        /**
         * 本地路径
         */
        @field:JsonProperty("file")
        val file: String?,
        /**
         * 下载URL
         */
        @field:JsonProperty("url")
        val url: String?,
        /**
         * 文件大小
         */
        @field:JsonProperty("file_size")
        val fileSize: String?,
        /**
         * 文件名
         */
        @field:JsonProperty("file_name")
        val fileName: String?,
        /**
         * Base64编码
         */
        @field:JsonProperty("base64")
        val base64: String?
    )

    /**
     * GroupFileUrl
     */
    data class GroupFileUrl(
        /**
         * 文件下载链接
         */
        @field:JsonProperty("url")
        val url: String?
    )

    /**
     * Image
     */
    data class Image(
        /**
         * 本地路径
         */
        @field:JsonProperty("file")
        val file: String?,
        /**
         * 下载URL
         */
        @field:JsonProperty("url")
        val url: String?,
        /**
         * 文件大小
         */
        @field:JsonProperty("file_size")
        val fileSize: String?,
        /**
         * 文件名
         */
        @field:JsonProperty("file_name")
        val fileName: String?,
        /**
         * Base64编码
         */
        @field:JsonProperty("base64")
        val base64: String?
    )

    /**
     * PrivateFileUrl
     */
    data class PrivateFileUrl(
        /**
         * 文件下载链接
         */
        @field:JsonProperty("url")
        val url: String?
    )

    /**
     * Record
     */
    data class Record(
        /**
         * 本地路径
         */
        @field:JsonProperty("file")
        val file: String?,
        /**
         * 下载URL
         */
        @field:JsonProperty("url")
        val url: String?,
        /**
         * 文件大小
         */
        @field:JsonProperty("file_size")
        val fileSize: String?,
        /**
         * 文件名
         */
        @field:JsonProperty("file_name")
        val fileName: String?,
        /**
         * Base64编码
         */
        @field:JsonProperty("base64")
        val base64: String?
    )

}

/**
 * 获取文件
 *
 * 获取指定文件的详细信息及下载路径
 * @param file 文件路径、URL或Base64
 * @param fileId 文件ID
 */
suspend fun NapCatBotApi.getFile(file: String? = null, fileId: String? = null): NapCatFileApiExpand.File {
    return apiRequest("get_file", mapOf("file" to file, "file_id" to fileId))
}

/**
 * 获取群文件URL
 *
 * 获取指定群文件的下载链接
 * @param groupId 群号
 * @param fileId 文件ID
 */
suspend fun NapCatBotApi.getGroupFileUrl(groupId: String, fileId: String): NapCatFileApiExpand.GroupFileUrl {
    return apiRequest("get_group_file_url", mapOf("group_id" to groupId, "file_id" to fileId))
}

/**
 * 获取图片
 *
 * 获取指定图片的信息及路径
 * @param file 文件路径、URL或Base64
 * @param fileId 文件ID
 */
suspend fun NapCatBotApi.getImage(file: String? = null, fileId: String? = null): NapCatFileApiExpand.Image {
    return apiRequest("get_image", mapOf("file" to file, "file_id" to fileId))
}

/**
 * 获取私聊文件URL
 *
 * 获取指定私聊文件的下载链接
 * @param fileId 文件ID
 */
suspend fun NapCatBotApi.getPrivateFileUrl(fileId: String): NapCatFileApiExpand.PrivateFileUrl {
    return apiRequest("get_private_file_url", mapOf("file_id" to fileId))
}

/**
 * 获取语音
 *
 * 获取指定语音文件的信息，并支持格式转换
 * @param file 文件路径、URL或Base64
 * @param fileId 文件ID
 * @param outFormat 输出格式
 */
suspend fun NapCatBotApi.getRecord(file: String? = null, fileId: String? = null, outFormat: String): NapCatFileApiExpand.Record {
    return apiRequest("get_record", mapOf("file" to file, "file_id" to fileId, "out_format" to outFormat))
}
