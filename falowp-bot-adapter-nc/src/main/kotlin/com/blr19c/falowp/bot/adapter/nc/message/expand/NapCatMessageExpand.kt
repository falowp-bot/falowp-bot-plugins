package com.blr19c.falowp.bot.adapter.nc.message.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport.tempBot
import com.blr19c.falowp.bot.adapter.nc.expand.getFile
import com.blr19c.falowp.bot.adapter.nc.expand.getMsg
import com.blr19c.falowp.bot.adapter.nc.message.NapCatEmoji
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatFaceEmoji
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageDataType
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.expand.toImageUrl
import com.blr19c.falowp.bot.system.expand.toURI
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString


/**
 * 文本消息
 */
fun NapCatMessage.toBotTextMessage(): String {
    return this.message.filter { it.type == NapCatMessageDataType.TEXT }
        .mapNotNull { it.data.text }
        .joinToString("")
        .trim()
}

/**
 * 语音消息
 */
fun NapCatMessage.toBotVoice(): ReceiveMessage.Voice? {
    return this.message.filter { it.type == NapCatMessageDataType.RECORD }
        .mapNotNull {
            val id = it.data.id ?: return@mapNotNull null
            val url = it.data.url ?: return@mapNotNull null
            ReceiveMessage.Voice(id, url.toURI())
        }
        .singleOrNull()
}

/**
 * AT消息
 */
suspend fun NapCatMessage.toBotAt(): List<ReceiveMessage.User> {
    return this.message.filter { it.type == NapCatMessageDataType.AT }
        .mapNotNull { it.data.qq }
        .mapNotNull { NapCatBotApiSupport.getUserInfo(it, this.groupId) }
        .toList()
}

/**
 * 图片消息
 */
fun NapCatMessage.toBotImage(): List<ImageUrl> {
    return this.message.filter { it.type == NapCatMessageDataType.IMAGE }
        .mapNotNull { it.data.url }
        .map { ImageUrl(it) }
        .toList()
}

/**
 * 表情 face&表情包
 */
fun NapCatMessage.toBotEmoji(): List<ReceiveMessage.Emoji> {
    val faceEmoji = this.message.filter { it.type == NapCatMessageDataType.FACE }
        .mapNotNull { m ->
            val id = m.data.id ?: return@mapNotNull null
            val raw = m.data.data ?: return@mapNotNull null
            val faceType = raw.path("faceType").safeString()
            NapCatFaceEmoji.fromValue(id, faceType).toNapCatEmoji()
        }.toList()
    val emoji = this.message.filter { it.type == NapCatMessageDataType.IMAGE }
        .mapNotNull { m ->
            val emojiId = m.data.emojiId ?: return@mapNotNull null
            val emojiPackageId = m.data.emojiPackageId ?: return@mapNotNull null
            val summary = m.data.summary ?: return@mapNotNull null
            NapCatEmoji(emojiId, emojiPackageId, summary)
        }.toList()
    return faceEmoji + emoji
}

/**
 * 视频消息
 */
fun NapCatMessage.toBotVideo(): ReceiveMessage.Video? {
    return this.message.filter { it.type == NapCatMessageDataType.VIDEO }
        .mapNotNull {
            val id = it.data.id ?: return@mapNotNull null
            val url = it.data.url ?: return@mapNotNull null
            ReceiveMessage.Video(id, ImageUrl.empty(), url.toURI(), it.data.fileSize ?: 0)
        }.singleOrNull()
}

/**
 * 分享消息
 */
