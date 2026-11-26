package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativeGroupApiExpand
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativePrivateApiExpand
import com.blr19c.falowp.bot.adapter.qqnative.core.history.HistoryKey
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MemoryMessageHistory
import com.blr19c.falowp.bot.system.json.Json
import kotlinx.coroutines.*
import tools.jackson.databind.JsonNode
import kotlin.test.*

class ExtensionsTest {
    @Test
    fun groupWebCsrfUsesSkeyWhileQzonePrefersPskey() {
        val cookies = mapOf("skey" to "session", "p_skey" to "domain")
        assertEquals(ExtensionWeb.bkn("session"), ExtensionWeb.csrf("qun.qq.com", cookies))
        assertEquals(ExtensionWeb.bkn("domain"), ExtensionWeb.csrf("qzone.qq.com", cookies))
        assertEquals(ExtensionWeb.bkn("session"), ExtensionWeb.csrf("qzone.qq.com", mapOf("skey" to "session")))
        assertFailsWith<IllegalStateException> { ExtensionWeb.csrf("qun.qq.com", mapOf("p_skey" to "domain")) }
    }

    private class Fixture {
        val calls = mutableListOf<Pair<String, JsonNode>>()
        var respond: (String, JsonNode) -> JsonNode = { _, _ -> j(obj("result" to 0)) }
        val bridge = NativeBridge { _, args ->
            val method = args["method"].toString()
            val params = j(args).path("arguments")
            calls.add(method to params)
            if (method == "getUidByUins") j(obj("uids" to nativeMap(params[0][0].asString() to "u_${params[0][0].asString()}"))) else respond(
                method,
                params
            )
        }
        val history = MemoryMessageHistory()
        val extensions = NativeExtensions(bridge, NativeContacts(bridge), j(obj("id" to "9", "uid" to "u_9")), history)
        suspend fun call(op: String, vararg args: Pair<String, Any?>) = extensions.call(op, j(obj(*args)))
        fun event(name: String, vararg args: Any?) =
            extensions.callbacks.event(j(obj("name" to name, "arguments" to args.toList())))
    }

    @Test
    fun originalElementsComeFromScopedArchiveWithoutReconstructingText(): Unit = runBlocking {
        val f = Fixture()
        val raw =
            obj("elements" to listOf(obj("elementType" to 11, "marketFaceElement" to obj("key" to "secret-face-key"))))
        f.history.put(HistoryKey("9", 2, "42", "original"), j(obj("raw" to raw)))
        val result = f.call("original_message", "message_id" to "original", "group" to true, "target" to "42")
        assertEquals("secret-face-key", result.path("elements")[0].path("marketFaceElement").s("key"))
        assertFalse(f.calls.any { it.first == "getMsgsByMsgId" })
        assertFailsWith<IllegalStateException> {
            f.call(
                "original_message",
                "message_id" to "original",
                "group" to true,
                "target" to "99"
            )
        }
    }

    @Test
    fun originalLookupWithoutCurrentPeerSupportsSchedulingAndRejectsAmbiguousIds(): Unit = runBlocking {
        val f = Fixture()
        val original = j(
            obj(
                "raw" to obj(
                    "chatType" to 2, "peerUid" to "42",
                    "elements" to listOf(obj("textElement" to obj("content" to "定时转发原文")))
                )
            )
        )
        f.history.put(HistoryKey("9", 2, "42", "same"), original)
        f.history.put(HistoryKey("other", 2, "99", "same"), original)
        assertEquals("42", f.call("original_message", "message_id" to "same").s("peerUid"))
        f.history.put(HistoryKey("9", 1, "u_11", "same"), original)
        assertFailsWith<IllegalStateException> { f.call("original_message", "message_id" to "same") }
        assertEquals(
            "42",
            f.call("original_message", "message_id" to "same", "group" to true, "target" to "42").s("peerUid")
        )
        assertFalse(f.calls.any { it.first == "getMsgsByMsgId" })
    }

