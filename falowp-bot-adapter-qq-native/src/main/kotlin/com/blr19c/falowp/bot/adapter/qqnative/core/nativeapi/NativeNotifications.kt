package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.s
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import tools.jackson.databind.JsonNode
import java.io.StringReader
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 解析 QQ 专属通知，消息正文与通用通知由 NativeEvents 处理
 */
internal class NativeNotifications(
    private val contacts: NativeContacts,
    private val emit: suspend (String, JsonNode) -> Unit
) {
    private val cards = linkedMapOf<String, String>()
    private val changes = linkedMapOf<String, Pair<Boolean, Long>>()
    private fun group(id: String) = obj("source" to "group", "target" to id)
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).hex()

    private val log = LoggerFactory.getLogger(javaClass)
    private val observed = linkedSetOf<String>()
    private val cardSequences = linkedMapOf<String, Long>()

    private suspend fun change(identity: String, added: Boolean, key: String, data: JsonNode) {
        val now = System.currentTimeMillis()
        changes[identity]?.let { (previous, at) -> if (previous == added && now - at < 10_000) return }
        emit(key, data)
        changes.remove(identity)
        changes[identity] = added to now
        if (changes.size > 8192) changes.remove(changes.keys.first())
    }

    suspend fun input(row: JsonNode, key: String) {
        val user = contacts.uin(row.s("fromUin"))
        if (user.isBlank()) return
        emit(
            key, j(
                obj(
                    "kind" to "inputStatus", "source" to "private", "target" to user,
                    "actor" to user, "status" to row.i("eventType"), "text" to row.s("statusText")
                )
            )
        )
    }

    suspend fun members(group: String, rows: JsonNode) {
        for ((uid, member) in rows.mapEntries()) {
            if (member.path("isDelete").asBoolean(false)) {
                cards.remove("$group/$uid"); continue
            }
            if (member.hasValue("cardName")) card(group, uid, member.s("cardName"), member.s("uin"))
        }
    }

    private suspend fun card(group: String, uid: String, value: String, uin: String) {
        val identity = "$group/$uid"
        val old = cards[identity]
        if (old != null && old != value) {
            val user = uin.takeIf { it.isNotBlank() && it != "0" } ?: contacts.uin(uid)
            emit(
                "card/$identity/${hash((old + "\u0000" + value).toByteArray())}/${System.nanoTime()}",
                j(
                    group(group) + obj(
                        "kind" to "groupCard",
                        "actor" to "0",
                        "member" to user,
                        "oldCard" to old,
                        "newCard" to value
                    )
                )
            )
        }
        cards.remove(identity)
        cards[identity] = value
        if (cards.size > 4096) cards.remove(cards.keys.first())
    }

    suspend fun system(bytes: ByteArray) {
        val head = b(bytes, 2)
        val body = b(b(bytes, 3), 2)
        val key = "notification/${hash(bytes)}"
        when (u(head, 1).toInt()) {
            44 -> {
                val group = u(body, 1).toString()
                val role = b(body, 4)
                val enabled = b(role, 2).isNotEmpty()
                val uid = s(b(role, if (enabled) 2 else 1), 1)
                if (group == "0" || uid.isBlank()) return
                val user = contacts.uin(uid)
                emit(
                    key,
                    j(
                        group(group) + obj(
                            "kind" to "groupAdmin",
                            "actor" to "0",
                            "member" to user,
                            "enabled" to enabled
                        )
                    )
                )
            }

            528 -> if (u(head, 2) == 39uL && u(body, 1) == 0uL && u(body, 2) == 203uL) {
                val like = b(b(body, 203), 14)
                val detail = b(like, 3)
                val user = u(detail, 3).toString()
                if (user == "0") return
                val count = Regex("\\d+").find(s(detail, 1))?.value?.toIntOrNull() ?: u(like, 1).toInt()
                emit(
                    key, j(
                        obj(
                            "kind" to "profileLike",
                            "source" to "private",
                            "target" to user,
                            "actor" to user,
                            "actorName" to s(detail, 5),
                            "count" to count,
                            "time" to u(like, 2).toLong()
                        )
                    )
                )
            }

            732 -> if (u(head, 2) == 16uL && body.size > 7) {
                val notify = body.copyOfRange(7, body.size)
                val essence = b(notify, 33)
                if (essence.isNotEmpty() && u(essence, 4) in listOf(1uL, 2uL)) {
                    val group = u(essence, 1).toString()
                    val sequence = u(essence, 2).toString()
                    val added = u(essence, 4) == 1uL
                    change(
                        "essence/$group/$sequence", added, key, j(
                            group(group) + obj(
                                "kind" to "groupEssence",
                                "actor" to u(essence, 6).toString(), "actorName" to s(essence, 9),
                                "member" to u(essence, 5).toString(), "memberName" to s(essence, 10),
                                "messageId" to "", "sequence" to sequence, "added" to added
                            )
                        )
                    )
                }
                if (u(notify, 13) == 35uL) {
                    val group = u(notify, 4).toString()
                    val reaction = b(b(b(notify, 44), 1), 1)
                    val target = b(reaction, 2)
                    val data = b(reaction, 3)
                    if (u(data, 5) !in listOf(1uL, 2uL)) return
                    reaction(
                        group, contacts.uin(s(data, 4)), u(target, 1).toString(), s(data, 1),
                        u(data, 3).toInt(), u(data, 5) == 1uL, key
                    )
                }
            }
        }
    }

    private suspend fun reaction(
        group: String,
        actor: String,
        sequence: String,
        emoji: String,
        count: Int,
        added: Boolean,
        key: String
    ) {
        if (group == "0" || sequence == "0" || actor.isBlank() || emoji.isBlank()) return
        change(
            "reaction/$group/$sequence/$actor/$emoji", added, key, j(
                group(group) + obj(
                    "kind" to "messageReaction",
                    "actor" to actor, "messageId" to "", "sequence" to sequence,
                    "emojiId" to emoji, "count" to count, "added" to added
                )
            )
        )
    }

    suspend fun message(row: JsonNode, key: String, updatesOnly: Boolean) {
        if (row.i("chatType") != 2) return
        val group = row.s("peerUid")
        val uid = row.s("senderUid")
        if (!updatesOnly && observed.add(key)) {
            if (observed.size > 8192) observed.remove(observed.first())
            val identity = "$group/$uid"
            val sequence = row.s("msgSeq").toLongOrNull()
            val previous = cardSequences[identity]
            if (row.hasValue("sendMemberName") && uid.isNotBlank() &&
                (sequence == null || previous == null || sequence > previous)
            ) {
                card(group, uid, row.s("sendMemberName"), row.s("senderUin"))
                if (sequence != null) {
                    cardSequences.remove(identity)
                    cardSequences[identity] = sequence
                    if (cardSequences.size > 4096) cardSequences.remove(cardSequences.keys.first())
                }
            }
        }
        for ((index, element) in row.list("elements").withIndex()) {
            try {
                messageElement(row, group, uid, element, "$key/$index", updatesOnly)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                log.warn("QQNative 通知元素解析失败，继续处理其余元素：{}/{}", key, index, error)
            }
        }
    }

    private suspend fun messageElement(
        row: JsonNode, group: String, uid: String, element: JsonNode,
        eventKey: String, updatesOnly: Boolean
    ) {
        val file = element.path("fileElement")
        if (file.s("fileUuid").isNotBlank() && (!updatesOnly || row.i("sendStatus") == 2)) {
            emit(
                "file/$eventKey", j(
                    group(group) + obj(
                        "kind" to "groupFileUpload", "actor" to contacts.uin(uid.ifBlank { row.s("senderUin") }),
                        "fileId" to file.s("fileUuid"), "name" to file.s("fileName"), "size" to file.l("fileSize"),
                        "busId" to file.l("fileBizId"), "messageId" to row.s("msgId")
                    )
                )
            )
        }
        val gray = element.path("grayTipElement")
        val mute = gray.path("groupElement").path("shutUp")
        if (mute.isObject) {
            val member = mute.path("member").s("uid")
            val duration = mute.l("duration")
            emit(
                "mute/$eventKey", j(
                    group(group) + obj(
                        "kind" to "groupMute",
                        "actor" to contacts.uin(mute.path("admin").s("uid")),
                        "member" to if (member.isBlank()) "0" else contacts.uin(member),
                        "duration" to if (member.isBlank() && duration > 0) -1 else duration
                    )
                )
            )
        }
        val tip = gray.path("jsonGrayTipElement")
        if (tip.s("jsonStr").isNotBlank()) {
            val items = mapper.readTree(tip.s("jsonStr")).list("items")
            if (tip.s("busiId") == "2401") {
                val link = items.firstOrNull { it.s("jp").contains("msgSeq=") }?.s("jp")
                if (link != null) {
                    val params = URI(link).rawQuery.orEmpty().split('&').associate {
                        val pair = it.split('=', limit = 2)
                        pair[0] to URLDecoder.decode(pair.getOrElse(1) { "" }, Charsets.UTF_8)
                    }
                    val sequence = params["msgSeq"].orEmpty()
                    val text = items.joinToString("") { it.s("txt") }
                    val added = when {
                        listOf("移出", "移除", "取消").any { text.contains(it) } -> false
                        listOf("设为", "设置", "添加").any { text.contains(it) } -> true
                        else -> null
                    }
                    if (sequence.isNotBlank() && added != null && params["groupCode"] == group) {
                        val users = items.filter { it.s("uid").isNotBlank() }
                        val actor = users.firstOrNull()?.s("uid")?.let { contacts.uin(it) } ?: "0"
                        change(
                            "essence/$group/$sequence", added, "essence/$eventKey", j(
                                group(group) + obj(
                                    "kind" to "groupEssence",
                                    "actor" to actor,
                                    "member" to "0",
                                    "sequence" to sequence,
                                    "messageId" to "",
                                    "added" to added
                                )
                            )
                        )
                    }
                }
            }
            if (items.lastOrNull()?.s("txt") == "头衔" && items.size >= 4) {
                val member = items[1].list("param").firstOrNull()?.asString().orEmpty()
                if (member.isNotBlank()) emit(
                    "title/$eventKey", j(
                        group(group) + obj(
                            "kind" to "groupTitle", "actor" to "0",
                            "member" to member, "title" to items[3].s("txt")
                        )
                    )
                )
            }
        }
        val xml = gray.path("xmlElement")
        if (xml.s("templId") == "10382") {
            val factory = DocumentBuilderFactory.newInstance().apply {
                // XML 解析器的特性标识，不会访问此地址
                @Suppress("HttpUrlsUsage")
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
                setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            }
            val doc = withContext(Dispatchers.IO) {
                factory.newDocumentBuilder().parse(InputSource(StringReader(xml.s("content"))))
            }

            fun attribute(tag: String, name: String) =
                doc.getElementsByTagName(tag).item(0)?.attributes?.getNamedItem(name)?.nodeValue.orEmpty()
            reaction(
                group,
                attribute("qq", "jp"),
                attribute("url", "msgseq"),
                attribute("face", "id"),
                1,
                true,
                "reaction/$eventKey"
            )
        }
    }
}
