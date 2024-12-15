package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import com.fasterxml.jackson.core.type.TypeReference
import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * 群打卡
 *
 * @param groupId 群组id
 */
suspend fun BotApi.setGroupSign(groupId: String = this.receiveMessage.source.id) {
    apiRequest<Unit>("set_group_sign", mapOf("group_id" to groupId))
}

/**
 * 获取推荐好友/群聊卡片
 */
suspend fun BotApi.arkSharePeer() {

}

/**
 * 推荐群聊
 */
suspend fun BotApi.arkShareGroup() {

}

/**
 * 获取机器人QQ号区间
 */
suspend fun BotApi.getRobotUinRange() {

}

/**
 * 设置在线状态
 */
suspend fun BotApi.setOnlineStatus() {

}

/**
 * 获取好友分类列表
 */
suspend fun BotApi.getFriendsWithCategory() {

}

/**
 * 设置QQ头像
 *
 * @param file 文件(支持文件路径/网络url)
 */
suspend fun BotApi.setQQAvatar(file: String) {
    apiRequest<Unit>("set_qq_avatar", mapOf("file" to file))
}

/**
 * 获取文件信息
 */
suspend fun BotApi.getFile() {

}

/**
 * 转发单条信息到私聊
 */
suspend fun BotApi.forwardFriendSingleMsg() {

}

/**
 * 转发单条信息到群聊
 */
suspend fun BotApi.forwardGroupSingleMsg() {

}

/**
 * 英译中翻译
 */
suspend fun BotApi.translateEn2zh() {

}

/**
 * 设置消息的表情回复
 */
suspend fun BotApi.setMsgEmojiLike() {

}

/**
 * 发送合并转发
 */
suspend fun BotApi.sendForwardMsg() {

}

/**
 * 标记私聊信息已读
 */
suspend fun BotApi.markPrivateMsgAsRead() {

}

/**
 * 标记群聊信息已读
 */
suspend fun BotApi.markGroupMsgAsRead() {

}

/**
 * 获取私聊记录
 */
suspend fun BotApi.getFriendMsgHistory() {

}

/**
 * 创建文本收藏
 */
suspend fun BotApi.createCollection() {

}

/**
 * 获取收藏列表
 */
suspend fun BotApi.getCollectionList() {

}

/**
 * 设置个人签名
 */
suspend fun BotApi.setSelfSignature() {

}

/**
 * 获取最近的聊天记录
 */
suspend fun BotApi.getRecentContact() {}

/**
 * 标记所有为已读
 */
suspend fun BotApi.markAllAsRead() {}

/**
 * 获取自身点赞列表
 */
suspend fun BotApi.getProfileLike() {}

/**
 * 获取收藏表情
 */
suspend fun BotApi.fetchCustomFace() {

}

/**
 * 拉取表情回应列表
 */
suspend fun BotApi.fetchEmojiLike() {

}

/**
 * 设置输入状态
 */
suspend fun BotApi.setInputStatus() {}

/**
 * 获取群组额外信息
 */
suspend fun BotApi.getGroupInfoEx() {}

/**
 * 获取群组忽略的通知
 */
suspend fun BotApi.getGroupIgnoreAddRequest() {}

/**
 * 删除群聊公告
 */
suspend fun BotApi.delGroupNotice() {}

/**
 * 获取用户个人资料页
 */
suspend fun BotApi.fetchUserProfileLike() {}

/**
 * 私聊戳一戳
 */
suspend fun BotApi.friendPoke() {}

/**
 * 群聊戳一戳
 */
suspend fun BotApi.groupPoke() {}

/**
 * 获取PacketServer状态
 */
suspend fun BotApi.ncGetPacketStatus() {}

/**
 * 获取陌生人在线状态
 */
suspend fun BotApi.ncGetUserStatus() {}

/**
 * 获取RKey
 */
suspend fun BotApi.ncGetRKey() {}

/**
 * 获取群聊被禁言用户
 */
suspend fun BotApi.getGroupShutList() {

}

/**
 * 签名小程序卡片
 */
suspend fun BotApi.getMiniAppArk() {

}

/**
 * AI文字转语音
 */
suspend fun BotApi.getAiRecord() {}

/**
 * 获取AI语音角色列表
 */
suspend fun BotApi.getAiCharacters() {}

/**
 * 群聊发送AI语音
 */
suspend fun BotApi.sendGroupAiRecord() {

}

private suspend inline fun <reified T : Any> apiRequest(url: String, body: Any = emptyMap<String, String>()): T? {
    val baseUrl = adapterConfigProperty("cq.apiUrl")
    val resBytes = longTimeoutWebclient().post("$baseUrl/$url") {
        setBody(body)
    }.bodyAsBytes()
    val typeReference = object : TypeReference<NapCatQQApiResult<T>>() {}
    val result = Json.readObj(resBytes, typeReference)
    if (result.success()) {
        return result.data
    }
    throw IllegalStateException("NapCatQQ适配器请求API失败:${result.msg},${result.wording}")
}