@file:Suppress("unused")

package com.blr19c.falowp.bot.adapter.nc.message.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.expand.*
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage.MessageData
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatFaceEmoji
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageDataType.*
import com.blr19c.falowp.bot.system.api.SendMessage
import tools.jackson.databind.JsonNode

/**
 * NapCat 自定义消息
 */
data class NapCatSendMessage(val message: NapCatMessage.Message) : SendMessage

/**
 * NapCat 自定义消息作用域
 */
class NapCatSendMessageScope(val builder: SendMessage.Builder)

/**
 * NapCat 消息作用域
 */
suspend fun SendMessage.Builder.nc(block: suspend NapCatSendMessageScope.() -> Unit) = apply {
    NapCatSendMessageScope(this).block()
}

/**
 * 表情消息
 *
 * @param face 表情
 */
fun NapCatSendMessageScope.faceMessage(face: NapCatFaceEmoji) {
    this.builder.messageList.addLast(
        NapCatSendMessage(NapCatMessage.Message(FACE, MessageData(id = face.id)))
    )
}

/**
 * 商城表情消息
 *
 * @param key 表情KEY
 * @param id 表情ID
 * @param packageId 所属表情包ID
 * @param summary 摘要
 */
fun NapCatSendMessageScope.mFaceMessage(key: String, id: String, packageId: String, summary: String) {
    val messageData = MessageData(
        key = key,
        emojiId = id,
        emojiPackageId = packageId,
        summary = summary
    )
    this.builder.messageList.addLast(NapCatSendMessage(NapCatMessage.Message(M_FACE, messageData)))
}

/**
 * 将其他消息拼接到此消息中
 */
suspend fun NapCatSendMessageScope.appendMessage(messageId: String) {
    val messageId = NapCatBotApiSupport.convertMessageId(messageId)
    val napCatMessage = NapCatBotApiSupport.tempBot.getMsg(messageId)
    napCatMessage.message.forEach {
        this.builder.messageList.addLast(NapCatSendMessage(it))
    }
}

/**
 * 骰子消息
 *
 * @param result 点数结果 1-6
 */
fun NapCatSendMessageScope.diceMessage(result: Int = 1) {
    this.builder.messageList.addLast(
        NapCatSendMessage(NapCatMessage.Message(DICE, MessageData(result = result.toString())))
    )
}

/**
 * 猜拳消息
 *
 * @param result 1-石头 2-剪刀 3-布
 */
fun NapCatSendMessageScope.rpsMessage(result: Int = 1) {
    this.builder.messageList.addLast(
        NapCatSendMessage(NapCatMessage.Message(RPS, MessageData(result = result.toString())))
    )
}

/**
 * 分享用户
 */
suspend fun NapCatSendMessageScope.shareUserMessage(userId: String) {
    customCardMessage(NapCatBotApiSupport.tempBot.arkSharePeer(userId))
}

/**
 * 分享群组
 */
suspend fun NapCatSendMessageScope.shareGroupMessage(groupId: String) {
    customCardMessage(NapCatBotApiSupport.tempBot.arkShareGroup(groupId))
}

/**
 * 音乐消息(如果没有配置音乐签名地址此方法不可用)
 *
 * @param type qq 163 kugou migu kuwo
 * @param id 平台音乐ID
 */
fun NapCatSendMessageScope.musicMessage(type: String, id: String) {
    this.builder.messageList.addLast(
        NapCatSendMessage(NapCatMessage.Message(MUSIC, MessageData(type = type, id = id)))
    )
}


/**
 * 分享卡片(NapCat原生Type)
 *
 * @param type 类型 bili/weibo
 * @param title 标题
 * @param desc 描述
 * @param picUrl 图片URL
 * @param jumpUrl 跳转URL
 */
suspend fun NapCatSendMessageScope.cardMessage(
    type: String,
    title: String,
    desc: String,
    picUrl: String,
    jumpUrl: String
) {
    customCardMessage(NapCatBotApiSupport.tempBot.getMiniAppArk(type, title, desc, picUrl, jumpUrl))
}

/**
 * QQ音乐分享卡片
 *
 * @param id 音乐ID
 * @param title 标题
 * @param desc 描述
 * @param picUrl 图片URL
 */
suspend fun NapCatSendMessageScope.qqMusicCardMessage(
    id: String,
    title: String,
    desc: String,
    picUrl: String,
) {
    val customMiniAppArk = NapCatBotApiSupport.tempBot.getCustomMiniAppArk(
        "1109523715",
        title,
        desc,
        picUrl,
        "pages/index/index?songid=$id",
        "https://y.qq.com/n/ryqq_v2/songDetail/$id",
        "https://miniapp.gtimg.cn/public/appicon/bf7a9c987edbd7f70b6540a8dd6c4a26_200.jpg"
    )
    customCardMessage(customMiniAppArk)
}

/**
 * B站视频分享卡片
 *
 * @param id 视频BV
 * @param title 标题
 * @param desc 描述
 * @param picUrl 图片URL
 */
suspend fun NapCatSendMessageScope.biliVideoCardMessage(
    id: String,
    title: String,
    desc: String,
    picUrl: String,
) {
    val customMiniAppArk = NapCatBotApiSupport.tempBot.getCustomMiniAppArk(
        "1109937557",
        title,
        desc,
        picUrl,
        "pages/video/video?bvid=$id",
        "https://www.bilibili.com/video/$id",
        "https://miniapp.gtimg.cn/public/appicon/51f90239b78a2e4994c11215f4c4ba15_200.jpg"
    )
    customCardMessage(customMiniAppArk)
}

/**
 * 自定义分享卡片
 *
 * @param jsonCard json数据
 */
fun NapCatSendMessageScope.customCardMessage(jsonCard: JsonNode) {
    this.builder.messageList.addLast(
        NapCatSendMessage(NapCatMessage.Message(JSON, MessageData(data = jsonCard)))
    )
}