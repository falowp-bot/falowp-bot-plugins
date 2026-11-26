package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativeGroupFilePage
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import com.blr19c.falowp.bot.system.json.Json
import kotlinx.coroutines.runBlocking
import tools.jackson.databind.JsonNode
import kotlin.test.*

class GroupFilesTest {
    private class Fixture {
        val callbacks = NativeCallbacks()
        val calls = mutableListOf<Pair<String, JsonNode>>()
        var respond: (String, JsonNode) -> JsonNode = { _, _ -> error("未设置回执") }
        val files = NativeGroupFiles(NativeBridge { _, args ->
            val name = args["method"].toString()
            val params = j(args).path("arguments")
            calls.add(name to params)
            respond(name, params)
        })

        suspend fun call(op: String, vararg args: Pair<String, Any?>) =
            files.call("group_files_$op", j(obj("group_id" to "42", *args)))

        fun event(vararg items: Any?, complete: Boolean = false, next: Int = 2) {
            callbacks.event(
                j(
                    obj(
                        "name" to "onGroupFileInfoUpdate", "arguments" to listOf(
                            obj(
                                "retCode" to 0, "isEnd" to complete, "nextIndex" to next, "item" to items.toList()
                            )
                        )
                    )
                )
            )
        }

        fun info() = j(
            obj(
                "result" to 0, "groupFileInfoResult" to obj(
                    "retCode" to 0,
                    "fileInfo" to obj("fileId" to "/file", "busId" to 104)
                )
            )
        )
    }

    @Test
    fun listMatchesGroupAndPreservesPaginationAndLargeSizes(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { name, params ->
            assertEquals("sendSsoCmdReqByContend", name)
            assertEquals("OidbSvcTrpcTcp.0x6D8_1", params[0].asString())
            val request = b(b(params[1].binary(), 4), 2)
            assertEquals("folder", s(request, 3))
            assertEquals(42uL, u(request, 1))
            f.event(complete = true, next = 0)
            f.event(obj("peerId" to "99", "fileInfo" to obj("fileId" to "wrong")))
            j(
                obj(
                    "result" to 0, "rspbuffer" to binary(
                        m(
                            4, m(
                                2,
                                n(1, 0),
                                n(4, 0),
                                n(13, 2),
                                m(5, n(1, 1), m(3, m(1, "/file"), n(3, 9000000000), n(4, 104))),
                                m(5, n(1, 2), m(2, m(1, "/folder"), m(3, "目录"), n(8, 3)))
                            )
                        )
                    )
                )
            )
        }
        val page = Json.convertValue<QQNativeGroupFilePage>(
            f.call(
                "list",
                "folder_id" to "folder",
                "start" to 0,
                "count" to 2
            )
        )
        assertEquals(9000000000, page.files.single().size)
        assertEquals("/file", page.files.single().id)
        assertEquals(2, page.nextIndex)
        assertFalse(page.complete)
        assertEquals("目录", page.folders.single().name)
        assertFailsWith<IllegalArgumentException> { f.call("list", "count" to 0) }
    }

    @Test
    fun listRejectsStuckCursorAndSpaceRequiresServerResult(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { _, _ -> j(obj("result" to 0, "rspbuffer" to binary(m(4, m(2, n(1, 0), n(4, 0), n(13, 0)))))) }
        assertFailsWith<IllegalStateException> { f.call("list", "count" to 1, "start" to 0) }
        f.respond = { method, params ->
            assertEquals("sendSsoCmdReqByContend", method)
            val sub = u(params[1].binary(), 2).toInt()
            val detail = if (sub == 2) cat(n(1, 0), n(4, 18)) else cat(n(1, 0), n(4, 10000000000), n(5, 7000000000))
            j(obj("result" to 0, "rspbuffer" to binary(m(4, m(sub + 1, detail)))))
        }
        val space = f.call("space")
        assertEquals(18, space.i("fileCount"))
        assertEquals(7000000000, space.l("usedSpace"))
    }

    @Test
    fun deleteUsesActualBusIdAndRejectsPartialSuccess(): Unit = runBlocking {
        val f = Fixture()
        var success = false
        f.respond = { method, params ->
            when (method) {
                "getGroupFileInfo" -> f.info()
                "deleteGroupFile" -> {
                    assertEquals(104, params[1][0].asInt())
                    j(
                        obj(
                            "result" to 0, "transGroupFileResult" to obj(
                                "result" to obj("retCode" to 0),
                                "successFileIdList" to if (success) listOf("/file") else emptyList(),
                                "failFileIdList" to emptyList<String>()
                            )
                        )
                    )
                }

                else -> error(method)
            }
        }
        assertFailsWith<IllegalStateException> { f.call("delete", "file_id" to "/file") }
        success = true
        f.call("delete", "file_id" to "/file")
    }

    @Test
    fun moveRenameAndUrlEncodeRealIdsAndCheckInnerFailure(): Unit = runBlocking {
        val f = Fixture()
        var rejected = false
        f.respond = { method, params ->
            when (method) {
                "getGroupFileInfo" -> f.info()
                "sendSsoCmdReqByContend" -> {
                    val request = params[1].binary()
                    val sub = u(request, 2).toInt()
                    val field = sub + 1
                    val data = b(b(request, 4), field)
                    assertEquals(42uL, u(data, 1))
                    assertEquals(104uL, u(data, 3))
                    assertEquals("/file", s(data, 4))
                    if (sub == 5) {
                        assertEquals("/", s(data, 5)); assertEquals("/folder", s(data, 6))
                    }
                    if (sub == 4) assertEquals(if (rejected) "no" else "renamed", s(data, 6))
                    val detail = if (sub == 2) cat(n(1, 0), m(5, "download.qq.com"), m(6, byteArrayOf(1, 2))) else n(
                        1,
                        if (rejected) 7 else 0
                    )
                    j(obj("result" to 0, "rspbuffer" to binary(cat(n(3, 0), m(4, m(field, detail))))))
                }

                else -> error(method)
            }
        }
        f.call("move", "file_id" to "/file", "from" to "/", "to" to "/folder")
        f.call("rename", "file_id" to "/file", "folder_id" to "/", "name" to "renamed")
        assertEquals("https://download.qq.com/ftn_handler/0102/?fname=", f.call("url", "file_id" to "/file").asString())
        rejected = true
        assertFailsWith<IllegalStateException> {
            f.call(
                "rename",
                "file_id" to "/file",
                "folder_id" to "/",
                "name" to "no"
            )
        }
    }

    @Test
    fun foldersAndPermanentSaveValidateNestedResult(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { method, _ ->
            when (method) {
                "createGroupFolder" -> j(
                    obj(
                        "result" to 0, "resultWithGroupItem" to obj(
                            "result" to obj("retCode" to 0),
                            "groupItem" to obj("folderInfo" to obj("folderId" to "/new", "folderName" to "新目录"))
                        )
                    )
                )

                "deleteGroupFolder" -> j(obj("result" to 0, "groupFileCommonResult" to obj("retCode" to 5)))
                "transGroupFile" -> j(
                    obj(
                        "result" to 0,
                        "transGroupFileResult" to obj("result" to obj("retCode" to 6))
                    )
                )

                else -> error(method)
            }
        }
        assertEquals("/new", f.call("create_folder", "name" to "新目录").s("id"))
        assertFailsWith<IllegalArgumentException> { f.call("delete_folder", "folder_id" to "/") }
        assertFailsWith<IllegalStateException> { f.call("delete_folder", "folder_id" to "/new") }
        assertFailsWith<IllegalStateException> { f.call("save", "file_id" to "/file") }
    }
}
