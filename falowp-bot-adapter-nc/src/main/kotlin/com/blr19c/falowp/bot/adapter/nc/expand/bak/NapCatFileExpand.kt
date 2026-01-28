package com.blr19c.falowp.bot.adapter.nc.expand.bak

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.system.json.Json
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import java.net.URI

/**
 * 文件类扩展
 */
class NapCatFileExpand {

    /**
     * 群文件链接信息
     */
    data class GroupFileUrl(
        /**
         * 文件链接
         */
        @field:JsonProperty("url")
        val url: URI,
    )

    /**
     * 群文件信息
     */
    data class GroupFileInfo(
        val fileId: String,
        val fileName: String,
        val size: Long,
    )

}

/**
 * 上传群文件
 *
 * @param groupId 群组id
 * @param file 本地文件路径
 * @param name 文件名（不传则使用原文件名）
 * @param folderId 目标文件夹id（不传则根目录）
 */
suspend fun NapCatBotApi.uploadGroupFile(
    groupId: String = this.receiveMessage.source.id,
    file: String,
    name: String? = null,
    folderId: String? = null
) {
    apiRequestUnit(
        "upload_group_file",
        mapOf(
            "group_id" to groupId,
            "file" to file,
            "name" to name,
            "folder_id" to folderId
        )
    )
}

/**
 * 上传私聊文件
 *
 * @param userId 用户id
 * @param file 本地文件路径
 * @param name 文件名（不传则使用原文件名）
 */
suspend fun NapCatBotApi.uploadPrivateFile(
    userId: String = this.receiveMessage.sender.id,
    file: String,
    name: String? = null
) {
    apiRequestUnit(
        "upload_private_file",
        mapOf(
            "user_id" to userId,
            "file" to file,
            "name" to name
        )
    )
}

/**
 * 获取群根目录文件列表
 *
 * @param groupId 群组id
 * @return 根目录文件列表
 */
suspend fun NapCatBotApi.getGroupRootFiles(
    groupId: String = this.receiveMessage.source.id
): JsonNode? {
    return apiRequest(
        "get_group_root_files",
        mapOf("group_id" to groupId)
    )
}

/**
 * 获取群子目录文件列表
 *
 * @param groupId 群组id
 * @param folderId 文件夹id
 * @return 子目录文件列表
 */
suspend fun NapCatBotApi.getGroupFilesByFolder(
    groupId: String = this.receiveMessage.source.id,
    folderId: String
): JsonNode? {
    return apiRequest(
        "get_group_files_by_folder",
        mapOf(
            "group_id" to groupId,
            "folder_id" to folderId
        )
    )
}

/**
 * 获取群文件系统信息
 *
 * @param groupId 群组id
 * @return 文件系统信息
 */
suspend fun NapCatBotApi.getGroupFileSystemInfo(
    groupId: String = this.receiveMessage.source.id
): JsonNode? {
    return apiRequest(
        "get_group_file_system_info",
        mapOf("group_id" to groupId)
    )
}

/**
 * 获取文件信息
 *
 * @param fileId 文件id
 * @return 文件信息
 */
suspend fun NapCatBotApi.getFileInfo(fileId: String): JsonNode? {
    return apiRequest(
        "get_file_info",
        mapOf("file_id" to fileId)
    )
}

/**
 * 获取群文件链接
 *
 * @param groupId 群组id
 * @param fileId 文件id
 * @param busid 业务id（部分实现需要）
 * @return 文件链接信息
 */
suspend fun NapCatBotApi.getGroupFileUrl(
    groupId: String = this.receiveMessage.source.id,
    fileId: String,
    busid: Int? = null
): NapCatFileExpand.GroupFileUrl {
    return apiRequest(
        "get_group_file_url",
        mapOf(
            "group_id" to groupId,
            "file_id" to fileId,
            "busid" to busid
        )
    )
}


/**
 * 获取私聊文件链接
 *
 * @param userId 用户id
 * @param fileId 文件id
 * @return 文件链接信息
 */
suspend fun NapCatBotApi.getPrivateFileUrl(
    userId: String = this.receiveMessage.sender.id,
    fileId: String
): JsonNode? {
    return apiRequest(
        "get_private_file_url",
        mapOf(
            "user_id" to userId,
            "file_id" to fileId
        )
    )
}

/**
 * 创建群文件文件夹
 *
 * @param groupId 群组id
 * @param name 文件夹名称
 * @param parentId 父文件夹id（不传则根目录）
 */
suspend fun NapCatBotApi.createGroupFileFolder(
    groupId: String = this.receiveMessage.source.id,
    name: String,
    parentId: String? = null
) {
    apiRequestUnit(
        "create_group_file_folder",
        mapOf(
            "group_id" to groupId,
            "name" to name,
            "parent_id" to parentId
        )
    )
}

/**
 * 删除群文件
 *
 * @param groupId 群组id
 * @param fileId 文件id
 * @param busid 业务id（部分实现需要）
 */
