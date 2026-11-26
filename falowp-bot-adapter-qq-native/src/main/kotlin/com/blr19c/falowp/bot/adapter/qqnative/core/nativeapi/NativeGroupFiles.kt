package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.all
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import tools.jackson.databind.JsonNode

/**
 * 群文件列表、目录管理和下载凭据
 */
internal class NativeGroupFiles(private val bridge: NativeBridge) {
    private fun checked(result: JsonNode): JsonNode {
        check(result.has("retCode") && result.i("retCode") == 0) {
            "群文件操作失败：${result.s("clientWording").ifBlank { result.s("retMsg") }}"
        }
        return result
    }

    private suspend fun invoke(method: String, vararg args: Any?): JsonNode = bridge.invoke("richMedia", method, *args)
        .also { checkResult(it, method) }

    private fun file(row: JsonNode) = obj(
        "id" to row.s("fileId"), "name" to row.s("fileName"),
        "size" to row.l("fileSize"), "busId" to row.l("busId"), "folderId" to row.s("parentFolderId"),
        "uploader" to row.s("uploaderUin"), "uploadedAt" to row.l("uploadTime"), "downloads" to row.l("downloadTimes")
    )

    private fun folder(row: JsonNode) = obj(
        "id" to row.s("folderId"), "name" to row.s("folderName"),
        "parentId" to row.s("parentFolderId"), "creator" to row.s("createUin"),
        "createdAt" to row.l("createTime"), "fileCount" to row.l("totalFileCount")
    )

    private suspend fun info(group: String, id: String): JsonNode {
        require(id.isNotBlank()) { "缺少文件 ID" }
        return checked(invoke("getGroupFileInfo", group, id).path("groupFileInfoResult")).path("fileInfo")
            .also { check(it.s("fileId") == id && it.l("busId") in 1..0xffffffffL) { "QQ 未返回目标文件详情" } }
    }

    private suspend fun packet(sub: Int, field: Int, body: ByteArray, command: Int = 0x6d6): ByteArray {
        val reply =
            bridge.packet(
                "OidbSvcTrpcTcp.0x${command.toString(16).uppercase()}_$sub",
                cat(n(1, command), n(2, sub), m(4, m(field, body)), n(12, 1))
            )
        check(u(reply, 3) == 0uL) { "群文件请求失败：${s(reply, 5)}" }
        val detail = b(b(reply, 4), field)
        check(detail.isNotEmpty() && u(detail, 1) == 0uL) { "群文件操作失败：${s(detail, 3).ifBlank { s(detail, 2) }}" }
        return detail
    }

