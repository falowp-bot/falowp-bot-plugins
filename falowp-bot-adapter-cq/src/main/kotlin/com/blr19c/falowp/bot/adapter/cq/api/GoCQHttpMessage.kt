package com.blr19c.falowp.bot.adapter.cq.api

import com.blr19c.falowp.bot.system.api.MessageTypeEnum
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.cache.CacheReference
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.net.URI
import java.util.*
import kotlin.time.Duration.Companion.minutes

/**
 * GoCQHttp消息
 */
data class GoCQHttpMessage(

    /**
     * 消息类型
     */
    @field:JsonProperty("post_type")
    var postType: String? = null,

    /**
     * 通知类型
     */
    @field:JsonProperty("notice_type")
    var noticeType: String? = null,

    /**
     * 请求类型
     */
    @field:JsonProperty("request_type")
    var requestType: String? = null,

    /**
     * 消息来源
     */
    @field:JsonProperty("message_type")
    var messageType: String? = null,

    /**
     * 时间戳
     */
    @field:JsonProperty("time")
    var time: Long? = null,

    /**
     * 当前机器人QQ号
     */
    @field:JsonProperty("self_id")
    var selfId: String? = null,

    /**
     * 信息子类型（群）
     */
    @field:JsonProperty("sub_type")
    var subType: String? = null,

    /**
     * 匿名（群）
     */
    @field:JsonProperty("anonymous")
    var anonymous: String? = null,

    /**
     * 群号（群）
     */
    @field:JsonProperty("group_id")
    var groupId: String? = null,

    /**
     * 消息序号
     */
    @field:JsonProperty("message_seq")
    var messageSeq: Long? = null,

    /**
     * 发送人信息
     */
    @field:JsonProperty("sender")
    var sender: Sender? = null,

    /**
     * 消息ID（群）
     */
    @field:JsonProperty("message_id")
    var messageId: String? = null,

    /**
     * 字体
     */
    @field:JsonProperty("font")
    var font: Int? = null,

    /**
     * 消息
     */
    @field:JsonProperty("message")
    var message: String? = null,

    /**
     * 消息文本
     */
    @field:JsonProperty("raw_message")
    var rawMessage: String? = null,

    /**
     * 发送人QQ号
     */
    @field:JsonProperty("user_id")
    var userId: String? = null,

    /**
     * 操作人（仅在notice情况下存在）
     */
    @field:JsonProperty("operator_id")
    var operatorId: String? = null,

    /**
     * 发送人QQ号（仅在notice情况下存在）
     */
    @field:JsonProperty("sender_id")
    var senderId: String? = null,

    /**
     * 目标人QQ号（仅在notice情况下存在）
     */
    @field:JsonProperty("target_id")
    var targetId: String? = null,

    /**
     * 回执
     */
    @field:JsonProperty("echo")
    var echo: String? = null,

    /**
     * 邀请者id
     */
    @field:JsonProperty("invitor_id")
    var invitorId: String? = null,

    /**
     * 备注(在申请时是附加信息)
     */
    @field:JsonProperty("comment")
    var comment: String? = null,

    /**
     * 特殊内容时的flag(回执)
     */
    @field:JsonProperty("flag")
    var flag: String? = null,
) {

    val content by CacheReference(30.minutes) { content() }

    val atList by CacheReference(30.minutes) { atList() }

    val imageList by CacheReference(30.minutes) { imageList() }

    val voice by CacheReference(30.minutes) { voice() }

    val video by CacheReference(30.minutes) { video() }

    val shareList by CacheReference(30.minutes) { shareList() }


    fun toMessageType(): MessageTypeEnum {
        if (this.subType == "poke") {
            return MessageTypeEnum.POKE
        }
        if (voice.isPresent) {
            return MessageTypeEnum.VOICE
        }
        if (shareList.isNotEmpty()) {
            return MessageTypeEnum.SHARE
        }
        return MessageTypeEnum.MESSAGE
    }

    /**
     * 处理文本
     */
    private fun content(): String {
        return this.message?.replace(Regex("\\[CQ:[^]]*]"), "")?.trim() ?: ""
    }

    /**
     * 处理@列表
     */
    private suspend fun atList(): List<ReceiveMessage.User> {
        val atSet = mutableSetOf<ReceiveMessage.User>()
        this.message?.let { cqMessage ->
            val atRegex = Regex("\\[CQ:at,qq=(\\d+)]")
            val atList = atRegex.findAll(cqMessage).map { it.groupValues[1] }.toList()
            atSet.addAll(atList.mapNotNull { GoCqHttpBotApiSupport.userInfo(it, groupId) }.toMutableList())
        }
        this.targetId?.let { GoCqHttpBotApiSupport.userInfo(it, groupId) }?.let { atSet.add(it) }
        return atSet.toList()
    }

    /**
     * 处理图片
     */
    private fun imageList(): List<ImageUrl> {
        val cqMessage = this.message ?: return emptyList()
        val imageRegex = Regex("\\[CQ:image.+,url=(https?://[^\\s/\$.?#].\\S*)]")
        return imageRegex.findAll(cqMessage).map { it.groupValues[1] }.map { ImageUrl(it) }.toList()
    }

    /**
     * 处理语音
     */
    private fun voice(): Optional<URI> {
        return Optional.empty()
    }

    /**
     * 处理视频
     */
    private fun video(): Optional<ReceiveMessage.Video> {
        return Optional.empty()
    }

    /**
     * 处理分享
     */
    private fun shareList(): List<ReceiveMessage.Share> {
        val cqMessage = this.message ?: return emptyList()
        val shareRegex = Regex("\\[CQ:json,data=([\\s\\S]*)]")
        val shareList = shareRegex.findAll(cqMessage).map { it.groupValues[1] }.toList()
        return shareList
            .map { replaceEscapeCharacter(it) }
            .map { Json.readJsonNode(it) }
            .mapNotNull { shareInfo(it) }
            .toList()
    }

    private fun shareInfo(jsonNode: JsonNode): ReceiveMessage.Share? {
        return if (jsonNode["app"].asText().startsWith("com.tencent.miniapp"))
            shareMiniAppStandard(jsonNode)
        else if (jsonNode["app"].asText().startsWith("com.tencent.structmsg"))
            shareStandard(jsonNode)
        else if (jsonNode["app"].asText().startsWith("com.tencent.troopsharecard"))
            shareCard(jsonNode)
        else null
    }

    private fun shareMiniAppStandard(jsonNode: JsonNode): ReceiveMessage.Share {
        val appInfo = jsonNode["meta"].elements().next()
        return ReceiveMessage.Share(
            appInfo["title"].asText(),
            appInfo["desc"].asText(),
            ImageUrl(appInfo["preview"].asText()),
            appInfo["qqdocurl"].asText(),
        )
    }

    private fun shareStandard(jsonNode: JsonNode): ReceiveMessage.Share {
        val view = jsonNode["view"].asText()
        return ReceiveMessage.Share(
            jsonNode["meta"][view]["tag"].asText(),
            jsonNode["meta"][view]["title"].asText(),
            ImageUrl(jsonNode["meta"][view]["preview"].asText()),
            jsonNode["meta"][view]["jumpUrl"].asText(),
        )
    }

    private fun shareCard(jsonNode: JsonNode): ReceiveMessage.Share {
        return ReceiveMessage.Share(
            jsonNode["meta"]["contact"]["tag"].asText(),
            jsonNode["meta"]["contact"]["nickname"].asText(),
            ImageUrl(jsonNode["meta"]["contact"]["avatar"].asText()),
            jsonNode["meta"]["contact"]["jumpUrl"].asText(),
        )
    }

    private fun replaceEscapeCharacter(cqMessage: String): String {
        return cqMessage.replace("&#44;", ",")
            .replace("&amp;", "&")
            .replace("&#91;", "[")
            .replace("&#93;", "]")
    }

    /**
     * GoCQHttp消息发送人信息
     */
    data class Sender(

        @field:JsonProperty("age")
        var age: Int? = null,

        @field:JsonProperty("area")
        var area: String? = null,

        @field:JsonProperty("card")
        var card: String? = null,

        @field:JsonProperty("level")
        var level: String? = null,

        @field:JsonProperty("nickname")
        var nickname: String? = null,

        @field:JsonProperty("role")
        var role: String? = null,

        @field:JsonProperty("sex")
        var sex: String? = null,

        @field:JsonProperty("title")
        var title: String? = null,

        @field:JsonProperty("user_id")
        var userId: String? = null
    )
}