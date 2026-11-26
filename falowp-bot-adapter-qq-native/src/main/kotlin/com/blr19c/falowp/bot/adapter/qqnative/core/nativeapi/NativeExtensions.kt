package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.history.HistoryKey
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MemoryMessageHistory
import com.blr19c.falowp.bot.adapter.qqnative.core.history.MessageHistory
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

/**
 * 将公共、私聊和群聊扩展动作分派到对应的原生服务
 */
internal class NativeExtensions(
    private val bridge: NativeBridge,
    private val contacts: NativeContacts,
    private val account: JsonNode,
    private val history: MessageHistory = MemoryMessageHistory()
) {
    val callbacks = NativeCallbacks()
    private val web = ExtensionWeb(bridge, account.s("id"))
    private val groupFiles = NativeGroupFiles(bridge)
    private val friendList = NativeFriendList(bridge, account.s("id"))
    private val album = NativeAlbum(bridge, web, account.s("id"))
    private fun defaults(name: String): ObjectNode =
        javaClass.getResourceAsStream("/qq-native/defaults/$name.json")
            .use { mapper.readTree(checkNotNull(it)) as ObjectNode }

    private suspend fun notifications(doubt: Boolean, count: Int = 100): List<JsonNode> =
        callbacks.await(
            "onGroupSingleScreenNotifies",
            { it.firstOrNull()?.asBoolean() == doubt }) {
            bridge.invoke(
                "group",
                "getSingleScreenNotifies",
                doubt,
                "",
                count
            )
        }[2].toList()

    private suspend fun message(group: String, id: String): JsonNode {
        require(group.isNotBlank() && id.isNotBlank()) { "需要群号和消息 ID" }
        return bridge.msg("getMsgsByMsgId", Peer(2, group), listOf(id)).list("msgList")
            .firstOrNull { it.s("msgId") == id } ?: error("消息不存在")
    }

    private suspend fun readPeer(args: JsonNode): Peer {
        val id = args.s("message_id")
        val target = args.s("target")
        val group = args.path("group").takeIf { it.isBoolean }?.asBoolean()
        val explicit = target.takeIf { it.isNotBlank() }?.let {
            Peer(if (group == true) 2 else 1, if (group == true) it else contacts.uid(it))
        }
        if (id.isBlank()) return checkNotNull(explicit) { "需要消息 ID 或目标会话" }
        val matches = history.find(account.s("id"), id).filter {
            (group == null || (it.chatType == 2) == group) &&
                    (explicit == null || (it.chatType == explicit.chatType && it.peer == explicit.peerUid))
        }
        check(matches.size <= 1) { "消息 ID 对应多个会话，请指定群号或 QQ 号" }
        matches.singleOrNull()?.let { return Peer(it.chatType, it.peer) }
        if (explicit != null) {
            val rows = bridge.msg("getMsgsByMsgId", explicit, listOf(id)).list("msgList")
            check(rows.any { it.s("msgId") == id && it.i("chatType") == explicit.chatType && it.s("peerUid") == explicit.peerUid }) {
                "目标会话中未找到消息 $id"
            }
            return explicit
        }
        error("未记录消息 $id 所属会话，请指定群号或 QQ 号")
    }

    private suspend fun unit(service: String, method: String, vararg args: Any?): JsonNode {
        val result = bridge.invoke(service, method, *args)
        checkResult(result, method)
        return j(null)
    }

    private suspend fun detail(group: String): JsonNode =
        callbacks.await("onGroupDetailInfoChange", { it.firstOrNull()?.s("groupCode") == group }) {
            bridge.invoke("group", "getGroupDetailInfo", group, 2)
        }.first()

    private fun group(row: JsonNode) = j(
        obj(
            "group_id" to row.s("groupCode"),
            "group_name" to row.s("groupName"),
            "member_count" to row.path("memberNum").asLong(row.l("memberCount")),
            "max_member_count" to row.path("maxMemberNum").asLong(row.l("maxMember")),
            "group_all_shut" to if (row.path("shutUpAllTimestamp")
                    .asLong(row.l("groupShutupExpireTime")) > 0
            ) -1 else 0,
            "group_remark" to row.s("remarkName")
        )
    )

    private suspend fun friend(row: JsonNode, category: Int): JsonNode {
        val core = row.path("coreInfo")
        val base = row.path("baseInfo")
        return j(
            obj(
                "user_id" to core.s("uin").ifEmpty { contacts.uin(core.s("uid")) },
                "nickname" to core.s("nick"),
                "remark" to core.s("remark"),
                "category_id" to category,
                "birthday_year" to base.path("birthday_year"),
                "birthday_month" to base.path("birthday_month"),
                "birthday_day" to base.path("birthday_day"),
                "age" to base.path("age"),
                "phone_num" to base.s("phoneNum"),
                "email" to base.s("eMail"),
                "sex" to when (base.i("sex")) {
                    1 -> "male"
                    2 -> "female"
                    else -> "unknown"
                },
                "level" to row.path("qqLevel").path("qqLevel")
            )
        )
    }

    private suspend fun friends(categorized: Boolean, fresh: Boolean = true): JsonNode {
        val categories = bridge.invoke("buddy", "getBuddyListV2", "0", fresh, 0).list("data")
        val uids = categories.flatMap { it.list("buddyUids").map { uid -> uid.asString() } }
        val profiles = bridge.invoke("profile", "getCoreAndBaseInfo", "nodeStore", uids).mapEntries().toMap()
        val groups = categories.map { c ->
            val rows = c.list("buddyUids").mapNotNull { profiles[it.asString()] }.map { friend(it, c.i("categoryId")) }
            obj(
                "categoryId" to c.i("categoryId"),
                "categoryName" to c.s("categroyName"),
                "categoryMbCount" to rows.size,
                "buddyList" to rows
            )
        }
        return j(if (categorized) groups else groups.flatMap { it["buddyList"] as List<*> })
    }

    private suspend fun members(group: String, fresh: Boolean): List<JsonNode> {
        val response = bridge.invoke("group", "getAllMemberList", group, fresh)
        check(response.i("errCode") == 0 && response.path("result").has("infos")) { "读取群成员失败" }
        return response.path("result").path("infos").mapEntries().map { (uid, row) ->
            j(
                obj(
                    "group_id" to group,
                    "user_id" to row.s("uin").ifEmpty { contacts.uin(uid) },
                    "nickname" to row.s("nick"),
                    "card" to row.s("cardName"),
                    "role" to when (row.i("role")) {
                        4 -> "owner"
                        3 -> "admin"
                        else -> "member"
                    },
                    "title" to row.s("memberSpecialTitle"),
                    "is_robot" to row.path("isRobot").asBoolean(false),
                    "shut_up_timestamp" to row.l("shutUpTime"),
                    "sex" to when (row.i("sex")) {
                        1 -> "male"
                        2 -> "female"
                        else -> "unknown"
                    },
                    "age" to row.path("age"),
                    "level" to row.path("memberRealLevel"),
                    "qq_level" to row.path("qqLevel").path("qqLevel"),
                    "join_time" to row.path("joinTime"),
                    "last_sent_time" to row.path("lastSpeakTime")
                )
            )
        }
    }

    private suspend fun oidb(command: Int, sub: Int, body: ByteArray, uid: Boolean = true): JsonNode {
        val result = bridge.packet(
            "OidbSvcTrpcTcp.0x${command.toString(16).uppercase()}_$sub",
            cat(n(1, command), n(2, sub), m(4, body), n(12, if (uid) 1 else 0))
        )
        check(u(result, 3) == 0uL) { "QQ 操作失败：${u(result, 3)} ${s(result, 5)}" }
        return j(null)
    }

    suspend fun call(operation: String, args: JsonNode): JsonNode {
        if (operation.startsWith("group_files_")) return groupFiles.call(operation, args)
        val group = args.s("group_id")
        suspend fun uid() = contacts.uid(args.s("user_id"))
        return when (operation) {
            "music_card" -> NativeMusic.card(args)
            "original_message" -> {
                val peer = readPeer(args)
                val id = args.s("message_id")
                require(id.isNotBlank()) { "缺少消息 ID" }
                val stored = history.get(HistoryKey(account.s("id"), peer.chatType, peer.peerUid, id))
                val row = stored?.path("raw")?.takeIf { it.list("elements").isNotEmpty() }
                    ?: bridge.msg("getMsgsByMsgId", peer, listOf(id)).list("msgList")
                        .firstOrNull { it.s("msgId") == id }
                check(row != null && row.list("elements").isNotEmpty()) { "未找到消息原文" }
                row.deepCopy()
            }

            "get_mini_app_ark" -> bridge.miniApp(args)
            "_del_group_notice" -> unit(
                "group",
                "deleteGroupBulletin",
                group,
                web.pskey("qun.qq.com"),
                args.s("notice_id")
            )

            "_get_group_notice" -> {
                val auth = web.cookies("qun.qq.com")
                val token = ExtensionWeb.csrf("qun.qq.com", auth)
                val result = web.request(
                    "https://web.qun.qq.com/cgi-bin/announce/get_t_list?bkn=$token&qid=$group&ft=23&ni=1&n=20&i=1&log_read=1&platform=1&s=-1",
                    cookies = auth
                )
                j((result.list("feeds") + result.list("inst")).distinctBy { it.s("fid") }.map { row ->
                    val images = row.path("msg").list("pics")
                        .map { obj("id" to it.s("id"), "height" to it.i("h"), "width" to it.i("w")) }
                    obj(
                        "notice_id" to row.s("fid"),
                        "sender_id" to row.s("u"),
                        "publish_time" to row.l("pubt"),
                        "message" to obj("text" to row.path("msg").s("text"), "image" to images, "images" to images),
                        "settings" to row.path("settings"),
                        "read_num" to row.l("read_num")
                    )
                })
            }

            "get_essence_msg_list" -> {
                val auth = web.cookies("qun.qq.com")
                val token = ExtensionWeb.csrf("qun.qq.com", auth)
                val result = mutableListOf<JsonNode>()
                var page = 0
                while (true) {
                    val response = web.request(
                        "https://qun.qq.com/cgi-bin/group_digest/digest_list?bkn=$token&page_start=$page&page_limit=50&group_code=$group",
                        cookies = auth
                    ).path("data")
                    result.addAll(response.list("msg_list"))
                    if (response.path("is_end").asBoolean(false) || response.list("msg_list").isEmpty()) break
                    check(++page < 1000) { "精华分页超限" }
                }
                j(result.map { row ->
                    val native = bridge.msg("getSingleMsg", Peer(2, group), row.s("msg_seq")).list("msgList")
                        .firstOrNull { it.s("msgSeq") == row.s("msg_seq") }
                    obj(
                        "message_id" to native?.s("msgId"),
                        "sender_id" to row.s("sender_uin"),
                        "operator_id" to row.s("add_digest_uin"),
                        "operator_time" to row.l("add_digest_time"),
                        "content" to row.path("msg_content"),
                        "msg_seq" to row.s("msg_seq"),
                        "msg_random" to row.s("msg_random")
                    )
                })
            }

            "get_cookies" -> {
                val cookies = web.cookies(args.s("domain"))
                j(
                    obj(
                        "cookies" to cookies.entries.joinToString("; ") { "${it.key}=${it.value}" },
                        "bkn" to ExtensionWeb.csrf(args.s("domain"), cookies).toString()
                    )
                )
            }

            "set_qq_avatar" -> withExtensionFile(args.s("file")) { path ->
                unit(
                    "profile",
                    "setHeader",
                    path.toString()
                )
            }

            "set_friend_add_request" -> {
                val rows = callbacks.await("onBuddyReqChange") { bridge.invoke("buddy", "getBuddyReq") }.first()
                    .list("buddyReqs")
                val row = rows.firstOrNull { it.s("reqTime") == args.s("flag") } ?: error("好友申请不存在")
                val ack = bridge.invoke(
                    "buddy",
                    "approvalFriendRequest",
                    obj(
                        "friendUid" to row.s("friendUid"),
                        "reqTime" to row.s("reqTime"),
                        "accept" to args.path("approve").asBoolean()
                    )
                )
                if (!ack.isNull) checkResult(ack)
                if (args.s("remark").isNotBlank() && args.path("approve").asBoolean()) unit(
                    "buddy",
                    "setBuddyRemark",
                    obj("uid" to row.s("friendUid"), "remark" to args.s("remark"))
                )
                j(null)
            }

            "get_unidirectional_friend_list" -> friendList.all()
            "get_unidirectional_friend_page" -> friendList.page(args.s("cookie"), args.i("count"))

            "get_friend_list" -> friends(false, args.path("no_cache").asBoolean(true))
            "get_friends_with_category" -> friends(true)
            "set_friend_remark" -> unit("buddy", "setBuddyRemark", obj("uid" to uid(), "remark" to args.s("remark")))
            "send_like" -> {
                require(args.l("times") in 1..20)
                unit(
                    "like",
                    "setBuddyProfileLike",
                    obj(
                        "friendUid" to uid(),
                        "sourceId" to 71,
                        "doLikeCount" to args.l("times"),
                        "doLikeTollCount" to 0
                    )
                )
            }

            "get_profile_like" -> {
                val own = args.s("user_id").isEmpty() || args.s("user_id") == account.s("id")
                val reply = bridge.invoke(
                    "like",
                    "getBuddyProfileLike",
                    obj(
                        "friendUids" to listOf(if (own) account.s("uid") else uid()),
                        "basic" to 1,
                        "vote" to 1,
                        "favorite" to 0,
                        "userProfile" to 1,
                        "type" to if (own) 2 else 1,
                        "start" to args.l("start"),
                        "limit" to args.l("count")
                    )
                )
                val data = reply.path("info").list("userLikeInfos").firstOrNull() ?: error("QQ 未返回点赞资料")
                for (entry in data.path("voteInfo").list("userInfos") + data.path("favoriteInfo").list("userInfos")) {
                    (entry as ObjectNode).set("uin", j(contacts.uin(entry.s("uid")).toLongOrNull()))
                }
                data
            }

            "set_online_status" -> unit(
                "msg",
                "setStatus",
                obj(
                    "status" to args.i("status"),
                    "extStatus" to args.i("ext_status"),
                    "batteryStatus" to args.i("battery_status")
                )
            )

            "set_diy_online_status" -> unit(
                "msg",
                "setStatus",
                obj(
                    "status" to 10,
                    "extStatus" to 2000,
                    "batteryStatus" to 0,
                    "customStatus" to obj(
                        "faceId" to args.s("face_id"),
                        "wording" to args.s("wording"),
                        "faceType" to args.s("face_type")
                    )
                )
            )

            "set_self_longnick" -> unit("profile", "setLongNick", args.s("longNick"))
            "get_recent_contact" -> {
                require(args.l("count") in 1..1000)
                val rows = bridge.invoke("recent", "getRecentContactListSnapShot", args.l("count")).path("info")
                    .list("changedList")
                j(rows.map { row ->
                    val msg =
                        bridge.msg("getMsgsByMsgId", Peer(row.i("chatType"), row.s("peerUid")), listOf(row.s("msgId")))
                            .list("msgList").firstOrNull()
                    obj(
                        "lastestMsg" to msg,
                        "peerUin" to row.s("peerUin"),
                        "remark" to row.s("remark"),
                        "msgTime" to row.s("msgTime"),
                        "chatType" to row.i("chatType"),
                        "msgId" to row.s("msgId"),
                        "sendNickName" to row.s("sendNickName"),
                        "sendMemberName" to row.s("sendMemberName"),
                        "peerName" to row.s("peerName")
                    )
                })
            }

            "get_group_list" -> j(
                callbacks.await(
                    "onGroupListUpdate",
                    { it.firstOrNull()?.asInt() in listOf(0, 1) }) {
                    bridge.invoke(
                        "group",
                        "getGroupList",
                        args.path("no_cache").asBoolean(true)
                    )
                }[1].toList().map { group(it) })

            "get_group_info", "get_group_detail_info" -> group(detail(group))
            "get_group_info_ex" -> detail(group)
            "get_group_member_list" -> j(members(group, args.path("no_cache").asBoolean(true)))
            "get_group_member_info" -> members(
                group,
                args.path("no_cache").asBoolean(true)
            ).firstOrNull { it.s("user_id") == args.s("user_id") } ?: error("群成员不存在")

            "set_group_add_request" -> {
                var doubt = false
                var row = notifications(
                    false,
                    args.i("count").takeIf { it > 0 } ?: 100).firstOrNull { it.s("seq") == args.s("flag") }
                if (row == null) {
                    doubt = true
                    row = notifications(
                        true,
                        args.i("count").takeIf { it > 0 } ?: 100).firstOrNull { it.s("seq") == args.s("flag") }
                }
                checkNotNull(row) { "加群申请不存在" }
                unit(
                    "group",
                    "operateSysNotify",
                    doubt,
                    obj(
                        "operateType" to if (args.path("approve").asBoolean()) 1 else 2,
                        "targetMsg" to obj(
                            "seq" to row.s("seq"),
                            "type" to row.i("type"),
                            "groupCode" to row.path("group").s("groupCode"),
                            "postscript" to args.s("reason").ifBlank { " " })
                    )
                )
            }

            "get_group_ignore_add_request", "get_group_ignored_notifies" -> {
                val rows = notifications(operation == "get_group_ignore_add_request")
                suspend fun mapped(row: JsonNode) = obj(
                    "request_id" to row.s("seq"),
                    "invitor_uin" to (contacts.uin(row.path("user1").s("uid")).toLongOrNull() ?: 0L),
                    "invitor_nick" to row.path("user1").s("nickName"),
                    "group_id" to row.path("group").s("groupCode"),
                    "message" to row.s("postscript"),
                    "group_name" to row.path("group").s("groupName"),
                    "checked" to (row.i("status") != 1),
                    "actor" to (contacts.uin(row.path("user2").s("uid")).toLongOrNull() ?: 0L),
                    "requester_nick" to row.path("user1").s("nickName"),
                    "invitor_uid" to row.path("user1").s("uid"),
                    "actor_uid" to row.path("user2").s("uid")
                )
                if (operation == "get_group_ignore_add_request") j(rows.map { mapped(it) }) else j(obj("invited_requests" to rows.filter {
                    it.i(
                        "type"
                    ) == 1
                }.map { mapped(it) }, "join_requests" to rows.filter { it.i("type") == 7 }.map { mapped(it) }))
            }

            "get_group_shut_list" -> {
                val now = System.currentTimeMillis() / 1000
                j(members(group, true).filter { it.l("shut_up_timestamp") > now }.map {
                    obj(
                        "user_id" to it.s("user_id"),
                        "nickname" to it.s("nickname"),
                        "shut_up_time" to it.l("shut_up_timestamp")
                    )
                })
            }

            "set_group_todo" -> {
                val seq = args.s("message_seq").ifEmpty { message(group, args.s("message_id")).s("msgSeq") }
                oidb(0xf90, 1, cat(n(1, group.toLong()), cat(Wire.varint(16u), Wire.varint(seq.toULong()))))
            }

            "set_essence_msg", "delete_essence_msg" -> {
                val row = if (args.s("msg_seq").isBlank()) message(group, args.s("message_id")) else null
                unit(
                    "group",
                    if (operation == "set_essence_msg") "addGroupEssence" else "removeGroupEssence",
                    obj(
                        "groupCode" to group,
                        "msgRandom" to (row?.s("msgRandom") ?: args.s("msg_random")).toLong(),
                        "msgSeq" to (row?.s("msgSeq") ?: args.s("msg_seq")).toLong()
                    )
                )
            }

            "set_group_add_option", "set_group_search" -> {
                val request = defaults("createGroupDetailInfoV2Param")
                request.put("groupCode", group)
                val filter = request.path("filter") as ObjectNode
                val values = request.path("modifyInfo") as ObjectNode
                fun field(name: String, value: JsonNode) {
                    filter.put(name, 1)
                    values.set(name, value)
                }
                if (operation == "set_group_add_option") {
                    val type = args.i("add_type")
                    require(type in 0..5)
                    field("addOption", j(type))
                    if (type == 4 || type == 5) {
                        field("groupQuestion", j(args.s("group_question")))
                        if (type == 4) field("groupAnswer", j(args.s("group_answer")))
                    }
                } else {
                    if (args.hasValue("no_code_finger_open")) field(
                        "noCodeFingerOpenFlag",
                        args.path("no_code_finger_open")
                    )
                    if (args.hasValue("no_finger_open")) field("noFingerOpenFlag", args.path("no_finger_open"))
                }
                unit("group", "modifyGroupDetailInfoV2", request, 0)
            }

            "set_group_robot_add_option" -> {
                val request = defaults("createGroupExtInfo")
                request.put("groupCode", group)
                // 保留当前 NTQQ 返回的完整 ext 结构
                val current = detail(group).path("groupExt")
                check(current.isObject) { "QQ 未返回完整群扩展资料" }
                val values = current.deepCopy() as ObjectNode
                request.set("extInfo", values)
                val filter = defaults("createGroupExtFilter")
                values.properties().forEach { (name, _) -> if (!filter.has(name)) filter.put(name, 0) }
                for ((key, field) in listOf(
                    "robot_member_switch" to "inviteRobotMemberSwitch",
                    "robot_member_examine" to "inviteRobotMemberExamine"
                )) if (args.hasValue(key)) {
                    filter.put(field, 1)
                    values.set(field, args.path(key))
                }
                unit("group", "modifyGroupExtInfoV2", request, filter)
            }

            "get_qun_album_list", "get_group_album_media_list", "del_group_album_media", "do_group_album_comment", "set_group_album_media_like", "upload_image_to_qun_album" -> album.call(
                operation,
                args
            )

            "set_group_remark" -> unit("group", "modifyGroupRemark", group, args.s("remark"))
            "set_group_name" -> unit("group", "modifyGroupName", group, args.s("group_name"), false)
            "set_group_card" -> unit("group", "modifyMemberCardName", group, uid(), args.s("card"))
            "set_group_admin" -> unit(
                "group",
                "modifyMemberRole",
                group,
                uid(),
                if (args.path("enable").asBoolean()) 3 else 2
            )

            "set_group_ban" -> {
                require(args.l("duration") in 0..2592000)
                unit("group", "setMemberShutUp", group, listOf(obj("uid" to uid(), "timeStamp" to args.l("duration"))))
            }

            "set_group_whole_ban" -> unit("group", "setGroupShutUp", group, args.path("enable").asBoolean(true))
            "set_group_kick", "set_group_kick_members" -> unit(
                "group",
                "kickMember",
                group,
                (if (args.path("user_id").isArray) args.list("user_id")
                    .map { it.asString() } else listOf(args.s("user_id"))).map { contacts.uid(it) },
                args.path("reject_add_request").asBoolean(false),
                ""
            )

            "set_group_leave" -> unit(
                "group",
                if (args.path("is_dismiss").asBoolean(false)) "destroyGroup" else "quitGroup",
                group
            )

            "set_group_special_title" -> oidb(
                0x8fc,
                2,
                cat(
                    n(1, group.toLong()),
                    m(3, m(1, uid()), m(5, args.s("special_title")), n(6, -1), m(7, args.s("special_title")))
                ),
                false
            )

            "set_group_sign", "send_group_sign" -> oidb(
                0xeb7,
                1,
                m(2, m(1, account.s("id")), m(2, group), m(3, "9.0.90")),
                false
            )

            "ark_share_group", "ark_share_peer" -> {
                var result = if (operation == "ark_share_group") bridge.invoke(
                    "group",
                    "getGroupRecommendContactArkJson",
                    group
                ) else bridge.invoke("buddy", "getBuddyRecommendContactArkJson", args.s("user_id"), "")
                if (operation == "ark_share_group" && result.i("errCode") == 1004 && result.s("errMsg")
                        .contains("invalid UTF-8")
                ) {
                    // 原生签名接口可能短暂失败，重新获取一次真实卡片后再返回结果
                    detail(group)
                    result = bridge.invoke("group", "getGroupRecommendContactArkJson", group)
                }
                val code = if (result.has("errCode")) result.i("errCode") else result.i("result")
                check(code == 0) { "QQ 分享卡片失败 ($code)：${result.s("errMsg")}" }
                val content = if (operation == "ark_share_group") result.s("arkJson") else result.s("arkMsg")
                check(content.isNotBlank()) { "QQ 未返回分享卡片" }
                mapper.readTree(content)
            }

            "delete_msg", "set_msg_emoji_like", "fetch_emoji_like", "get_emoji_likes", "forward_single_msg", "set_input_status" -> {
                val peer = Peer(
                    if (args.path("group").asBoolean(false)) 2 else 1,
                    if (args.path("group").asBoolean(false)) args.s("target") else contacts.uid(args.s("target"))
                )
                when (operation) {
                    "delete_msg" -> {
                        callbacks.await(
                            "onMsgInfoListUpdate",
                            { rows ->
                                rows.firstOrNull()?.toList()?.any {
                                    it.s("msgId") == args.s("message_id") && it.s("peerUid") == peer.peerUid && it.s("recallTime")
                                        .let { time -> time.isNotBlank() && time != "0" }
                                } == true
                            }) { bridge.msg("recallMsg", peer, listOf(args.s("message_id"))) }
                        j(null)
                    }

                    "set_input_status" -> unit(
                        "msg",
                        "sendShowInputStatusReq",
                        peer.chatType,
                        args.i("event_type"),
                        peer.peerUid
                    )

                    "forward_single_msg" -> {
                        val to = Peer(
                            if (args.path("to_group").asBoolean()) 2 else 1,
                            if (args.path("to_group").asBoolean()) args.s("to") else contacts.uid(args.s("to"))
                        )
                        unit("msg", "forwardMsg", listOf(args.s("message_id")), peer, listOf(to), nativeMap())
                    }

                    else -> {
                        val row = bridge.msg("getMsgsByMsgId", peer, listOf(args.s("message_id"))).list("msgList")
                            .firstOrNull() ?: error("消息不存在")
                        val emoji = args.s("emoji_id")
                        val type = args.s("emoji_type").ifBlank { if (emoji.length > 3) "2" else "1" }
                        if (operation == "set_msg_emoji_like") unit(
                            "msg",
                            "setMsgEmojiLikes",
                            peer,
                            row.s("msgSeq"),
                            emoji,
                            type,
                            args.path("set").asBoolean(true)
                        )
                        else bridge.msg(
                            "getMsgEmojiLikesList",
                            peer,
                            row.s("msgSeq"),
                            emoji,
                            type,
                            args.s("cookie"),
                            false,
                            args.i("count").takeIf { it > 0 } ?: 20).also { checkResult(it) }
                    }
                }
            }

            "click_inline_keyboard_button" -> unit(
                "msg",
                "clickInlineKeyboardButton",
                obj(
                    "buttonId" to args.s("button_id"),
                    "peerId" to group,
                    "botAppid" to args.s("bot_appid"),
                    "msgSeq" to args.s("msg_seq"),
                    "callback_data" to args.s("callback_data"),
                    "dmFlag" to 0,
                    "chatType" to 2
                )
            )

            "mark_msg_as_read" -> unit("msg", "setMsgRead", readPeer(args))
            "mark_all_as_read" -> unit("msg", "setAllC2CAndGroupMsgRead")
            else -> error("尚未实现扩展：$operation")
        }
    }
}
