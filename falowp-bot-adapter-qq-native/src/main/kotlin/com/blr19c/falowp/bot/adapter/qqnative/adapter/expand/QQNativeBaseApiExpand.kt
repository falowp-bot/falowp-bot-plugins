// 公共扩展由业务插件调用
@file:Suppress("unused")

package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand

import com.blr19c.falowp.bot.adapter.qqnative.adapter.QQNativeBotApi
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativeCardMessage
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativeEmojiLikes
import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessage
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessageReceipt
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeReceivedPart
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.api.SendMessageChain
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.plugin.message.MessageMatch
import tools.jackson.databind.JsonNode

/**
 * 追加自定义 Ark 卡片；构建器会复制 JSON，避免后续修改影响消息
 *
 * @param json 完整的原生 Ark 卡片内容
 */
fun SendMessage.Builder.customCardMessage(json: JsonNode) =
    apply { messageList.addLast(QQNativeCardMessage(json.deepCopy())) }

/**
 * 获取群分享卡片
 *
 * @param groupId 群号，默认使用当前消息来源
 */
suspend fun QQNativeBotApi.arkShareGroup(groupId: String = receiveMessage.source.id): JsonNode =
    expandRequest("ark_share_group", mapOf("group_id" to groupId))

/**
 * 获取用户分享卡片
 *
 * @param userId QQ 号，默认使用当前消息发送者
 */
suspend fun QQNativeBotApi.arkSharePeer(userId: String = receiveMessage.sender.id): JsonNode =
    expandRequest("ark_share_peer", mapOf("user_id" to userId))

/**
 * 将当前账号的全部会话标记为已读
 */
suspend fun QQNativeBotApi.markAllAsRead() = expandUnit("mark_all_as_read")

/**
 * 按预设类型生成小程序分享卡片
 *
 * @param type 小程序类型
 * @param title 卡片标题
 * @param desc 卡片摘要
 * @param picUrl 封面地址
 * @param jumpUrl 跳转地址
 */
suspend fun QQNativeBotApi.getMiniAppArk(
    type: String,
    title: String,
    desc: String,
    picUrl: String,
    jumpUrl: String
): JsonNode =
    expandRequest(
        "get_mini_app_ark",
        mapOf("type" to type, "title" to title, "desc" to desc, "picUrl" to picUrl, "jumpUrl" to jumpUrl)
    )

/**
 * 生成指定小程序的分享卡片
 *
 * @param appId 小程序应用 ID
 * @param title 卡片标题
 * @param desc 卡片摘要
 * @param picUrl 封面地址
 * @param pathUrl 小程序内部页面路径
 * @param docUrl 网页跳转地址
 * @param iconUrl 小程序图标地址
 */
suspend fun QQNativeBotApi.getCustomMiniAppArk(
    appId: String,
    title: String,
    desc: String,
    picUrl: String,
    pathUrl: String,
    docUrl: String,
    iconUrl: String
): JsonNode =
    expandRequest(
        "get_mini_app_ark",
        mapOf(
            "appId" to appId,
            "title" to title,
            "desc" to desc,
            "picUrl" to picUrl,
            "jumpUrl" to pathUrl,
            "webUrl" to docUrl,
            "iconUrl" to iconUrl
        )
    )

/**
 * 在当前机器人账号上下文中构建分享消息
 */
class QQNativeSendMessageScope(val builder: SendMessage.Builder, val api: QQNativeBotApi) {
    /**
     * 追加自定义 Ark 卡片；构建器会复制 JSON，避免后续修改影响消息
     *
     * @param json 完整的原生 Ark 卡片内容
     */
    fun customCardMessage(json: JsonNode) {
        builder.customCardMessage(json)
    }

    /**
     * 追加指定用户的分享卡片
     */
    suspend fun shareUserMessage(userId: String) {
        customCardMessage(api.arkSharePeer(userId))
    }

    /**
     * 追加指定群的分享卡片
     */
    suspend fun shareGroupMessage(groupId: String) {
        customCardMessage(api.arkShareGroup(groupId))
    }

    /**
     * 追加预设类型的小程序分享卡片
     */
    suspend fun cardMessage(type: String, title: String, desc: String, picUrl: String, jumpUrl: String) {
        customCardMessage(api.getMiniAppArk(type, title, desc, picUrl, jumpUrl))
    }

