@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * NapCatFileExtraExpand
 */
class NapCatFileExtraExpand {
    /**
     * FilesetId
     */
    data class FilesetId(
        /**
         * 文件集 ID
         */
        @field:JsonProperty("fileset_id")
        val filesetId: String
    )

}

/**
 * 取消在线文件
 *
 * @param userId 用户ID
 * @param msgId 消息ID
 */
suspend fun NapCatBotApi.cancelOnlineFile(userId: String, msgId: String) {
    apiRequestUnit("cancel_online_file", mapOf("user_id" to userId, "msg_id" to msgId))
}

/**
 * 创建闪照任务
 *
 * @param files 文件列表
 * @param name 任务名称
 * @param thumbPath 缩略图路径
 */
suspend fun NapCatBotApi.createFlashTask(files: List<String>, name: String? = null, thumbPath: String? = null) {
    apiRequestUnit("create_flash_task", mapOf("files" to files, "name" to name, "thumb_path" to thumbPath))
}

/**
 * 下载文件集
 *
 * @param filesetId 文件集ID
 */
suspend fun NapCatBotApi.downloadFileset(filesetId: String): JsonNode {
    return apiRequest("download_fileset", mapOf("fileset_id" to filesetId))
}

/**
 * 获取文件集 ID
 *
 * @param shareCode 分享码
 */
suspend fun NapCatBotApi.getFilesetId(shareCode: String): NapCatFileExtraExpand.FilesetId {
    return apiRequest("get_fileset_id", mapOf("share_code" to shareCode))
}

/**
 * 获取文件集信息
 *
 * @param filesetId 文件集ID
 */
suspend fun NapCatBotApi.getFilesetInfo(filesetId: String): JsonNode {
    return apiRequest("get_fileset_info", mapOf("fileset_id" to filesetId))
}

/**
 * 获取闪照文件列表
 *
 * @param filesetId 文件集ID
 */
suspend fun NapCatBotApi.getFlashFileList(filesetId: String): JsonNode {
    return apiRequest("get_flash_file_list", mapOf("fileset_id" to filesetId))
}

/**
 * 获取闪照文件链接
 *
 * @param filesetId 文件集ID
 * @param fileName 文件名
 * @param fileIndex 文件索引
 */
suspend fun NapCatBotApi.getFlashFileUrl(
    filesetId: String,
    fileName: String? = null,
    fileIndex: Long? = null
): JsonNode {
    return apiRequest(
        "get_flash_file_url",
        mapOf("fileset_id" to filesetId, "file_name" to fileName, "file_index" to fileIndex)
    )
}

/**
 * 获取在线文件消息
 *
 * @param userId 用户ID
 */
suspend fun NapCatBotApi.getOnlineFileMsg(userId: String): JsonNode {
    return apiRequest("get_online_file_msg", mapOf("user_id" to userId))
}

/**
 * 获取文件分享链接
 *
 * @param filesetId 文件集ID
 */
suspend fun NapCatBotApi.getShareLink(filesetId: String): JsonNode {
    return apiRequest("get_share_link", mapOf("fileset_id" to filesetId))
}

/**
 * 移动群文件
 *
 * @param groupId 群组ID
 * @param fileId 文件ID
 * @param currentParentDirectory 当前父目录
 * @param targetParentDirectory 目标父目录
 */
suspend fun NapCatBotApi.moveGroupFile(
    groupId: String,
    fileId: String,
    currentParentDirectory: String,
    targetParentDirectory: String
) {
    apiRequestUnit(
        "move_group_file",
        mapOf(
            "group_id" to groupId,
            "file_id" to fileId,
            "current_parent_directory" to currentParentDirectory,
            "target_parent_directory" to targetParentDirectory
        )
    )
}

/**
 * 接收在线文件
 *
 * @param userId 用户ID
 * @param msgId 消息ID
 * @param elementId 元素ID
 */
suspend fun NapCatBotApi.receiveOnlineFile(userId: String, msgId: String, elementId: String) {
    apiRequestUnit("receive_online_file", mapOf("user_id" to userId, "msg_id" to msgId, "element_id" to elementId))
}

/**
 * 拒绝在线文件
 *
 * @param userId 用户ID
 * @param msgId 消息ID
 * @param elementId 元素ID
 */
suspend fun NapCatBotApi.refuseOnlineFile(userId: String, msgId: String, elementId: String) {
    apiRequestUnit("refuse_online_file", mapOf("user_id" to userId, "msg_id" to msgId, "element_id" to elementId))
}

/**
 * 重命名群文件
 *
 * @param groupId 群组ID
 * @param fileId 文件ID
 * @param currentParentDirectory 当前父目录
 * @param newName 新文件名
 */
suspend fun NapCatBotApi.renameGroupFile(
    groupId: String,
    fileId: String,
    currentParentDirectory: String,
    newName: String
) {
    apiRequestUnit(
        "rename_group_file",
        mapOf(
            "group_id" to groupId,
            "file_id" to fileId,
            "current_parent_directory" to currentParentDirectory,
            "new_name" to newName
        )
    )
}

/**
 * 发送闪照消息
 *
 * @param filesetId 文件集ID
 * @param userId 用户ID
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.sendFlashMsg(filesetId: String, userId: String? = null, groupId: String? = null) {
    apiRequestUnit("send_flash_msg", mapOf("fileset_id" to filesetId, "user_id" to userId, "group_id" to groupId))
}

/**
 * 发送在线文件
 *
 * @param userId 用户ID
 * @param filePath 文件路径
 * @param fileName 文件名
 */
suspend fun NapCatBotApi.sendOnlineFile(userId: String, filePath: String, fileName: String? = null) {
    apiRequestUnit("send_online_file", mapOf("user_id" to userId, "file_path" to filePath, "file_name" to fileName))
}

/**
 * 发送在线文件夹
 *
 * @param userId 用户ID
 * @param folderPath 文件夹路径
 * @param folderName 文件夹名称
 */
suspend fun NapCatBotApi.sendOnlineFolder(userId: String, folderPath: String, folderName: String? = null) {
    apiRequestUnit(
        "send_online_folder",
        mapOf("user_id" to userId, "folder_path" to folderPath, "folder_name" to folderName)
    )
}

/**
 * 传输群文件
 *
 * @param groupId 群组ID
 * @param fileId 文件ID
 */
suspend fun NapCatBotApi.transGroupFile(groupId: String, fileId: String) {
    apiRequestUnit("trans_group_file", mapOf("group_id" to groupId, "file_id" to fileId))
}
