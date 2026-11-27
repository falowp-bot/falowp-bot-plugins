package com.blr19c.falowp.bot.adapter.qq.api

import com.blr19c.falowp.bot.adapter.qq.expand.*
import com.blr19c.falowp.bot.adapter.qq.expand.model.QQMediaType
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.safeString
import kotlin.reflect.KClass
import tools.jackson.databind.JsonNode

/**
 * 适配框架的单聊和群聊发送接口
 * @param receiveMessage 当前消息或事件的上下文
 * @param originalClass 发起调用的插件类型
 */
class QQBotApi(receiveMessage: ReceiveMessage, originalClass: KClass<*>) : BotApi(receiveMessage, originalClass) {
    /**
     * 获取当前机器人身份
     * @return 机器人自身信息
     */
    override suspend fun self(): BotSelf = QQBotApiSupport.botSelf

    /**
     * 发送群聊消息
     * @param sendMessageChain 按顺序发送的消息链
     * @param sourceId 目标群 OpenID
     * @param reference 是否引用当前消息
     * @param forward 合并转发标记，QQ 官方接口仅支持 false
     */
    override suspend fun sendGroup(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        send(sendMessageChain, sourceId, SourceTypeEnum.GROUP, reference, forward)
    }

    /**
     * 发送单聊消息
     * @param sendMessageChain 按顺序发送的消息链
     * @param sourceId 目标用户 OpenID
     * @param reference 是否引用当前消息
     * @param forward 合并转发标记，QQ 官方接口仅支持 false
     */
    override suspend fun sendPrivate(
        vararg sendMessageChain: SendMessageChain,
        sourceId: String,
        reference: Boolean,
        forward: Boolean
    ) {
        send(sendMessageChain, sourceId, SourceTypeEnum.PRIVATE, reference, forward)
    }

    /**
     * 向本进程已观察到的所有群发送消息
     * @param sendMessageChain 按顺序发送的消息链
     * @param reference 是否引用当前消息
     * @param forward 合并转发标记，QQ 官方接口仅支持 false
     */
    override suspend fun sendAllGroup(vararg sendMessageChain: SendMessageChain, reference: Boolean, forward: Boolean) {
        QQBotApiSupport.groupIdList.toList()
            .forEach { sendGroup(*sendMessageChain, sourceId = it, reference = reference, forward = forward) }
    }

    /**
     * 仅为原会话或原发送者的单聊回复提供被动回复消息 ID
     */
    internal fun replyId(target: String, type: SourceTypeEnum): String? {
        val sameSource = receiveMessage.source.id == target && receiveMessage.source.type == type
        val privateReplyToSender = type == SourceTypeEnum.PRIVATE && target == receiveMessage.sender.id
        return receiveMessage.id.takeIf { it.isNotBlank() && (sameSource || privateReplyToSender) }
    }

    /**
     * 从消息场景中提取引用展示所需的 msg_idx
     */
    internal fun referenceId(): String? {
        val event = receiveMessage.adapter.data as? JsonNode ?: return null
        var result: String? = null
        event.path("d").path("message_scene").path("ext").forEach {
            val value = it.safeString()
            if (value.startsWith("msg_idx=")) result = value.removePrefix("msg_idx=").takeIf(String::isNotBlank)
        }
        return result
    }

    private suspend fun send(
        chains: Array<out SendMessageChain>,
        target: String,
        type: SourceTypeEnum,
        reference: Boolean,
        forward: Boolean
    ) {
        require(target.isNotBlank()) { "QQ 消息目标不能为空" }
        require(!forward) { "QQ 官方接口不支持合并转发" }
        val parts = chains.flatMap { it.messageList }
        require(parts.all { it is TextSendMessage || it is ImageSendMessage || it is VideoSendMessage || it is VoiceSendMessage }) {
            "QQ 适配器支持文本、图片、视频和语音；不支持的消息元素不会被静默丢弃"
        }
        targetPath(target, type)
        sendParts(parts, target, type, reference)
    }

    /**
     * 合并相邻文本，保留文本和附件的原始顺序
     */
    private suspend fun sendParts(parts: List<SendMessage>, target: String, type: SourceTypeEnum, reference: Boolean) {
        val text = StringBuilder()
        for (part in parts) {
            if (part is TextSendMessage) {
                text.append(part.content)
            } else {
                flushText(text, target, type, reference)
                sendAttachment(part, target, type, reference)
            }
        }
        flushText(text, target, type, reference)
    }

    private suspend fun flushText(text: StringBuilder, target: String, type: SourceTypeEnum, reference: Boolean) {
        if (text.isNotBlank()) {
            sendPayload(mapOf("msg_type" to 0, "content" to text.toString()), target, type, reference)
        }
        text.clear()
    }

    private suspend fun sendAttachment(part: SendMessage, target: String, type: SourceTypeEnum, reference: Boolean) {
        val (fileType, url) = when (part) {
            is ImageSendMessage -> QQMediaType.IMAGE to part.image.toUrl()
            is VideoSendMessage -> QQMediaType.VIDEO to part.video.toString()
            is VoiceSendMessage -> QQMediaType.VOICE to part.voice.toString()
            else -> error("不支持的 QQ 消息")
        }
        sendMedia(url, fileType, target, type, reference = reference)
    }
}