    /**
     * 追加 QQ 音乐歌曲卡片，id 为歌曲标识
     */
    suspend fun qqMusicCardMessage(id: String, title: String, desc: String, picUrl: String) {
        customCardMessage(
            api.getCustomMiniAppArk(
                "1109523715",
                title,
                desc,
                picUrl,
                "pages/index/index?songid=$id",
                "https://y.qq.com/n/ryqq_v2/songDetail/$id",
                "https://miniapp.gtimg.cn/public/appicon/bf7a9c987edbd7f70b6540a8dd6c4a26_200.jpg"
            )
        )
    }

    /**
     * 追加哔哩哔哩视频卡片，id 为视频 BV 号
     */
    suspend fun biliVideoCardMessage(id: String, title: String, desc: String, picUrl: String) {
        customCardMessage(
            api.getCustomMiniAppArk(
                "1109937557",
                title,
                desc,
                picUrl,
                "pages/video/video?bvid=$id",
                "https://www.bilibili.com/video/$id",
                "https://miniapp.gtimg.cn/public/appicon/51f90239b78a2e4994c11215f4c4ba15_200.jpg"
            )
        )
    }
}

/**
 * 使用指定账号构建 QQ Native 扩展消息
 */
suspend fun SendMessage.Builder.qqNative(api: QQNativeBotApi, block: suspend QQNativeSendMessageScope.() -> Unit) =
    apply { block(QQNativeSendMessageScope(this, api)) }

private fun QQNativeBotApi.context() = mapOf<String, Any?>(
    "group" to (receiveMessage.source.type == SourceTypeEnum.GROUP),
    "target" to receiveMessage.source.id
)

/**
 * 从当前会话解析消息原文
 *
 * @param messageId 原生消息 ID
 * @throws IllegalStateException 无法找到消息原文时抛出
 */
suspend fun QQNativeBotApi.getMsg(messageId: String): QQNativeMessage {
    val group = receiveMessage.source.type == SourceTypeEnum.GROUP
    val peer =
        if (group) QQNativeConversation.Group(receiveMessage.source.id) else QQNativeConversation.Private(receiveMessage.source.id)
    val original = receiveMessage.adapter.data as? QQNativeMessage
    val context = original ?: QQNativeMessage(peer, peer.id, QQNativeMessageReceipt("", "", "", ""), "", 0, emptyList())
    return checkNotNull(
        nativeAccount().messages.resolve(
            context,
            QQNativeReceivedPart.Reply(messageId, "", "", "")
        )
    ) { "消息不存在" }
}

/**
 * 撤回当前会话中的指定消息
 */
suspend fun QQNativeBotApi.deleteMsg(messageId: String) =
    expandUnit("delete_msg", context() + mapOf("message_id" to messageId))

/**
 * 标记会话已读；消息 ID 用于定位会话，不表示只读到该条消息
 */
suspend fun QQNativeBotApi.markMsgAsRead(
    messageId: String? = null, userId: String? = null, groupId: String? = null
) {
    require(userId == null || groupId == null) { "群号和 QQ 号不能同时指定" }
    val parameters = when {
        groupId != null -> mapOf("group" to true, "target" to groupId)
        userId != null -> mapOf("group" to false, "target" to userId)
        messageId != null -> emptyMap()
        else -> context()
    }
    expandUnit("mark_msg_as_read", parameters + mapOf("message_id" to messageId))
}

/**
 * 标记指定群聊已读，也可通过已记录的消息 ID 定位群聊
 */
suspend fun QQNativeBotApi.markGroupMsgAsRead(groupId: String? = null, messageId: String? = null) =
    expandUnit(
        "mark_msg_as_read", mapOf(
            "group" to true, "message_id" to messageId,
            "target" to (groupId ?: receiveMessage.source.id.takeIf { messageId == null })
        )
    )

/**
 * 标记指定私聊已读，也可通过已记录的消息 ID 定位私聊
 */
suspend fun QQNativeBotApi.markPrivateMsgAsRead(userId: String? = null, messageId: String? = null) =
    expandUnit(
        "mark_msg_as_read", mapOf(
            "group" to false, "message_id" to messageId,
            "target" to (userId ?: receiveMessage.sender.id.takeIf { messageId == null })
        )
    )

/**
 * 将当前会话中的单条消息转发给指定用户
 */
