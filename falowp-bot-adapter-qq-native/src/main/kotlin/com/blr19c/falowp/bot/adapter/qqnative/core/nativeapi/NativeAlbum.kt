package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tools.jackson.databind.JsonNode
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * 封装群相册查询、管理和媒体上传
 */
internal class NativeAlbum(private val bridge: NativeBridge, private val web: ExtensionWeb, private val self: String) {
    private val batch = AtomicLong()
    private fun extra() = obj(
        "map_info" to emptyList<Any>(),
        "map_bytes_info" to emptyList<Any>(),
        "map_user_account" to emptyList<Any>()
    )

    private fun response(result: JsonNode): JsonNode {
        val data = if (result.has("response")) result.path("response") else result
        check(data.has("result") && data.i("result") == 0) { "群相册操作失败：${data.s("result")} ${data.s("errMs")}" }
        return data
    }

    private suspend fun mediaLikeKey(base: Map<String, Any?>, album: String, batch: String, lloc: String): String {
        var cursor = ""
        val seen = mutableSetOf<String>()
        do {
            check(seen.add(cursor)) { "相册点赞查询游标未推进" }
            val page = response(
                bridge.invoke(
                    "album",
                    "getMediaList",
                    base + obj("album_id" to album, "lloc" to lloc, "batch_id" to batch, "attach_info" to cursor)
                )
            )
            val row = page.list("media_list")
                .firstOrNull { it.path("image").s("lloc") == lloc || it.path("video").s("lloc") == lloc }
            if (row != null) {
                require(batch.isBlank() || row.s("batch_id") == batch) { "媒体与上传批次不匹配" }
                return row.path("like").s("key").also { check(it.isNotBlank()) { "QQ 未返回媒体点赞标识" } }
            }
            if (!page.path("next_has_more").asBoolean(false)) break
            cursor = page.s("next_attach_info")
            check(cursor.isNotBlank()) { "QQ 未返回相册下一页游标" }
        } while (true)
        error("相册媒体不存在")
    }

    suspend fun call(operation: String, a: JsonNode): JsonNode {
        val group = a.s("group_id")
        val album = a.s("album_id")
        val lloc = a.s("lloc")
        val seq = (System.nanoTime() and 0x7fffffff).toInt()
        val base = obj(
            "qun_id" to group,
            "attach_info" to a.s("attach_info"),
            "seq" to seq,
            "request_time_line" to obj("request_invoke_time" to "0")
        )
        return when (operation) {
            "get_qun_album_list" -> {
                val result = mutableListOf<JsonNode>()
                var cursor = ""
                do {
                    val page = response(bridge.invoke("album", "getAlbumList", base + obj("attach_info" to cursor)))
                    result.addAll(page.list("album_list"))
                    val next = page.s("attach_info")
                    check(
                        !page.path("has_more").asBoolean(false) || next.isNotBlank() && next != cursor
                    ) { "相册分页游标未推进" }
                    cursor = if (page.path("has_more").asBoolean(false)) next else ""
                } while (cursor.isNotEmpty())
                j(result.map { row ->
                    obj(
                        "album_id" to row.s("album_id"),
                        "album_name" to row.s("name"),
                        "cover_url" to (row.get("cover_url") ?: row.path("cover").get("url")),
                        "create_time" to row.get("create_time"),
                        "raw" to row
                    )
                })
            }

            "get_group_album_media_list" -> response(
                bridge.invoke(
                    "album",
                    "getMediaList",
                    base + obj("album_id" to album, "lloc" to "", "batch_id" to "")
                )
            )

            "del_group_album_media" -> response(
                bridge.invoke(
                    "album",
                    "deleteMedias",
                    seq,
                    group,
                    album,
                    listOf(lloc),
                    emptyList<Any>(),
                    emptyList<Any>()
                )
            )

            "do_group_album_comment" -> {
                require(lloc.isNotBlank())
                val feed = obj(
                    "cell_common" to obj("time" to ""),
                    "cell_user_info" to obj("user" to obj("uin" to self)),
                    "cell_media" to obj(
                        "album_id" to album,
                        "batch_id" to "",
                        "media_items" to listOf(obj("image" to obj("lloc" to lloc)))
                    )
                )
                val content = obj(
                    "client_key" to System.currentTimeMillis() * 1000,
                    "content" to listOf(
                        obj(
                            "type" to 0,
                            "content" to a.s("content"),
                            "who" to 0,
                            "uid" to "",
                            "name" to "",
                            "url" to ""
                        )
                    ),
                    "user" to obj("uin" to self),
                    "like" to obj("id" to "", "count" to 0, "status" to 0)
                )
                response(
                    bridge.invoke(
                        "album",
                        "doQunComment",
                        seq,
                        extra(),
                        group,
                        2,
                        feed,
                        content
                    )
                ).also { check(it.path("comment").s("id").isNotEmpty()) { "QQ 未返回评论 ID" } }
            }

            "set_group_album_media_like" -> {
                val batch = a.s("id")
                val set = a.path("set").asBoolean()
                val prefix = "421_1_0_$group|$album|$batch"
                val id = if (lloc.isBlank()) prefix else mediaLikeKey(base, album, batch, lloc)
                val feed = obj(
                    "cell_common" to obj("time" to System.currentTimeMillis(), "feed_id" to "422_0_$batch"),
                    "cell_user_info" to obj("user" to obj("uin" to self)),
                    "cell_media" to obj("album_id" to album, "batch_id" to batch),
                    "cell_qun_info" to obj("qun_id" to group)
                )
                response(
                    bridge.invoke(
                        "album",
                        "doQunLike",
                        seq,
                        extra(),
                        if (set) 2 else 1,
                        obj("id" to id, "status" to if (set) 0 else 1),
                        feed
                    )
                )
            }

            "upload_image_to_qun_album" -> withExtensionFile(a.s("file")) { path -> upload(a, path) }
            else -> error("未知相册操作")
        }
    }