    @Test
    fun groupManagementUsesNativeUidAndRefusesInvalidDuration(): Unit = runBlocking {
        val f = Fixture()
        f.call("set_group_admin", "group_id" to "42", "user_id" to "11", "enable" to true)
        assertEquals("modifyMemberRole", f.calls.last().first)
        assertEquals(listOf("42", "u_11", "3"), f.calls.last().second.toList().map { it.asString() })
        f.call("set_group_ban", "group_id" to "42", "user_id" to "11", "duration" to 0)
        assertEquals(0, f.calls.last().second[1][0].i("timeStamp"))
        assertFailsWith<IllegalArgumentException> {
            f.call(
                "set_group_ban",
                "group_id" to "42",
                "user_id" to "11",
                "duration" to -1
            )
        }
        f.respond = { _, _ -> j(obj("result" to 5)) }
        assertFailsWith<IllegalStateException> { f.call("set_group_name", "group_id" to "42", "group_name" to "new") }
    }

    @Test
    fun searchDisablingUsesPresenceNotTruthiness(): Unit = runBlocking {
        val f = Fixture()
        f.call("set_group_search", "group_id" to "42", "no_code_finger_open" to 0)
        val request = f.calls.last().second[0]
        assertEquals(1, request.path("filter").i("noCodeFingerOpenFlag"))
        assertEquals(0, request.path("modifyInfo").i("noCodeFingerOpenFlag"))
        assertEquals(0, request.path("filter").i("noFingerOpenFlag"))
    }

    @Test
    fun robotSettingsPreserveNativeNestedFieldsAndOnlyEnableRequestedFilters(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { method, _ ->
            if (method == "getGroupDetailInfo") f.event(
                "onGroupDetailInfoChange", obj(
                    "groupCode" to "42", "groupExt" to obj(
                        "inviteRobotMemberSwitch" to 1, "inviteRobotMemberExamine" to 2,
                        "cronMuteTask" to obj("activeTaskId" to "existing", "muteTasks" to emptyList<Any>()),
                        "groupIdentifier" to obj(
                            "authInfoList" to emptyList<Any>(),
                            "openPlatformInfo" to obj("name" to "")
                        )
                    )
                )
            )
            j(obj("result" to 0))
        }
        f.call("set_group_robot_add_option", "group_id" to "42", "robot_member_switch" to 0)
        val args = f.calls.last().second
        assertEquals(0, args[0].path("extInfo").i("inviteRobotMemberSwitch"))
        assertEquals("existing", args[0].path("extInfo").path("cronMuteTask").s("activeTaskId"))
        assertEquals(1, args[1].i("inviteRobotMemberSwitch"))
        assertEquals(0, args[1].i("cronMuteTask"))
        assertEquals(0, args[1].i("inviteRobotMemberExamine"))
    }

    @Test
    fun groupDetailWaitsForCorrectEarlyCallback(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { method, _ ->
            assertEquals("getGroupDetailInfo", method)
            f.event("onGroupDetailInfoChange", obj("groupCode" to "99", "groupName" to "wrong"))
            f.event(
                "onGroupDetailInfoChange",
                obj(
                    "groupCode" to "42",
                    "groupName" to "right",
                    "memberNum" to 3,
                    "maxMemberNum" to 200,
                    "shutUpAllTimestamp" to 123
                )
            )
            j(obj("result" to 0))
        }
        val result = Json.convertValue<QQNativeGroupApiExpand.GroupInfo>(f.call("get_group_info", "group_id" to "42"))
        assertEquals("right", result.groupName)
        assertEquals(-1, result.groupAllShut)
        assertEquals(3, result.memberCount)
    }

    @Test
    fun friendProfilesDecodeNestedCoreAndBaseInfo(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { method, _ ->
            when (method) {
                "getBuddyListV2" -> j(
                    obj(
                        "data" to listOf(
                            obj(
                                "categoryId" to 2,
                                "categroyName" to "friends",
                                "buddyUids" to listOf("u_11")
                            )
                        )
                    )
                )

                "getCoreAndBaseInfo" -> j(
                    nativeMap(
                        "u_11" to obj(
                            "coreInfo" to obj(
                                "uid" to "u_11",
                                "uin" to "11",
                                "nick" to "Alice",
                                "remark" to "A"
                            ), "baseInfo" to obj("sex" to 2, "age" to 20)
                        )
                    )
                )

                else -> error(method)
            }
        }
        val list = Json.convertValue<List<QQNativePrivateApiExpand.FriendUser>>(f.call("get_friend_list"))
        assertEquals("11", list.single().userId)
        assertEquals("Alice", list.single().nickname)
        assertEquals("female", list.single().sex)
        val groups =
            Json.convertValue<List<QQNativePrivateApiExpand.FriendsWithCategoryItemItem>>(f.call("get_friends_with_category"))
        assertEquals("friends", groups.single().categoryName)
        assertEquals(11, groups.single().buddyList.single().userId)
    }