suspend fun QQNativeBotApi.forwardFriendSingleMsg(messageId: String, userId: String = receiveMessage.sender.id) =
    expandUnit("forward_single_msg", context() + mapOf("message_id" to messageId, "to_group" to false, "to" to userId))

/**
 * 将当前会话中的单条消息转发到指定群
 */
suspend fun QQNativeBotApi.forwardGroupSingleMsg(messageId: String, groupId: String = receiveMessage.source.id) =
    expandUnit("forward_single_msg", context() + mapOf("message_id" to messageId, "to_group" to true, "to" to groupId))

/**
 * 设置或取消消息表情回应，set 为 true 时添加回应
 */
suspend fun QQNativeBotApi.setMsgEmojiLike(messageId: String, emojiId: String, set: Boolean = true) =
    expandUnit("set_msg_emoji_like", context() + mapOf("message_id" to messageId, "emoji_id" to emojiId, "set" to set))

/**
 * 分页查询消息表情回应
 *
 * @param messageId 原生消息 ID
 * @param emojiId 表情标识
 * @param emojiType 原生表情类型
 * @param count 本页最大数量
 * @param cookie 上一页返回的游标，首页传空字符串
 */
suspend fun QQNativeBotApi.fetchEmojiLike(
    messageId: String,
    emojiId: String,
    emojiType: String = "1",
    count: Int = 20,
    cookie: String = ""
): QQNativeEmojiLikes =
    expandRequest(
        "fetch_emoji_like",
        context() + mapOf(
            "message_id" to messageId,
            "emoji_id" to emojiId,
            "emoji_type" to emojiType,
            "count" to count,
            "cookie" to cookie
        )
    )

/**
 * 根据表情标识推断类型，并分页查询消息表情回应
 */
suspend fun QQNativeBotApi.getEmojiLikes(messageId: String, emojiId: String, count: Int = 20, cookie: String = "") =
    fetchEmojiLike(messageId, emojiId, if (emojiId.length > 3) "2" else "1", count, cookie)

/**
 * 触发群消息中的内联键盘按钮回调
 *
 * @param groupId 群号
 * @param botAppid 按钮所属机器人应用 ID
 * @param buttonId 按钮标识
 * @param msgSeq 消息序列号
 * @param callbackData 按钮回调数据
 */
suspend fun QQNativeBotApi.clickInlineKeyboardButton(
    groupId: String = receiveMessage.source.id,
    botAppid: String,
    buttonId: String,
    msgSeq: String,
    callbackData: String
) =
    expandUnit(
        "click_inline_keyboard_button",
        mapOf(
            "group_id" to groupId,
            "bot_appid" to botAppid,
            "button_id" to buttonId,
            "msg_seq" to msgSeq,
            "callback_data" to callbackData
        )
    )

/**
 * 向指定用户发送原生输入状态事件，eventType 为原生事件类型
 */
suspend fun QQNativeBotApi.setInputStatus(userId: String = receiveMessage.sender.id, eventType: Int) =
    expandUnit("set_input_status", mapOf("group" to false, "target" to userId, "event_type" to eventType))

/**
 * 向指定群发送消息链，默认发送到当前消息来源
 */
suspend fun QQNativeBotApi.sendGroupMsg(vararg chains: SendMessageChain, groupId: String = receiveMessage.source.id) =
    sendGroup(*chains, sourceId = groupId)

/**
 * 向指定用户发送消息链，默认发送给当前消息发送者
 */
suspend fun QQNativeBotApi.sendPrivateMsg(vararg chains: SendMessageChain, userId: String = receiveMessage.sender.id) =
    sendPrivate(*chains, sourceId = userId)

/**
 * 发送戳一戳；提供 groupId 时使用群聊，否则使用私聊
 */
suspend fun QQNativeBotApi.sendPoke(groupId: String? = null, userId: String = receiveMessage.sender.id) {
    val message = SendMessage.builder().nudge(userId).build()
    if (groupId == null) sendPrivate(message, sourceId = userId) else sendGroup(message, sourceId = groupId)
}

/**
 * 限定消息匹配器仅匹配 QQ Native 适配器
 */
fun MessageMatch.Build.qqNative() = apply {
    appendCustom { it.adapter.id == "QQNative" }
}

/**
 * 限定消息匹配器仅匹配 QQ Native 适配器
 */
fun MessageMatch.qqNative() = copy(adapterId = "QQNative")
