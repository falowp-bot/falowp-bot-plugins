package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand

import com.blr19c.falowp.bot.adapter.qqnative.adapter.QQNativeBotApi
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.*
import com.blr19c.falowp.bot.system.api.SendMessage
import java.net.URI
import java.nio.file.Path

/**
 * 分页查询群文件，folderId 为空时查询根目录
 */
suspend fun QQNativeBotApi.getGroupFiles(
    groupId: String = receiveMessage.source.id, folderId: String = "",
    startIndex: Int = 0, count: Int = 100
): QQNativeGroupFilePage = expandRequest(
    "group_files_list",
    mapOf("group_id" to groupId, "folder_id" to folderId, "start" to startIndex, "count" to count)
)

/**
 * 查询群文件数量和空间用量
 */
suspend fun QQNativeBotApi.getGroupFileSpace(groupId: String = receiveMessage.source.id): QQNativeGroupFileSpace =
    expandRequest("group_files_space", mapOf("group_id" to groupId))

/**
 * 查询单个群文件的详情
 */
suspend fun QQNativeBotApi.getGroupFile(fileId: String, groupId: String = receiveMessage.source.id): QQNativeGroupFile =
    expandRequest("group_files_info", mapOf("group_id" to groupId, "file_id" to fileId))

/**
 * 获取群文件临时下载地址
 */
suspend fun QQNativeBotApi.getGroupFileUrl(fileId: String, groupId: String = receiveMessage.source.id): String =
    expandRequest("group_files_url", mapOf("group_id" to groupId, "file_id" to fileId))

/**
 * 在群文件根目录创建文件夹
 */
suspend fun QQNativeBotApi.createGroupFolder(
    name: String,
    groupId: String = receiveMessage.source.id
): QQNativeGroupFolder =
    expandRequest("group_files_create_folder", mapOf("group_id" to groupId, "name" to name))

/**
 * 删除群文件夹及其内容
 */
suspend fun QQNativeBotApi.deleteGroupFolder(folderId: String, groupId: String = receiveMessage.source.id) =
    expandUnit("group_files_delete_folder", mapOf("group_id" to groupId, "folder_id" to folderId))

/**
 * 删除群文件，业务标识从文件详情读取
 */
suspend fun QQNativeBotApi.deleteGroupFile(fileId: String, groupId: String = receiveMessage.source.id) =
    expandUnit("group_files_delete", mapOf("group_id" to groupId, "file_id" to fileId))

/**
 * 移动群文件，目录标识使用列表接口的返回值，根目录传 /
 */
suspend fun QQNativeBotApi.moveGroupFile(
    fileId: String, fromFolder: String, toFolder: String,
    groupId: String = receiveMessage.source.id
) = expandUnit(
    "group_files_move",
    mapOf("group_id" to groupId, "file_id" to fileId, "from" to fromFolder, "to" to toFolder)
)

/**
 * 重命名群文件，folderId 为文件当前所在目录
 */
suspend fun QQNativeBotApi.renameGroupFile(
    fileId: String, name: String, folderId: String = "/",
    groupId: String = receiveMessage.source.id
) = expandUnit(
    "group_files_rename",
    mapOf("group_id" to groupId, "file_id" to fileId, "name" to name, "folder_id" to folderId)
)

/**
 * 将临时群文件转为永久群文件
 */
suspend fun QQNativeBotApi.saveGroupFile(fileId: String, groupId: String = receiveMessage.source.id) =
    expandUnit("group_files_save", mapOf("group_id" to groupId, "file_id" to fileId))

/**
 * 上传本地文件到指定群文件夹
 */
suspend fun QQNativeBotApi.uploadGroupFile(
    file: Path, name: String = file.fileName.toString(), folderId: String = "",
    groupId: String = receiveMessage.source.id
) = uploadGroupFile(file.toUri(), name, folderId, groupId)

/**
 * 下载并上传远程文件到指定群文件夹
 */
suspend fun QQNativeBotApi.uploadGroupFile(
    file: URI, name: String, folderId: String = "",
    groupId: String = receiveMessage.source.id
) = sendGroup(
    SendMessage.builder().apply { messageList.addLast(QQNativeFileMessage(file, name, folderId)) }.build(),
    sourceId = groupId
)
