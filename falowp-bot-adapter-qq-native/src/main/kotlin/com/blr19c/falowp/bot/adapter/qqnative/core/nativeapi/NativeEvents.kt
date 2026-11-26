package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.history.*
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.security.MessageDigest

/**
 * 保存并解析原生消息回调，将消息与通知分发给适配器
 */
internal class NativeEvents(
    private val account: JsonNode,
    private val contacts: NativeContacts,
    private val messages: NativeMessages,
    private val archive: MessageHistory = MemoryMessageHistory(),
    private val onMessage: (JsonNode) -> Unit,
    private val onNotice: (JsonNode) -> Unit
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val history = linkedMapOf<String, JsonNode>()
    private val notices = linkedSetOf<String>()
    private val notifications =
        NativeNotifications(contacts) { key, data -> notice(key, data, "native") }
    private val transitions = linkedMapOf<String, Pair<String, Long>>()
    private suspend fun notice(key: String, data: JsonNode, origin: String = "gray") {
        if (data.s("actor").isBlank() || data.s("target").isBlank() || !notices.add(key)) return
        if (notices.size > 8192) notices.remove(notices.first())
        if (data.s("kind") in listOf("join", "leave")) {
            val identity = "${data.s("kind")}/${data.s("target")}/${data.s("member")}"
            val time = System.currentTimeMillis()
            transitions[identity]?.let { (previous, at) -> if (previous != origin && time - at < 10_000) return }
            transitions.remove(identity)
            transitions[identity] = origin to time
            if (transitions.size > 8192) transitions.remove(transitions.keys.first())
        }
        val enriched = data.deepCopy() as ObjectNode
        val group = data.s("target").takeIf { data.s("source") == "group" }
        if (group != null && data.s("kind") in listOf("groupEssence", "messageReaction")) {
            val original = optional("通知消息原文") {
                val sequence = data.s("sequence")
                val cached =
                    history.values.firstOrNull { it.s("target") == group && it.s("source") == "group" && it.s("seq") == sequence }
                cached ?: messages.bridge.msg("getSingleMsg", Peer(2, group), sequence).list("msgList")
                    .firstOrNull { it.s("msgSeq") == sequence && it.i("chatType") == 2 && it.s("peerUid") == group }
                    ?.let { row ->
                        enriched.put("messageId", row.s("msgId"))
                        archived(HistoryKey(account.s("id"), 2, group, row.s("msgId")))
                            ?: row.takeIf { it.list("elements").isNotEmpty() }?.let { messages.decode(it) }
                    }
            }
            if (original != null) {
                enriched.set("message", original)
                enriched.put("messageId", original.s("id"))
                if (data.s("kind") == "groupEssence" && data.s("member") in listOf("", "0"))
                    enriched.put("member", original.s("sender"))
            }
        }
        if (group != null && data.s("kind") == "groupFileUpload") {
            val url = optional("群文件下载地址") {
                NativeGroupFiles(messages.bridge).call(
                    "group_files_url",
                    j(obj("group_id" to group, "file_id" to data.s("fileId")))
                ).asString()
            }
            if (url != null) enriched.put("url", url)
        }
        enriched.set("actorInfo", contacts.profile(data.s("actor"), group, data.s("actorName")))
        if (enriched.s("member").isNotBlank()) enriched.set(
            "memberInfo",
            contacts.profile(enriched.s("member"), group, data.s("memberName"))
        )
        onNotice(enriched)
    }

    private suspend fun <T> optional(name: String, block: suspend () -> T): T? = try {
        block()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        log.warn("QQNative {}查询失败，保留通知基本信息", name, error)
        null
    }

    /**
     * 接收其他成员入群或退群的原生回调；即使没有会话灰条提示也能生成事件
     */
    suspend fun system(bytes: ByteArray) {
        notifications.system(bytes)
        val head = Wire.b(bytes, 2)
        val type = Wire.u(head, 1).toInt()
        if (type !in listOf(33, 34)) return
        val change = Wire.b(Wire.b(bytes, 3), 2)
        val group = Wire.u(change, 1).toString()
        if (group == "0") return
        val reason = Wire.u(change, 4).toInt()
        val memberUid =
            Wire.s(change, 3).ifBlank { if (type == 34 && reason in listOf(3, 129)) account.s("uid") else "" }
        if (memberUid.isBlank()) return
        val member = contacts.uin(memberUid)
        val operator = Wire.b(change, 5)
        val parsed = if (operator.firstOrNull() == 10.toByte()) runCatching {
            Wire.s(
                Wire.b(operator, 1),
                1
            )
        }.getOrDefault("") else ""
        val operatorUid =
            parsed.ifBlank { text(operator).takeIf { it.matches(Regex("(u_[A-Za-z0-9_-]+|[0-9]+)")) }.orEmpty() }
        val actor = operatorUid.takeIf { it.isNotBlank() }?.let { contacts.uin(it) }
            ?: if (type == 34 && reason == 130) member else "0"
        val action = if (type == 33) if (reason == 131) "invite" else "approve" else when (reason) {
            130 -> "leave"
            3 -> "kick_me"
            129 -> "disband"
            else -> if (member == account.s("id")) "kick_me" else "kick"
        }
        val key = "system/" + MessageDigest.getInstance("SHA-256").digest(bytes).hex()
        notice(
            key, j(
                obj(
                    "kind" to if (type == 33) "join" else "leave", "source" to "group", "target" to group,
                    "actor" to actor, "member" to member, "action" to action
                )
            ), "system"
        )
    }

    suspend fun request(kind: String, row: JsonNode) {
        val data = if (kind == "friendRequest") {
            if (row.path("isInitiator").asBoolean(false) || row.path("isDecide")
                    .asBoolean(false) || (row.hasValue("reqType") && row.i("reqType") != 1)
            ) return
            val actor = contacts.uin(row.s("friendUin").ifEmpty { row.s("friendUid") })
            j(
                obj(
                    "kind" to kind,
                    "source" to "private",
                    "actor" to actor,
                    "target" to actor,
                    "flag" to row.s("reqTime").ifEmpty { row.s("reqId") },
                    "comment" to row.s("extWords").ifEmpty { row.s("extMsg") })
            )
        } else {
            val invite = row.i("type") == 1
            if (row.i("status") != 1 || row.i("type") !in listOf(1, 5, 7)) return
            val applicant = row.path(if (invite) "user2" else "user1")
            j(
                obj(
                    "kind" to kind,
                    "source" to "group",
                    "target" to row.path("group").s("groupCode"),
                    "flag" to row.s("seq"),
                    "actor" to contacts.uin(applicant.s("uin").ifEmpty { applicant.s("uid") }),
                    "action" to if (invite) "invite" else "add",
                    "comment" to row.s("postscript")
                )
            )
        }
        notice(listOf(kind, data.s("target"), data.s("actor"), data.s("flag")).joinToString("/"), data)
    }

    suspend fun callback(name: String, args: List<JsonNode>, key: String) {
        when (name) {
            "onInputStatusPush" -> args.firstOrNull()?.let { notifications.input(it, key) }
            "onMemberInfoChange" -> if (args.size >= 3) notifications.members(args[0].asString(), args[2])
        }
    }

    suspend fun receive(row: JsonNode, updatesOnly: Boolean) {
        if (row.i("chatType") !in listOf(1, 2) || row.s("msgId").isBlank()) return
        val key = "${row.i("chatType")}/${row.s("peerUid")}/${row.s("msgId")}"
        val storageKey = HistoryKey(account.s("id"), row.i("chatType"), row.s("peerUid"), row.s("msgId"))
        val revoked = row.list("elements").any { it.path("grayTipElement").hasValue("revokeElement") }
        // 先保存 NTQQ 原始消息，再解析内容或下载媒体，保留尚未支持的消息元素
        if (!revoked && !history.containsKey(key) && row.list("elements").isNotEmpty()) {
            archive.put(storageKey, j(obj("raw" to row)))
        }
        notifications.message(row, key, updatesOnly)
        for (e in row.list("elements")) {
            if (e.hasValue("grayTipElement")) {
                val gray = e.path("grayTipElement")
                val original = if (gray.hasValue("revokeElement")) history[key] ?: archived(storageKey) else null
                gray(row, gray, original)?.let { notice("${it.s("kind")}/$key", it) }
            }
        }
        // 撤回通知的灰条不能覆盖已保存的消息原文
        if (revoked) return
        val own = row.s("senderUid") == account.s("uid") || row.s("senderUin") == account.s("id")
        if (history.containsKey(key) || (updatesOnly && !(own && row.i("sendStatus") in listOf(
                2,
                3
            ))) || row.list("elements").isEmpty()
        ) return
        val data = messages.decode(row)
        if (data.list("parts").isNotEmpty() && data.s("target").isNotBlank() && data.s("sender").isNotBlank()) {
            history[key] = data
            if (history.size > 8192) history.remove(history.keys.first())
            try {
                archive.put(storageKey, j(obj("raw" to row, "decoded" to data)))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                log.error("QQNative 解析结果写入失败，保留此前保存的原始回调和当前内存记录", error)
            }
            if (!own) onMessage(data)
        }
    }

    private suspend fun archived(key: HistoryKey): JsonNode? = try {
        archive.get(key)?.let { stored ->
            when {
                stored.hasValue("decoded") -> stored.path("decoded")
                stored.hasValue("raw") -> messages.decode(stored.path("raw")).takeIf { it.list("parts").isNotEmpty() }
                else -> null
            }
        }
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        log.error("QQNative 消息记录读取失败", error)
        null
    }

    private suspend fun gray(row: JsonNode, gray: JsonNode, original: JsonNode?): JsonNode? {
        val group = row.i("chatType") == 2
        val target =
            if (group) row.s("peerUid") else contacts.uin(row.s("peerUin").takeIf { it.isNotBlank() && it != "0" }
                ?: row.s("peerUid"))
        val base = obj("source" to if (group) "group" else "private", "target" to target)
        if (gray.hasValue("revokeElement")) {
            val revoke = gray.path("revokeElement")
            val actor = contacts.uin(
                revoke.s("operatorUid")
                    .ifBlank { if (revoke.path("isSelfOperate").asBoolean(false)) account.s("id") else "0" })
            val message = original ?: j(
                base + obj(
                    "id" to row.s("msgId"),
                    "seq" to row.s("msgSeq"),
                    "time" to row.s("msgTime"),
                    "peerUid" to row.s("peerUid"),
                    "sender" to contacts.uin(row.s("senderUin").takeIf { it.isNotBlank() && it != "0" }
                        ?: row.s("senderUid")),
                    "nickname" to row.s("sendNickName"),
                    "role" to 0,
                    "parts" to emptyList<Any>(),
                    "contentAvailable" to false))
            return j(
                base + obj(
                    "kind" to "withdraw",
                    "actor" to actor,
                    "actorName" to revoke.s("operatorMemRemark").ifBlank { revoke.s("operatorNick") },
                    "original" to message
                )
            )
        }
        val g = gray.path("groupElement")
        if (group && g.i("type") in listOf(1, 3)) {
            val add = g.path("memberAdd")
            val show = add.path("showType").asInt(-1)
            if (g.i("type") == 1 && show == 8) return null // 已是群成员，不重复生成入群事件
            val (memberUid, actorUid) = if (g.i("type") == 1) when (show) {
                0 -> add.path("otherAdd").s("uid") to g.s("adminUid")
                1 -> account.s("uid") to g.s("adminUid")
                2 -> add.path("otherAddByOtherQRCode").path("invited").s("uid") to add.path("otherAddByOtherQRCode")
                    .path("inviter").s("uid")

                3 -> add.path("otherAddByYourQRCode").s("uid") to account.s("uid")
                4 -> account.s("uid") to add.path("youAddByOtherQRCode").s("uid")
                5 -> add.path("otherInviteOther").path("invited").s("uid") to add.path("otherInviteOther")
                    .path("inviter").s("uid")

                6 -> account.s("uid") to add.path("otherInviteYou").s("uid")
                7 -> add.path("youInviteOther").s("uid") to account.s("uid")
                else -> g.s("memberUid") to g.s("adminUid")
            } else g.s("memberUid") to g.s("adminUid")
            val member = contacts.uin(memberUid.ifBlank { g.s("memberUid") })
            if (member.isBlank()) return null
            val operator = actorUid.ifBlank { g.s("adminUid") }
            val actor = if (operator.isNotBlank()) contacts.uin(operator) else if (g.i("type") == 3) member else "0"
            val action = if (g.i("type") == 1) if (show in 5..7) "invite" else "approve"
            else if (actor == member) "leave" else if (member == account.s("id")) "kick_me" else "kick"
            return j(
                base + obj(
                    "actor" to actor, "member" to member, "kind" to if (g.i("type") == 1) "join" else "leave",
                    "action" to action, "actorName" to g.s("adminNick"), "memberName" to g.s("memberNick")
                )
            )
        }
        val tip = gray.path("jsonGrayTipElement")
        if (tip.s("busiId") !in listOf("1061", "5143")) return null
        val params = tip.path("xmlToJsonParam").path("templParam").mapEntries().toMap()
        var ids = listOf(params["uin_str1"]?.asString().orEmpty(), params["uin_str2"]?.asString().orEmpty())
        if (ids.any { it.isBlank() }) ids = runCatching {
            mapper.readTree(tip.s("jsonStr")).list("items").map { it.s("uid") }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
        if (ids.size != 2) return null
        return j(base + obj("kind" to "nudge", "actor" to contacts.uin(ids[0]), "member" to contacts.uin(ids[1])))
    }
}
