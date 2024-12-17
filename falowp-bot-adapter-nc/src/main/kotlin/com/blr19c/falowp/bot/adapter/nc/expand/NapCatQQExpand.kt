package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.cq.api.GoCQHttpMessage
import com.blr19c.falowp.bot.adapter.cq.api.GoCqHttpBotApiSupport.messageIdToCQMessageIdMap
import com.blr19c.falowp.bot.adapter.nc.expand.enums.BotOnlineStatusEnum
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
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
 * 获取推荐好友
 *
 * @param userId 用户id
 * @param phoneNumber 手机号
 */
suspend fun BotApi.arkSharePeerUser(
    userId: String = this.receiveMessage.sender.id,
    phoneNumber: String? = null
): ArkSharePeer {
    val request = mutableMapOf("user_id" to userId)
    phoneNumber?.let { request["phone_number"] = it }
    return apiRequest<ArkSharePeer>("ArkSharePeer", request)!!
}

/**
 * 推荐群聊
 *
 * @param groupId 群组id
 * @return json卡片
 */
suspend fun BotApi.arkShareGroup(groupId: String = this.receiveMessage.source.id): String {
    return apiRequest<String>("ArkShareGroup", mapOf("group_id" to groupId))!!
}

/**
 * 获取机器人QQ号区间
 */
suspend fun BotApi.getRobotUinRange(): List<RobotUinRange> {
    return apiRequest<List<RobotUinRange>>("get_robot_uin_range")!!
}

/**
 * 设置在线状态
 *
 * @param onlineStatus 在线状态
 * @param battery 电池电量
 */
suspend fun BotApi.setOnlineStatus(onlineStatus: BotOnlineStatusEnum, battery: Int = 0) {
    apiRequest<Unit>(
        "set_online_status",
        mapOf(
            "status" to onlineStatus.status,
            "ext_status" to onlineStatus.extStatus,
            "battery_status" to battery
        )
    )
}

/**
 * 获取好友分组列表
 */