    @Test
    fun transientGroupArkEncodingErrorRefreshesOnceAndPersistentErrorsStillFail(): Unit = runBlocking {
        val f = Fixture()
        var attempts = 0
        var persistent = false
        f.respond = { method, _ ->
            when (method) {
                "getGroupRecommendContactArkJson" -> {
                    attempts++
                    if (persistent || attempts == 1) j(
                        obj(
                            "errCode" to 1004,
                            "errMsg" to "string field contains invalid UTF-8"
                        )
                    )
                    else j(obj("errCode" to 0, "arkJson" to "{\"app\":\"com.tencent.contact\"}"))
                }

                "getGroupDetailInfo" -> {
                    f.event("onGroupDetailInfoChange", obj("groupCode" to "42"))
                    j(obj("result" to 0))
                }

                else -> error(method)
            }
        }
        assertEquals("com.tencent.contact", f.call("ark_share_group", "group_id" to "42").s("app"))
        assertEquals(2, attempts)
        persistent = true
        assertFailsWith<IllegalStateException> { f.call("ark_share_group", "group_id" to "42") }
        assertEquals(4, attempts)
    }

    @Test
    fun applicationsUseExactRequestAndApprovalType(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { method, _ ->
            if (method == "getSingleScreenNotifies") f.event(
                "onGroupSingleScreenNotifies",
                false,
                "",
                listOf(obj("seq" to "flag", "type" to 7, "group" to obj("groupCode" to "42")))
            )
            j(obj("result" to 0))
        }
        f.call("set_group_add_request", "flag" to "flag", "approve" to false, "reason" to "reason")
        assertEquals("operateSysNotify", f.calls.last().first)
        assertEquals(2, f.calls.last().second[1].i("operateType"))
        assertEquals("reason", f.calls.last().second[1].path("targetMsg").s("postscript"))
    }

    @Test
    fun unresolvedNoticeUidsDoNotBreakTypedRequestLists(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { method, _ ->
            if (method == "getSingleScreenNotifies") f.event(
                "onGroupSingleScreenNotifies", true, "", listOf(
                    obj(
                        "seq" to "12",
                        "status" to 1,
                        "user1" to obj("uid" to "u_unknown"),
                        "user2" to obj("uid" to "u_operator"),
                        "group" to obj("groupCode" to "42")
                    )
                )
            )
            j(obj("result" to 0))
        }
        val rows =
            Json.convertValue<List<QQNativeGroupApiExpand.GroupIgnoreAddRequestItemItem>>(f.call("get_group_ignore_add_request"))
        assertEquals(0L, rows.single().actor)
        assertEquals(0L, rows.single().invitorUin)
        assertEquals("u_unknown", rows.single().invitorUid)
        assertEquals("u_operator", rows.single().actorUid)
    }

    @Test
    fun albumPaginationAndUnlikePreserveServerContract(): Unit = runBlocking {
        val f = Fixture()
        var pages = 0
        var mediaPages = 0
        var missingKey = false
        f.respond = { method, params ->
            when (method) {
                "getAlbumList" -> {
                    pages++
                    assertEquals(if (pages == 1) "" else "next", params[0].s("attach_info"))
                    j(
                        obj(
                            "response" to obj(
                                "result" to 0,
                                "album_list" to listOf(obj("album_id" to "a$pages", "name" to "Album")),
                                "has_more" to (pages == 1),
                                "attach_info" to "next"
                            )
                        )
                    )
                }

                "getMediaList" -> {
                    mediaPages++
                    val first = params[0].s("attach_info").isEmpty()
                    j(
                        obj(
                            "response" to obj(
                                "result" to 0, "next_has_more" to first, "next_attach_info" to "media-next",
                                "media_list" to if (first) emptyList<Any>() else listOf(
                                    obj(
                                        "image" to obj("lloc" to "photo"),
                                        "batch_id" to "batch",
                                        "like" to obj("key" to if (missingKey) "" else "server-media-key")
                                    )
                                )
                            )
                        )
                    )
                }

                else -> j(obj("result" to 0))
            }
        }
        assertEquals(2, f.call("get_qun_album_list", "group_id" to "42").size())
        f.call(
            "set_group_album_media_like",
            "group_id" to "42",
            "album_id" to "a1",
            "id" to "batch",
            "lloc" to "photo",
            "set" to false
        )
        assertEquals("doQunLike", f.calls.last().first)
        assertEquals(1, f.calls.last().second[2].asInt())
        assertEquals("server-media-key", f.calls.last().second[3].s("id"))
        assertEquals(2, mediaPages)
        missingKey = true
        val likes = f.calls.count { it.first == "doQunLike" }
        assertFailsWith<IllegalStateException> {
            f.call(
                "set_group_album_media_like",
                "group_id" to "42",
                "album_id" to "a1",
                "id" to "batch",
                "lloc" to "photo",
                "set" to true
            )
        }
        assertEquals(likes, f.calls.count { it.first == "doQunLike" })
        f.call("del_group_album_media", "group_id" to "42", "album_id" to "a1", "lloc" to "photo")
        assertEquals("deleteMedias", f.calls.last().first)
        assertEquals(6, f.calls.last().second.size())
    }

