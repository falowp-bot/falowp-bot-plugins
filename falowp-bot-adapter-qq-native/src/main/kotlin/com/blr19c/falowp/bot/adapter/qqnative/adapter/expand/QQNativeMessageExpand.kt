// 公共扩展由业务插件调用
@file:Suppress("unused")

package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand

import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.*
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.SendMessage
import java.net.URI
import java.nio.file.Path

/**
 * 追加本地离线文件
 */
fun SendMessage.Builder.fileMessage(file: Path, name: String = file.fileName.toString()) =
    fileMessage(file.toUri(), name)

/**
 * 追加远程离线文件，发送前下载到临时目录
 */
fun SendMessage.Builder.fileMessage(file: URI, name: String) =
    apply { messageList.addLast(QQNativeFileMessage(file, name)) }

/**
 * 追加商城表情
 */
fun SendMessage.Builder.marketFaceMessage(id: String, packageId: Long, key: String, name: String = "[商城表情]") =
    apply { messageList.addLast(QQNativeMarketFaceMessage(id, packageId, key, name)) }

/**
 * 追加骰子，result 可指定 1 到 6，省略时随机
 */
fun SendMessage.Builder.diceMessage(result: Int? = null) =
    apply { messageList.addLast(QQNativeGameMessage(true, result)) }

/**
 * 追加猜拳，1 为石头、2 为剪刀、3 为布，省略时随机
 */
fun SendMessage.Builder.rpsMessage(result: Int? = null) =
    apply { messageList.addLast(QQNativeGameMessage(false, result)) }

/**
 * 按消息 ID 追加当前账号已保存的原文，同名 ID 可用群号或 QQ 号指定会话
 */
suspend fun QQNativeSendMessageScope.appendMessage(messageId: String, groupId: String? = null, userId: String? = null) {
    require(groupId == null || userId == null) { "群号和 QQ 号不能同时指定" }
    val args = mutableMapOf<String, Any>("message_id" to messageId)
    (groupId ?: userId)?.let {
        require(it.isNotBlank()) { "会话 ID 不能为空" }
        args["target"] = it
        args["group"] = groupId != null
    }
    val message = api.expand("original_message", args)
    builder.messageList.addLast(QQNativeOriginalMessage(api.nativeAccount().id, message.deepCopy()))
}

/**
 * 追加可播放的音乐卡片，支持 qq、163、kugou、kuwo、migu
 *
 * @param signUrl 音乐签名服务地址，默认读取 qqNative.musicSignUrl
 */
suspend fun QQNativeSendMessageScope.musicMessage(
    type: String, id: String,
    signUrl: String = adapterConfigProperty("qqNative.musicSignUrl") { "" }
) {
    customCardMessage(api.expand("music_card", mapOf("type" to type, "id" to id, "sign_url" to signUrl)))
}
