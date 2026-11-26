package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * 调用原生联系人服务，转换 QQ 号、UID 与群成员资料
 */
internal class NativeContacts(private val bridge: NativeBridge) {
    private val groups = linkedSetOf<String>()
    private val groupLock = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private val identities = linkedMapOf<String, String>()
    private val uins = linkedMapOf<String, String>()

    private data class Member(val role: Int, val expires: Long)

    private val members = linkedMapOf<String, Member>()
    private val memberProfiles = linkedMapOf<String, JsonNode>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Int>>()

    @Synchronized
    fun remember(uin: String, uid: String) {
        if (uin.isEmpty() || uin == "0" || uid.isEmpty()) return
        identities[uin] = uid
        uins[uid] = uin
        if (identities.size > 8192) {
            val first = identities.keys.first()
            uins.remove(identities.remove(first))
        }
    }

    suspend fun uin(id: String): String {
        if (!id.startsWith("u_")) return id
        synchronized(this) { uins[id] }?.let { return it }
        val result = runCatching {
            bridge.invoke(
                "group",
                "getUinByUids",
                listOf(id)
            )
        }.onFailure { if (it is CancellationException) throw it }.getOrNull()
        val uin = result?.path("uins")?.mapEntries()?.firstOrNull { it.first == id }?.second?.asString().orEmpty()
        remember(uin, id)
        return uin.ifEmpty { id }
    }

    suspend fun uid(id: String): String {
        if (id.startsWith("u_")) return id
        synchronized(this) { identities[id] }?.let { return it }
        val result = bridge.invoke("group", "getUidByUins", listOf(id))
        val uid = result.path("uids").mapEntries().firstOrNull { it.first == id }?.second?.asString().orEmpty()
        check(uid.isNotEmpty()) { "无法解析 QQ 用户 $id" }
        remember(id, uid)
        return uid
    }

    @Synchronized
    fun groupUpdate(type: Int, rows: List<JsonNode>) {
        if (type == 0 || type == 1) groups.clear()
        for (row in rows) {
            if (type == 3) groups.remove(row.s("groupCode")) else groups.add(row.s("groupCode"))
        }
        if (type == 0 || type == 1) ready.complete(Unit)
    }

    suspend fun groups(): List<String> {
        groupLock.withLock {
            if (!ready.isCompleted) {
                bridge.invoke("group", "getGroupList", false)
                withTimeout(12_000.milliseconds) { ready.await() }
            }
        }
        return synchronized(this) { groups.toList() }
    }

    suspend fun has(id: String): Boolean {
        if (id in groups()) return true
        val uid =
            runCatching { uid(id) }.onFailure { if (it is CancellationException) throw it }.getOrNull() ?: return false
        val result = bridge.invoke("buddy", "getBuddyListV2", "0", true, 0)
        check(result.path("data").isArray) { "NTQQ 好友列表读取失败" }
        return result.list("data").any { row -> row.list("buddyUids").any { it.asString() == uid } }
    }

    @Synchronized
    fun memberUpdate(group: String, rows: JsonNode) {
        for ((uid, member) in rows.mapEntries()) {
            remember(member.s("uin"), uid)
            val profileKey = "$group/$uid"
            val old = memberProfiles[profileKey]
            val merged = if (old?.isObject == true) old.deepCopy() as ObjectNode else mapper.createObjectNode()
            member.properties().forEach { (name, value) ->
                if (!value.isNull && (!value.isString || value.asString()
                        .isNotBlank() || name == "cardName")
                ) merged.set(name, value)
            }
            memberProfiles.remove(profileKey)
            memberProfiles[profileKey] = merged
            if (memberProfiles.size > 4096) memberProfiles.remove(memberProfiles.keys.first())
            val key = "$group/$uid"
            val role = if (member.path("isDelete").asBoolean(false)) 0 else merged.i("role")
            members.remove(key)
            members[key] = Member(role, System.currentTimeMillis() + 60_000)
            if (members.size > 4096) members.remove(members.keys.first())
            pending[key]?.complete(role)
        }
    }

    suspend fun profile(id: String, group: String? = null, hint: String = ""): JsonNode {
        if (id.isBlank() || id == "0") return j(obj("id" to "0", "nickname" to hint, "role" to 0))
        var name = hint
        var role = 0
        try {
            val uid = uid(id)
            if (group != null) {
                role = role(group, uid)
                val cached = synchronized(this) { memberProfiles["$group/$uid"] }
                name = cached?.s("cardName").orEmpty().ifBlank { cached?.s("nick").orEmpty() }.ifBlank { name }
            }
            if (group == null || name.isBlank()) {
                val profiles = bridge.invoke("profile", "getCoreAndBaseInfo", "nodeStore", listOf(uid))
                val core = profiles.mapEntries().firstOrNull { it.first == uid }?.second?.path("coreInfo")
                name = core?.s("remark").orEmpty().ifBlank { core?.s("nick").orEmpty() }.ifBlank { name }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
        }
        return j(obj("id" to id, "nickname" to name.ifBlank { id }, "role" to role))
    }

    suspend fun role(group: String, uid: String): Int {
        val key = "$group/$uid"
        synchronized(this) { members[key]?.takeIf { it.expires > System.currentTimeMillis() } }?.let { return it.role }
        val completion = CompletableDeferred<Int>()
        val previous = pending.putIfAbsent(key, completion)
        if (previous != null) return withTimeoutOrNull(1000.milliseconds) { previous.await() } ?: 0
        try {
            return withTimeoutOrNull(1000.milliseconds) {
                val reply = bridge.invoke("group", "getMemberInfo", group, listOf(uid), false)
                if (reply.i("result") != 0) 0 else completion.await()
            } ?: 0
        } finally {
            pending.remove(key, completion)
        }
    }
}
