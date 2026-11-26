package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model

/**
 * 群文件的原生标识和存储信息，id 可直接用于下载及管理接口
 */
data class QQNativeGroupFile(
    val id: String, val name: String, val size: Long, val busId: Long,
    val folderId: String, val uploader: String, val uploadedAt: Long, val downloads: Long
)

/**
 * 群文件目录
 */
data class QQNativeGroupFolder(
    val id: String, val name: String, val parentId: String,
    val creator: String, val createdAt: Long, val fileCount: Long
)

/**
 * 一页群文件，未结束时使用 nextIndex 继续查询
 */
data class QQNativeGroupFilePage(
    val files: List<QQNativeGroupFile>, val folders: List<QQNativeGroupFolder>,
    val nextIndex: Int, val complete: Boolean
)

/**
 * QQ 返回的群文件数量和空间用量，容量单位为字节
 */
data class QQNativeGroupFileSpace(val fileCount: Long, val usedSpace: Long, val totalSpace: Long)