    private suspend fun upload(a: JsonNode, path: Path): JsonNode = withContext(Dispatchers.IO) {
        val file = hashes(path)
        require(file.size > 0)
        val auth = web.cookies("qzone.qq.com")
        val skey = checkNotNull(auth["skey"]) { "QQ 未返回 skey" }
        val pskey = auth.getValue("p_skey")
        val batchId = batch.updateAndGet { maxOf(it + 1, System.currentTimeMillis() / 1000) }
        val biz = obj(
            "sPicTitle" to path.fileName.toString(),
            "sPicDesc" to "",
            "sAlbumName" to a.s("album_name"),
            "sAlbumID" to a.s("album_id"),
            "iAlbumTypeID" to 0,
            "iBitmap" to 0,
            "iUploadType" to 0,
            "iUpPicType" to 0,
            "iBatchID" to batchId,
            "sPicPath" to "",
            "iPicWidth" to 0,
            "iPicHight" to 0,
            "iWaterType" to 0,
            "iDistinctUse" to 0,
            "iUploadTime" to System.currentTimeMillis() / 1000,
            "iNeedFeeds" to 1,
            "mapExt" to obj("appid" to "qun", "userid" to a.s("group_id")),
            "stExtendInfo" to obj("mapParams" to obj("batch_num" to "1", "photo_num" to "1", "video_num" to "0")),
            "mutliPicInfo" to obj("iBatUploadNum" to "1", "iCurUpload" to "0", "iSuccNum" to 0, "iFailNum" to 0)
        )
        val control = obj(
            "uin" to self,
            "token" to obj("type" to 4, "data" to pskey, "appid" to 5),
            "appid" to "qun",
            "checksum" to file.md5.hex(),
            "check_type" to 0,
            "file_len" to file.size,
            "env" to obj("refer" to "qzone", "deviceInfo" to "h5"),
            "model" to 0,
            "biz_req" to biz,
            "session" to "",
            "asy_upload" to 0,
            "cmd" to "FileUpload"
        )
        val prefix = "https://h5.qzone.qq.com/webapp/json/sliceUpload"
        val gtk = ExtensionWeb.bkn(skey)
        val response = web.request(
            "$prefix/FileBatchControl/${file.md5.hex()}?g_tk=$gtk",
            mapper.writeValueAsBytes(obj("control_req" to listOf(control))),
            cookies = auth
        ).path("data")
        val session = response.s("session")
        check(session.isNotBlank()) { "相册上传会话缺失" }
        val size = response.i("slice_size").coerceIn(16384, 1024 * 1024)
        var offset = 0L
        var seq = 0
        Files.newInputStream(path).use { input ->
            while (true) {
                val bytes = input.readNBytes(size)
                if (bytes.isEmpty()) break
                val end = offset + bytes.size
                val boundary = "falowp-${UUID.randomUUID()}"
                val body = ByteArrayOutputStream()
                fun str(value: String) {
                    body.writeBytes(value.toByteArray())
                }

                val fields = obj(
                    "uin" to self,
                    "appid" to "qun",
                    "session" to session,
                    "offset" to offset,
                    "checksum" to "",
                    "check_type" to 0,
                    "retry" to 0,
                    "seq" to seq,
                    "end" to end,
                    "cmd" to "FileUpload",
                    "slice_size" to size,
                    "biz_req.iUploadType" to 0
                )
                fields.forEach { (key, value) -> str("--$boundary\r\nContent-Disposition: form-data; name=\"$key\"\r\n\r\n$value\r\n") }
                str("--$boundary\r\nContent-Disposition: form-data; name=\"data\"; filename=\"blob\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                body.writeBytes(bytes)
                str("\r\n--$boundary--\r\n")
                web.request(
                    "$prefix/FileUpload?seq=$seq&retry=0&offset=$offset&end=$end&total=${file.size}&type=form&g_tk=$gtk",
                    body.toByteArray(),
                    "multipart/form-data; boundary=$boundary",
                    auth
                )
                offset = end
                seq++
            }
        }
        check(offset == file.size) { "上传期间文件发生变化" }
        return@withContext j(null)
    }
}