    @Test
    fun titleAndTodoPacketsKeep64BitSequenceAndRejectServerErrors(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { _, _ -> j(obj("result" to 0, "rspbuffer" to binary(Wire.n(3, 0)))) }
        f.call("set_group_todo", "group_id" to "42", "message_seq" to "18446744073709551615")
        val packet = f.calls.last().second[1].binary()
        assertEquals(ULong.MAX_VALUE, Wire.u(Wire.b(packet, 4), 2))
        f.call("set_group_special_title", "group_id" to "42", "user_id" to "11", "special_title" to "Title")
        val title = f.calls.last().second[1].binary()
        assertEquals(0uL, Wire.u(title, 12))
        assertEquals("u_11", Wire.s(Wire.b(Wire.b(title, 4), 3), 1))
        f.respond = { _, _ -> j(obj("result" to 0, "rspbuffer" to binary(Wire.n(3, 5)))) }
        assertFailsWith<IllegalStateException> { f.call("set_group_sign", "group_id" to "42") }
    }

    @Test
    fun miniAppUsesServerResponseAndConvertsRawArkKeys(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { method, params ->
            assertEquals("sendSsoCmdReqByContend", method)
            assertEquals("LightAppSvc.mini_app_share.AdaptShareInfo", params[0].asString())
            assertEquals("1109937557", Wire.s(Wire.b(params[1].binary(), 4), 2))
            val raw = mapper.writeValueAsString(
                obj(
                    "appName" to "com.tencent.miniapp_01",
                    "appView" to "view",
                    "metaData" to obj("detail_1" to obj("title" to "Title"))
                )
            )
            j(obj("result" to 0, "rspbuffer" to binary(Wire.m(4, Wire.m(2, raw)))))
        }
        val card = f.call("get_mini_app_ark", "type" to "bili", "title" to "Title")
        assertEquals("com.tencent.miniapp_01", card.s("app"))
        assertEquals("Title", card.path("meta").path("detail_1").s("title"))
    }