suspend fun BotApi.getFriendsWithCategory(): List<FriendsWithCategory> {
    return apiRequest<List<FriendsWithCategory>>("get_friends_with_category")!!
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
 *
 * WARNING: 未经过测试
 *
 * @param fileId 文件id
 */
suspend fun BotApi.getFileInfo(fileId: String): FileInfo {
    return apiRequest<FileInfo>("get_file", mapOf("file_id" to fileId))!!
}

/**
 * 转发单条信息到私聊
 *
 * @param userId 用户id
 * @param messageId 消息id
 */
suspend fun BotApi.forwardFriendSingleMsg(
    userId: String = this.receiveMessage.sender.id,
    messageId: String = this.receiveMessage.id
) {
    apiRequest<Unit>(
        "forward_friend_single_msg",
        mapOf("user_id" to userId, "message_id" to convertMessageId(messageId))
    )
}

/**
 * 转发单条信息到群聊
 *
 * @param groupId 群组id
 * @param messageId 消息id
 */
suspend fun BotApi.forwardGroupSingleMsg(
    groupId: String = this.receiveMessage.source.id,
    messageId: String = this.receiveMessage.id
) {
    apiRequest<Unit>(
        "forward_group_single_msg",
        mapOf("group_id" to groupId, "message_id" to convertMessageId(messageId))
    )
}

/**
 * 英译中翻译
 *
 * WARNING: 由于NapCatQQ自身原因会卡死
 *
 * @param words 内容
 */
suspend fun BotApi.translateEn2zh(vararg words: String): List<String> {
    return apiRequest<List<String>>("translate_en2zh", mapOf("words" to words.toList()))!!
}

/**
 * 贴表情
 *
 * @param messageId 消息id
 * @param emojiId 表情
 * @param set 设置
 */
suspend fun BotApi.setMsgEmojiLike(
    messageId: String = this.receiveMessage.id,
    emojiId: String,
    set: Boolean = true
) {
    apiRequest<Unit>(
        "set_msg_emoji_like",
        mapOf(
            "message_id" to convertMessageId(messageId),
            "emoji_id" to emojiId,
            "set" to set
        )
    )
}

/**
 * 标记私聊信息已读
 *
 * @param userId 用户id
 */
suspend fun BotApi.markPrivateMsgAsRead(userId: String = this.receiveMessage.sender.id) {
    apiRequest<Unit>("mark_private_msg_as_read", mapOf("user_id" to userId))
}

/**
 * 标记群聊信息已读
 *
 * @param groupId 群组id
 */
suspend fun BotApi.markGroupMsgAsRead(groupId: String = this.receiveMessage.source.id) {
    apiRequest<Unit>("mark_group_msg_as_read", mapOf("group_id" to groupId))
}

/**
 * 获取私聊记录
 *
 * @param userId 用户id
 * @param messageSeq 起始消息序号, 可通过GoCQHttpMessage获得
 * @param count 数量
 * @param reverseOrder 倒序
 */
suspend fun BotApi.getFriendMsgHistory(
    userId: String = this.receiveMessage.sender.id,
    messageSeq: Long,
    count: Int = 20,
    reverseOrder: Boolean = false
): List<GoCQHttpMessage> {
    return apiRequest<Map<String, List<GoCQHttpMessage>>>(
        "get_friend_msg_history",
        mapOf(
            "user_id" to userId,
            "message_seq" to messageSeq,
            "count" to count,
            "reverse_order" to reverseOrder
        )
    )!!["messages"]!!
}

/**
 * 创建文本收藏
 *
 * @param brief 标题
 * @param rawData 内容
 */
suspend fun BotApi.createCollection(brief: String, rawData: String) {
    apiRequest<Unit>("create_collection", mapOf("brief" to brief, "rawData" to rawData))
}

/**
 * 获取收藏列表
 *
 * WARNING: 由于NapCatQQ自身原因会出现不支持API
 *
 * @param category 类别
 * @param count 数量
 */
suspend fun BotApi.getCollectionList(category: String = "10", count: Int = 1): String {
    return apiRequest<String>("get_collection_list", mapOf("category" to category, "count" to count))!!
}

/**
 * 设置个人签名
 *
 * @param longNick 签名
 */
suspend fun BotApi.setSelfSignature(longNick: String) {
    apiRequest<Unit>("set_self_longnick", mapOf("longNick" to longNick))
}

/**
 * 获取最近的聊天记录
 */
suspend fun BotApi.getRecentContact(): ArrayNode {
    return apiRequest<ArrayNode>("get_recent_contact")!!
}

/**
 * 标记所有为已读
 */
suspend fun BotApi.markAllAsRead() {
    apiRequest<Unit>("_mark_all_as_read")
}

/**
 * 获取点赞列表
 *
 * WARNING: 由于NapCatQQ自身原因会出现"Cannot read properties of undefined (reading 'voteInfo')"异常
 */
suspend fun BotApi.getProfileLike(): ProfileLike {
    return apiRequest<ProfileLike>("get_profile_like")!!
}

/**
 * 获取收藏表情
 *
 * @param count 数量
 */
suspend fun BotApi.fetchCustomFace(count: Int = 40): List<String> {
    return apiRequest<List<String>>("fetch_custom_face", mapOf("count" to count))!!
}

/**
 * 设置输入状态
 *
 * @param userId 用户id
 * @param eventType 状态,0正在讲话,1正在输入
 */
suspend fun BotApi.setInputStatus(
    userId: String = this.receiveMessage.sender.id,
    eventType: String
) {
    apiRequest<Unit>("set_input_status", mapOf("user_id" to userId, "event_type" to eventType))
}

/**
 * 获取群组额外信息
 *
 * @param groupId 群组id
 */
suspend fun BotApi.getGroupInfoEx(groupId: String = this.receiveMessage.source.id): JsonNode {
    return apiRequest<JsonNode>("get_group_info_ex", mapOf("group_id" to groupId))!!
}

/**
 * 获取群组忽略的通知
 */
suspend fun BotApi.getGroupIgnoreAddRequest(): JsonNode {
    return apiRequest<JsonNode>("fetch_user_profile_like")!!
}

/**
 * 删除群聊公告
 *
 * WARNING: 无法测试 没有能获取公告列表的api
 *
 * @param groupId 群组id
 * @param noticeId 公告id
 */
suspend fun BotApi.delGroupNotice(
    groupId: String = this.receiveMessage.source.id,
    noticeId: String
) {
    apiRequest<Unit>("_del_group_notice", mapOf("group_id" to groupId, "notice_id" to noticeId))
}

/**
 * 获取用户个人资料页
 *
 * WARNING: 由于NapCatQQ自身原因会出现 Cannot read properties of undefined (reading 'toString') 异常
 *
 * @param userId 用户id
 */
suspend fun BotApi.fetchUserProfileLike(userId: String = this.receiveMessage.sender.id): String {
    return apiRequest<String>("fetch_user_profile_like", mapOf("qq" to userId))!!
}

/**
 * 获取PacketServer状态
 */
suspend fun BotApi.ncGetPacketStatus(): String? {
    return apiRequest<String>("nc_get_packet_status")
}

/**
 * 获取用户状态
 *
 * @param userId 用户id
 */
suspend fun BotApi.ncGetUserStatus(userId: String = this.receiveMessage.sender.id): BotOnlineStatusEnum? {
    val res = apiRequest<Map<String, Int>>("nc_get_user_status", mapOf("user_id" to userId))!!
    return BotOnlineStatusEnum.entries.singleOrNull {
        it.status == res["status"] && it.extStatus == res["extStatus"]
    }
}

/**
 * 获取RKey
 */
suspend fun BotApi.ncGetRKey(): List<NCRKey> {
    return apiRequest<List<NCRKey>>("nc_get_rkey")!!
}

/**
 * 获取群聊被禁言用户
 *
 * WARNING: 由于NapCatQQ自身原因会出现 ListenerName:NodeIKernelGroupListener/onShutUpMemberListChanged timeout 异常
 *
 * @param groupId 群组id
 */
suspend fun BotApi.getGroupShutList(groupId: String = this.receiveMessage.source.id): List<GroupShutInfo> {
    return apiRequest<List<GroupShutInfo>>("get_group_shut_list", mapOf("group_id" to groupId))!!
}

/**
 * 签名小程序卡片
 *
 * @param title 标题
 * @param desc 内容
 * @param picUrl 图片链接
 * @param jumpUrl 跳转链接
 * @param type 类型,可选: bili,weibo,如果不填写这两个,那么sdkId和sdkId之后的值必须要填写其中一个
 * @param iconUrl 图标
 * @param versionId 版本
 */
suspend fun BotApi.getMiniAppArk(
    title: String,
    desc: String,
    picUrl: String,
    jumpUrl: String,
    type: String? = null,
    iconUrl: String? = null,
    versionId: String? = null,
    sdkId: String? = null,
    appId: String? = null,
    scene: String? = null,
    templateType: String? = null,
    businessType: String? = null,
    verType: String? = null,
    shareType: String? = null,
    withShareTicket: String? = null,
    rawArkData: String? = null
): String {

    return apiRequest<String>(
        "get_mini_app_ark",
        mapOf(
            "title" to title,
            "desc" to desc,
            "picUrl" to picUrl,
            "jumpUrl" to jumpUrl,
            "type" to type,
            "iconUrl" to iconUrl,
            "versionId" to versionId,
            "sdkId" to sdkId,
            "appId" to appId,
            "scene" to scene,
            "templateType" to templateType,
            "businessType" to businessType,
            "verType" to verType,
            "shareType" to shareType,
            "withShareTicket" to withShareTicket,
            "rawArkData" to rawArkData
        )
    )!!
}

/**
 * AI文字转语音
 *
 * @param groupId 群组id
 * @param character ai角色id
 * @param text 内容
 * @return 语音链接
 */
suspend fun BotApi.getAiRecord(
    groupId: String = this.receiveMessage.source.id,
    character: String,
    text: String
): String {
    return apiRequest<String>(
        "get_ai_record",
        mapOf(
            "group_id" to groupId,
            "character" to character,
            "text" to text
        )
    )!!
}

/**
 * 获取AI语音角色列表
 *
 * @param groupId 群组id
 * @param chatType 类型
 */
suspend fun BotApi.getAiCharacters(
    groupId: String = this.receiveMessage.source.id,
    chatType: Int = 0
): AiCharacters {
    return apiRequest<AiCharacters>("get_ai_characters", mapOf("group_id" to groupId, "chat_type" to chatType))!!
}

/**
 * 群聊发送AI语音
 *
 * @param groupId 群组id
 * @param character ai角色id
 * @param text 内容
 */
suspend fun BotApi.sendGroupAiRecord(
    groupId: String = this.receiveMessage.source.id,
    character: String,
    text: String
) {

    apiRequest<Unit>(
        "send_group_ai_record",
        mapOf(
            "group_id" to groupId,
            "character" to character,
            "text" to text
        )
    )
}

private fun convertMessageId(messageId: String): String {
    return messageIdToCQMessageIdMap.asMap()
        .filter { it.value.contains(messageId) }
        .firstNotNullOfOrNull { it.key } ?: messageId
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