    suspend fun call(operation: String, args: JsonNode): JsonNode {
        val group = args.s("group_id")
        require(group.toLongOrNull()?.let { it > 0 } == true) { "需要有效群号" }
        val id = args.s("file_id")
        return when (operation) {
            "group_files_list" -> {
                val count = args.i("count")
                val start = args.i("start")
                require(count in 1..1000 && start >= 0) { "文件页大小应为 1 到 1000，起始位置不能为负数" }
                val result = packet(
                    1, 2, cat(
                        n(1, group.toLong()), n(2, 7), m(3, args.s("folder_id").ifBlank { "/" }),
                        n(5, count), n(9, 1), n(12, 0xffffff), n(13, start), n(17, 2), n(18, 0)
                    ), 0x6d8
                )
                val complete = u(result, 4) != 0uL
                val next = u(result, 13).toLong()
                check(complete || next in (start.toLong() + 1)..Int.MAX_VALUE.toLong()) { "群文件分页未返回有效游标" }
                val items = all(result, 5)
                j(
                    obj(
                        "files" to items.filter { u(it, 1) == 1uL }.map {
                            val row = b(it, 3)
                            obj(
                                "id" to s(row, 1), "name" to s(row, 2), "size" to u(row, 3).toLong(),
                                "busId" to u(row, 4).toLong(), "folderId" to s(row, 16),
                                "uploader" to u(row, 15).toString(), "uploadedAt" to u(row, 6).toLong(),
                                "downloads" to u(row, 9).toLong()
                            )
                        },
                        "folders" to items.filter { u(it, 1) == 2uL }.map {
                            val row = b(it, 2)
                            obj(
                                "id" to s(row, 1), "parentId" to s(row, 2), "name" to s(row, 3),
                                "creator" to u(row, 6).toString(), "createdAt" to u(row, 4).toLong(),
                                "fileCount" to u(row, 8).toLong()
                            )
                        }, "nextIndex" to next, "complete" to complete
                    )
                )
            }

            "group_files_space" -> {
                val base = cat(n(1, group.toLong()), n(2, 7))
                val count = packet(2, 3, cat(base, n(3, 6)), 0x6d8)
                val space = packet(3, 4, base, 0x6d8)
                j(
                    obj(
                        "fileCount" to u(count, 4).toLong(), "usedSpace" to u(space, 5).toLong(),
                        "totalSpace" to u(space, 4).toLong()
                    )
                )
            }

            "group_files_info" -> j(file(info(group, id)))
            "group_files_create_folder" -> {
                require(args.s("name").isNotBlank()) { "目录名不能为空" }
                val created = invoke("createGroupFolder", group, args.s("name")).path("resultWithGroupItem")
                checked(created.path("result"))
                val item = created.path("groupItem").let { if (it.isArray) it.first() else it }
                val detail = item.path("folderInfo")
                check(detail.s("folderId").isNotBlank()) { "QQ 未返回新目录标识" }
                j(folder(detail))
            }

            "group_files_delete_folder" -> {
                require(args.s("folder_id").isNotBlank() && args.s("folder_id") != "/") { "不能删除根目录" }
                checked(invoke("deleteGroupFolder", group, args.s("folder_id")).path("groupFileCommonResult"))
                j(null)
            }

            "group_files_delete" -> {
                val detail = info(group, id)
                val result =
                    invoke("deleteGroupFile", group, listOf(detail.l("busId")), listOf(id)).path("transGroupFileResult")
                checked(result.path("result"))
                check(
                    result.list("failFileIdList").isEmpty() && result.list("successFileIdList")
                        .any { it.asString() == id }) {
                    "QQ 未确认删除目标群文件"
                }
                j(null)
            }

            "group_files_save" -> {
                require(id.isNotBlank()) { "缺少文件 ID" }
                checked(invoke("transGroupFile", group, id).path("transGroupFileResult").path("result"))
                j(null)
            }

            "group_files_url", "group_files_move", "group_files_rename" -> {
                val detail = info(group, id)
                val common = cat(n(1, group.toLong()), n(3, detail.l("busId")), m(4, id))
                when (operation) {
                    "group_files_url" -> {
                        val result = packet(2, 3, cat(common, n(2, 7)))
                        val host = s(result, 5)
                        val key = b(result, 6).hex()
                        check(host.matches(Regex("[A-Za-z0-9.-]+")) && key.isNotEmpty()) { "QQ 未返回文件下载凭据" }
                        j("https://$host/ftn_handler/$key/?fname=")
                    }

                    "group_files_move" -> {
                        require(args.s("from").isNotBlank() && args.s("to").isNotBlank()) { "缺少源目录或目标目录" }
                        packet(5, 6, cat(common, n(2, 5), m(5, args.s("from")), m(6, args.s("to"))))
                        j(null)
                    }

                    else -> {
                        require(
                            args.s("name").isNotBlank() && args.s("folder_id").isNotBlank()
                        ) { "缺少新文件名或当前目录" }
                        packet(4, 5, cat(common, m(5, args.s("folder_id")), m(6, args.s("name"))))
                        j(null)
                    }
                }
            }

            else -> error("未知群文件操作：$operation")
        }
    }
}