    @Test
    fun callbacksWithoutRequestIdsCannotCompleteConcurrentCallsTogether(): Unit = runBlocking {
        val callbacks = NativeCallbacks()
        var invoked = 0
        fun event(value: Int) = callbacks.event(j(obj("name" to "query", "arguments" to listOf(value))))
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            callbacks.await("query") {
                invoked++
                j(null)
            }
        }
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            callbacks.await("query") {
                invoked++
                j(null)
            }
        }
        assertEquals(1, invoked)
        event(1)
        assertEquals(1, first.await().single().asInt())
        yield()
        assertEquals(2, invoked)
        assertFalse(second.isCompleted)
        event(2)
        assertEquals(2, second.await().single().asInt())
        assertFailsWith<IllegalStateException> {
            callbacks.await("query") { j(obj("errCode" to 8, "errMsg" to "rejected")) }
        }
        val afterError = async(start = CoroutineStart.UNDISPATCHED) { callbacks.await("query") { j(null) } }
        event(3)
        assertEquals(3, afterError.await().single().asInt())
    }

    @Test
    fun memberDetailsPreserveAvailableFieldsAndMissingValues(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { _, _ ->
            j(
                obj(
                    "errCode" to 0, "result" to obj(
                        "infos" to nativeMap(
                            "u_11" to obj(
                                "uin" to "11", "nick" to "Alice", "role" to 3, "sex" to 2, "age" to 20,
                                "memberRealLevel" to 5, "joinTime" to "1700000000", "lastSpeakTime" to "1700000001"
                            )
                        )
                    )
                )
            )
        }
        val row = Json.convertValue<List<QQNativeGroupApiExpand.GroupMember>>(
            f.call(
                "get_group_member_list",
                "group_id" to "42"
            )
        ).single()
        assertEquals("admin", row.role)
        assertEquals("female", row.sex)
        assertEquals(5, row.level)
        assertEquals(1700000000, row.joinTime)
        assertNull(row.qqLevel)
    }

    @Test
    fun sharingRequiresSuccessfulNativeResultAndNonemptyCard(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { _, _ -> j(obj("errCode" to 1004, "errMsg" to "encoding error")) }
        assertFailsWith<IllegalStateException> { f.call("ark_share_group", "group_id" to "42") }
        f.respond = { _, _ -> j(obj("errCode" to 0, "arkJson" to "")) }
        assertFailsWith<IllegalStateException> { f.call("ark_share_group", "group_id" to "42") }
        f.respond = { _, _ -> j(obj("errCode" to 0, "arkMsg" to "{\"app\":\"com.tencent.contact\"}")) }
        assertEquals("com.tencent.contact", f.call("ark_share_peer", "user_id" to "11").s("app"))
    }

    @Test
    fun shutListRefreshesMembersAndExcludesExpiredBans(): Unit = runBlocking {
        val f = Fixture()
        val now = System.currentTimeMillis() / 1000
        f.respond = { method, params ->
            assertEquals("getAllMemberList", method)
            assertTrue(params[1].asBoolean())
            j(
                obj(
                    "errCode" to 0, "result" to obj(
                        "infos" to nativeMap(
                            "u_11" to obj("uin" to "11", "nick" to "active", "shutUpTime" to now + 3600),
                            "u_12" to obj("uin" to "12", "nick" to "expired", "shutUpTime" to now - 1),
                            "u_13" to obj("uin" to "13", "nick" to "normal", "shutUpTime" to 0)
                        )
                    )
                )
            )
        }
        val result =
            Json.convertValue<List<QQNativeGroupApiExpand.GroupShut>>(f.call("get_group_shut_list", "group_id" to "42"))
        assertEquals(11, result.single().userId)
        assertEquals(now + 3600, result.single().shutUpTime)
    }

    @Test
    fun markReadLocatesTheStoredConversationAndRejectsAmbiguousIds(): Unit = runBlocking {
        val f = Fixture()
        f.history.put(HistoryKey("9", 2, "42", "message"), j(obj()))
        f.history.put(HistoryKey("other-account", 1, "u_99", "message"), j(obj()))
        f.call("mark_msg_as_read", "message_id" to "message")
        assertEquals("setMsgRead", f.calls.last().first)
        assertEquals("42", f.calls.last().second[0].s("peerUid"))
        f.history.put(HistoryKey("9", 1, "u_11", "message"), j(obj()))
        f.calls.clear()
        assertFailsWith<IllegalStateException> { f.call("mark_msg_as_read", "message_id" to "message") }
        assertTrue(f.calls.isEmpty())
        f.call("mark_msg_as_read", "message_id" to "message", "group" to false)
        assertEquals("u_11", f.calls.last().second[0].s("peerUid"))
        assertEquals(1, f.calls.last().second[0].i("chatType"))
    }

    @Test
    fun markReadValidatesUnrecordedMessagesInExplicitPeers(): Unit = runBlocking {
        val f = Fixture()
        f.respond = { method, _ ->
            if (method == "getMsgsByMsgId") j(
                obj(
                    "msgList" to listOf(
                        obj(
                            "msgId" to "m",
                            "chatType" to 2,
                            "peerUid" to "42"
                        )
                    )
                )
            )
            else j(obj("result" to 0))
        }
        f.call("mark_msg_as_read", "message_id" to "m", "group" to true, "target" to "42")
        assertEquals(listOf("getMsgsByMsgId", "setMsgRead"), f.calls.map { it.first })
        f.calls.clear()
        assertFailsWith<IllegalStateException> {
            f.call("mark_msg_as_read", "message_id" to "m", "group" to true, "target" to "99")
        }
        assertFalse(f.calls.any { it.first == "setMsgRead" })
        f.calls.clear()
        assertFailsWith<IllegalStateException> { f.call("mark_msg_as_read", "message_id" to "unknown") }
        assertTrue(f.calls.isEmpty())
    }

}