suspend fun NapCatBotApi.deleteGroupFile(
    groupId: String = this.receiveMessage.source.id,
    fileId: String,
    busid: Int? = null
) {
    apiRequestUnit(
        "delete_group_file",
        mapOf(
            "group_id" to groupId,
            "file_id" to fileId,
            "busid" to busid
        )
    )
}

/**
 * 删除群文件夹
 *
 * @param groupId 群组id
 * @param folderId 文件夹id
 */
suspend fun NapCatBotApi.deleteGroupFolder(
    groupId: String = this.receiveMessage.source.id,
    folderId: String
) {
    apiRequestUnit(
        "delete_group_folder",
        mapOf(
            "group_id" to groupId,
            "folder_id" to folderId
        )
    )
}

/**
 * 移动群文件
 *
 * @param groupId 群组id
 * @param fileId 文件id
 * @param destFolderId 目标文件夹id（不传则根目录）
 */
suspend fun NapCatBotApi.moveGroupFile(
    groupId: String = this.receiveMessage.source.id,
    fileId: String,
    destFolderId: String? = null
) {
    apiRequestUnit(
        "move_group_file",
        mapOf(
            "group_id" to groupId,
            "file_id" to fileId,
            "dest_folder_id" to destFolderId
        )
    )
}

/**
 * 重命名群文件
 *
 * @param groupId 群组id
 * @param fileId 文件id
 * @param name 新文件名
 */
suspend fun NapCatBotApi.renameGroupFile(
    groupId: String = this.receiveMessage.source.id,
    fileId: String,
    name: String
) {
    apiRequestUnit(
        "rename_group_file",
        mapOf(
            "group_id" to groupId,
            "file_id" to fileId,
            "name" to name
        )
    )
}

/**
 * 转存为永久文件
 *
 * @param fileId 文件id
 * @return 转存结果
 */
suspend fun NapCatBotApi.saveFileToPermanent(
    fileId: String
): JsonNode {
    return apiRequest(
        "save_file_to_permanent",
        mapOf("file_id" to fileId)
    )
}

/**
 * 下载文件到缓存目录
 *
 * @param url 文件url
 * @param name 保存文件名（可选）
 * @return 下载结果（通常包含缓存路径）
 */
suspend fun NapCatBotApi.downloadFileToCache(
    url: String,
    name: String? = null
): JsonNode? {
    return apiRequest(
        "download_file_to_cache",
        mapOf(
            "url" to url,
            "name" to name
        )
    )
}

/**
 * 清空缓存
 */
suspend fun NapCatBotApi.clearCache() {
    apiRequestUnit("clear_cache")
}

/**
 * 删除群相册文件
 *
 * @param groupId 群组id
 * @param albumId 相册id
 * @param fileId 文件id
 */
suspend fun NapCatBotApi.deleteGroupAlbumFile(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    fileId: String
) {
    apiRequestUnit(
        "delete_group_album_file",
        mapOf(
            "group_id" to groupId,
            "album_id" to albumId,
            "file_id" to fileId
        )
    )
}

/**
 * 点赞群相册
 *
 * @param groupId 群组id
 * @param albumId 相册id
 * @param fileId 文件id
 * @param like 是否点赞
 */
suspend fun NapCatBotApi.likeGroupAlbum(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    fileId: String,
    like: Boolean = true
) {
    apiRequestUnit(
        "like_group_album",
        mapOf(
            "group_id" to groupId,
            "album_id" to albumId,
            "file_id" to fileId,
            "like" to like
        )
    )
}

/**
 * 查看群相册评论
 *
 * @param groupId 群组id
 * @param albumId 相册id
 * @param fileId 文件id
 * @return 评论列表
 */
suspend fun NapCatBotApi.getGroupAlbumComments(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    fileId: String
): ArrayNode {
    return apiRequest<ArrayNode>(
        "get_group_album_comments",
        mapOf(
            "group_id" to groupId,
            "album_id" to albumId,
            "file_id" to fileId
        )
    ) ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取群相册列表
 *
 * @param groupId 群组id
 * @return 相册列表
 */
suspend fun NapCatBotApi.getGroupAlbumList(
    groupId: String = this.receiveMessage.source.id
): ArrayNode {
    return apiRequest<ArrayNode>(
        "get_group_album_list",
        mapOf("group_id" to groupId)
    ) ?: Json.objectMapper().createArrayNode()
}

/**
 * 上传图片到群相册
 *
 * @param groupId 群组id
 * @param albumId 相册id
 * @param file 本地图片路径
 * @param name 图片名（可选）
 */
suspend fun NapCatBotApi.uploadImageToGroupAlbum(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    file: String,
    name: String? = null
) {
    apiRequestUnit(
        "upload_image_to_group_album",
        mapOf(
            "group_id" to groupId,
            "album_id" to albumId,
            "file" to file,
            "name" to name
        )
    )
}

/**
 * 获取群相册总列表
 *
 * @return 群相册总列表
 */
suspend fun NapCatBotApi.getGroupAlbumTotalList(): ArrayNode {
    return apiRequest<ArrayNode>("get_group_album_total_list")
}