@Suppress("SpellCheckingInspection")
fun NapCatMessage.toBotShare(): List<ReceiveMessage.Share> {
    return this.message.filter { it.type == NapCatMessageDataType.JSON }.mapNotNull {
        val node = Json.unwrapJsonNode(it.data.data ?: return@mapNotNull null)
        val app = node.path("app")?.safeString() ?: return@mapNotNull null
        when {
            app.startsWith("com.tencent.miniapp") -> {
                val appInfo = node["meta"].iterator().next()
                ReceiveMessage.Share(
                    appId = appInfo.path("appid").safeString(),
                    appName = appInfo.path("title").safeString(),
                    title = appInfo.path("desc").safeString(),
                    preview = appInfo.path("preview").safeString().toImageUrl(),
                    sourceUrl = appInfo.path("qqdocurl").safeString()
                        .ifBlank { appInfo.path("url").safeString() },
                )
            }

            app.startsWith("com.tencent.structmsg")
                    || app.startsWith("com.tencent.tuwen.lua")
                    || app.startsWith("com.tencent.music.lua") -> {
                val view = node["view"].safeString()
                val meta = node["meta"][view]
                ReceiveMessage.Share(
                    appId = meta.path("appid").safeString(),
                    appName = meta.path("tag").safeString(),
                    title = meta.path("title").safeString(),
                    preview = meta.path("preview").safeString().toImageUrl(),
                    sourceUrl = meta.path("jumpUrl").safeString()
                        .ifBlank { meta.path("url").safeString() },
                )
            }

            app.startsWith("com.tencent.troopsharecard")
                    || app.startsWith("com.tencent.contact.lua") -> {
                val contact = node["meta"]["contact"]
                ReceiveMessage.Share(
                    appId = contact.path("appid").safeString(),
                    appName = contact.path("tag").safeString(),
                    title = contact.path("nickname").safeString(),
                    preview = contact.path("avatar").safeString().toImageUrl(),
                    sourceUrl = contact.path("jumpUrl").safeString()
                        .ifBlank { contact.path("url").safeString() },
                )
            }


            else -> null
        }
    }.toList()
}

/**
 * 文件消息
 */
suspend fun NapCatMessage.toBotFile(): List<ReceiveMessage.File> {
    val normalFileList = this.message.filter { it.type == NapCatMessageDataType.FILE }
        .mapNotNull {
            val fileId = it.data.fileId ?: return@mapNotNull null
            val fileName = it.data.file ?: return@mapNotNull null
            val fileSize = it.data.fileSize ?: return@mapNotNull null
            val url = it.data.url ?: tempBot.getFile(fileId = fileId).url
            ReceiveMessage.File(fileId, "normal", fileName, url.toURI(), fileSize)
        }.toList()
    val onlineFileList = this.message.filter { it.type == NapCatMessageDataType.ONLINE_FILE }
        .mapNotNull {
            val messageId = it.data.id ?: return@mapNotNull null
            val fileId = it.data.fileId ?: return@mapNotNull null
            val fileName = it.data.file ?: return@mapNotNull null
            val fileSize = it.data.fileSize ?: return@mapNotNull null
            val isDir = it.data.isDir ?: return@mapNotNull null
            val subType = if (isDir) "_dir" else "_file"
            val id = "${messageId}_${fileId}"
            ReceiveMessage.File(id, "online$subType", fileName, "".toURI(), fileSize)
        }.toList()
    return normalFileList + onlineFileList
}

/**
 * Bot消息体
 */
suspend fun NapCatMessage.toBotContent(reference: suspend (String) -> ReceiveMessage?): ReceiveMessage.Content {
    return ReceiveMessage.Content(
        this.toBotTextMessage(),
        this.toBotVoice(),
        this.toBotAt(),
        this.toBotImage(),
        this.toBotEmoji(),
        this.toBotVideo(),
        this.toBotShare(),
        this.toBotFile(),
    ) {
        val messageId = this.message.filter { it.type == NapCatMessageDataType.REPLY }
            .mapNotNull { it.data.id }
            .singleOrNull() ?: return@Content null
        return@Content reference(messageId)
    }
}

/**
 * Bot用户
 */
fun NapCatMessage.toBotUser(): ReceiveMessage.User {
    return ReceiveMessage.User(
        this.sender.userId,
        this.sender.nickname,
        ApiAuth.ADMINISTRATOR,
        NapCatBotApiSupport.avatar(this.sender.userId)
    )
}

/**
 * Bot来源
 */
fun NapCatMessage.toBotSource(): ReceiveMessage.Source {
    val sourceType = this.messageType.toSourceType()
    val id = when (sourceType) {
        SourceTypeEnum.GROUP -> this.groupId ?: ""
        SourceTypeEnum.PRIVATE -> this.sender.userId
        else -> ""
    }
    return ReceiveMessage.Source(id, sourceType)
}

/**
 * 转换为系统消息
 */
suspend fun NapCatMessage.toBotMessage(): ReceiveMessage {
    val content = this.toBotContent { referenceMessageId ->
        val message = tempBot.getMsg(referenceMessageId).toBotMessage()
        return@toBotContent message
    }
    val sender = this.toBotUser()
    val source = this.toBotSource()
    val self = NapCatBotApiSupport.self()
    val messageId = this.messageId
    val messageType = content.toMessageType()
    val adapter = ReceiveMessage.Adapter("NapCat", this)
    return ReceiveMessage(messageId, messageType, content, sender, source, self, adapter)
}