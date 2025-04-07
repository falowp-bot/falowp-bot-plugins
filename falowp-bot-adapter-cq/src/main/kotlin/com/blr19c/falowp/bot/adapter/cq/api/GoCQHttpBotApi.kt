package com.blr19c.falowp.bot.adapter.cq.api

import com.blr19c.falowp.bot.adapter.cq.api.GoCqHttpBotApiSupport.messageIdToCQMessageIdMap
import com.blr19c.falowp.bot.adapter.cq.expand.*
import com.blr19c.falowp.bot.adapter.cq.message.CqCustomMusicMessage
import com.blr19c.falowp.bot.adapter.cq.message.CqFaceMessage
import com.blr19c.falowp.bot.adapter.cq.message.CqJsonMessage
import com.blr19c.falowp.bot.adapter.cq.message.CqMusicMessage
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.fasterxml.jackson.databind.node.ArrayNode
import java.net.URI
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * GoCQHttpBotApi
 */
class GoCQHttpBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) : BotApi(receiveMessage, originalClass) {

    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        //当forward时reference失效
        if (forward) {
            val message = buildForwardMessage(SourceTypeEnum.GROUP, sourceId, *sendMessageChain)
            return this.cqSendGroupForwardMsg(sourceId, message).saveMessageId(*sendMessageChain)
        }
        sendMessageChain.forEach {
            val message = buildMessage(it, SourceTypeEnum.GROUP, sourceId, reference)
            this.cqSendGroupMsg(sourceId, message).saveMessageId(it)
        }
    }

    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        for (groupId in GoCqHttpBotApiSupport.groupIdList) {
            //当forward时reference失效
            if (forward) {
                val message = buildForwardMessage(SourceTypeEnum.GROUP, groupId, *sendMessageChain)
                this.cqSendGroupForwardMsg(groupId, message).saveMessageId(*sendMessageChain)
                continue
            }
            sendMessageChain.forEach {
                val message = buildMessage(it, SourceTypeEnum.GROUP, groupId, reference)
                this.cqSendGroupMsg(groupId, message).saveMessageId(it)
            }
        }
    }

    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        //当forward时reference失效
        if (forward) {
            val message = buildForwardMessage(SourceTypeEnum.PRIVATE, sourceId, *sendMessageChain)
            return this.cqSendPrivateForwardMsg(sourceId, message).saveMessageId(*sendMessageChain)
        }
        sendMessageChain.forEach {
            val message = buildMessage(it, SourceTypeEnum.PRIVATE, sourceId, reference)
            this.cqSendPrivateMsg(userId = sourceId, message = message).saveMessageId(it)
        }
    }

    /**
     * 转发消息
     */
    private suspend fun buildForwardMessage(
        sourceTypeEnum: SourceTypeEnum,
        sourceId: String,
        vararg sendMessageChains: SendMessageChain
    ): ArrayNode {
        val nickname = systemConfigProperty("nickname")
        val selfId = this.receiveMessage.self.id
        val messageNode = Json.objectMapper().createArrayNode()
        for (sendMessageChain in sendMessageChains) {
            val message = buildMessage(sendMessageChain, sourceTypeEnum, sourceId, false)
            val forwardData = """{"type":"node","data":{"name":"$nickname","uin":"$selfId","content":"$message"}}"""
            messageNode.add(Json.readJsonNode(forwardData))
        }
        return messageNode
    }

    /**
     * 单个消息
     */
    private suspend fun buildMessage(
        sendMessageChain: SendMessageChain,
        sourceTypeEnum: SourceTypeEnum,
        sourceId: String,
        reference: Boolean,
    ): String {
        val builder = StringBuilder()
        for (sendMessage in sendMessageChain.messageList) {
            val message = when (sendMessage) {
                is AtSendMessage -> atCQ(sendMessage.at, sourceId, sourceTypeEnum)
                is TextSendMessage -> sendMessage.content
                is VoiceSendMessage -> voiceCQ(sendMessage.voice)
                is ImageSendMessage -> imageCQ(sendMessage.image)
                is VideoSendMessage -> videoCQ(sendMessage.video)
                is PokeSendMessage -> pokeCQ(receiveMessage.sender.id)
                is CqFaceMessage -> faceCQ(sendMessage.faceId)
                is CqMusicMessage -> musicCQ(sendMessage)
                is CqCustomMusicMessage -> customMusicCQ(sendMessage)
                is CqJsonMessage -> jsonCQ(sendMessage.message, sendMessage.escape)
                else -> ""
            }
            builder.append(message)
        }
        if (reference) {
            builder.append(replyCQ(receiveMessage.id))
        }
        return builder.toString()
    }

    private fun String.saveMessageId(vararg sendMessageChains: SendMessageChain) {
        val messageIdList = messageIdToCQMessageIdMap.get(this) + sendMessageChains.map { it.id }
        messageIdToCQMessageIdMap.put(this, messageIdList)
    }

    private suspend fun atCQ(at: String, sourceId: String, sourceTypeEnum: SourceTypeEnum): String {
        if (at == "all" && sourceTypeEnum == SourceTypeEnum.GROUP) {
            val remain = this.cqGetGroupAtAllRemain(sourceId)
            if (!remain.canAtAll || min(remain.remainAtAllCountForGroup, remain.remainAtAllCountForUin) <= 0) {
                log().info("@全体次数已用尽")
                return ""
            }
        }
        return "[CQ:at,qq=${at}]"
    }

    private fun replyCQ(messageId: String): String {
        return "[CQ:reply,id=$messageId]"
    }

    private fun voiceCQ(voice: URI): String {
        return "[CQ:record,file=$voice]"
    }

    private suspend fun imageCQ(image: ImageUrl): String {
        return if (image.isUrl()) "[CQ:image,file=${image.toUrl()}]"
        else "[CQ:image,file=base64://${image.toBase64()}]"
    }

    private fun videoCQ(video: URI): String {
        return "[CQ:video,file=$video]"
    }

    private fun pokeCQ(sendId: String): String {
        return "[CQ:poke,qq=$sendId]"
    }

    private fun faceCQ(faceId: Int): String {
        return "[CQ:face,id=${faceId}]"
    }

    private fun customMusicCQ(cqCustomMusicMessage: CqCustomMusicMessage): String {
        return "[CQ:music,type=custom,subtype=${cqCustomMusicMessage.subtype},url=${cqCustomMusicMessage.url},audio=$${cqCustomMusicMessage.audio},title=${cqCustomMusicMessage.title},image=${cqCustomMusicMessage.image},content=${cqCustomMusicMessage.content}]"
    }

    private fun musicCQ(cqMusicMessage: CqMusicMessage): String {
        return "[CQ:music,type=${cqMusicMessage.type},id=${cqMusicMessage.id}]"
    }

    private fun jsonCQ(message: String, escape: Boolean): String {
        return "[CQ:json,data=${replaceEscapeCharacter(message, escape)}]"
    }

    private fun replaceEscapeCharacter(cqMessage: String, escape: Boolean): String {
        if (!escape) return cqMessage
        return cqMessage.replace(",", "&#44;")
            .replace("[", "&#91;")
            .replace("]", "&#93;")
    }
}