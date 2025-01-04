@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.adapter.cq.expand

import com.blr19c.falowp.bot.adapter.cq.api.GoCQHttpMessage
import com.blr19c.falowp.bot.adapter.cq.api.GoCqHttpBotApiSupport.messageIdToCQMessageIdMap
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.listener.events.RequestAddFriendEvent
import com.blr19c.falowp.bot.system.listener.events.RequestJoinGroupEvent
import com.blr19c.falowp.bot.system.web.longTimeoutWebclient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.time.Duration

/**
 * 发送私聊消息
 *
 * @param groupId 群组id,存在时为主动发起临时会话时的来源群号,机器人本身必须是管理员/群主
 * @param userId 用户id
 * @param message 消息内容
 * @param autoEscape 消息内容是否作为纯文本发送(即不解析CQ码)
 * @return 消息id
 */
suspend fun BotApi.cqSendPrivateMsg(
    groupId: String? = null,
    userId: String = this.receiveMessage.sender.id,
    message: String,
    autoEscape: Boolean = false
): String {
    return apiRequest<Map<String, String>>(
        "send_private_msg",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "message" to message,
            "auto_escape" to autoEscape
        )
    )!!["message_id"]!!
}

/**
 * 发送群消息
 *
 * @param groupId 群组id
 * @param message 消息内容
 * @param autoEscape 消息内容是否作为纯文本发送(即不解析CQ码)
 * @return 消息id
 */
suspend fun BotApi.cqSendGroupMsg(
    groupId: String = this.receiveMessage.source.id,
    message: String,
    autoEscape: Boolean = false
): String {
    return apiRequest<Map<String, String>>(
        "send_group_msg",
        mapOf(
            "group_id" to groupId,
            "message" to message,
            "auto_escape" to autoEscape
        )
    )!!["message_id"]!!
}

/**
 * 发送合并转发(群聊)
 *
 * @param groupId 群组id
 * @param messages 自定义转发消息
 */
suspend fun BotApi.cqSendGroupForwardMsg(
    groupId: String = this.receiveMessage.source.id,
    messages: Any
): String {
    return apiRequest<Map<String, String>>(
        "send_group_forward_msg",
        mapOf(
            "group_id" to groupId,
            "messages" to messages
        )
    )!!["message_id"]!!
}

/**
 * 发送合并转发(好友)
 *
 * @param userId 用户id
 * @param messages 自定义转发消息
 */
suspend fun BotApi.cqSendPrivateForwardMsg(
    userId: String = this.receiveMessage.sender.id,
    messages: Any
): String {
    return apiRequest<Map<String, String>>(
        "send_private_forward_msg",
        mapOf(
            "user_id" to userId,
            "messages" to messages
        )
    )!!["message_id"]!!
}

/**
 * 发送消息
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param messageType 消息类型,支持private、group
 * @param message 消息内容
 * @param autoEscape 消息内容是否作为纯文本发送(即不解析CQ码)
 * @return 消息id
 */
suspend fun BotApi.cqSendMsg(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    messageType: String,
    message: String,
    autoEscape: Boolean = false
): String {
    return apiRequest<Map<String, String>>(
        "send_msg",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "message_type" to messageType,
            "message" to message,
            "auto_escape" to autoEscape
        )
    )!!["message_id"]!!
}

/**
 * 撤回消息
 */
suspend fun BotApi.cqDeleteMsg(messageId: String = this.receiveMessage.id) {
    apiRequest<Unit>("delete_msg", mapOf("message_id" to convertMessageId(messageId)))
}

/**
 * 获取消息
 */
suspend fun BotApi.cqGetMsg(messageId: String = this.receiveMessage.id): GoCQHttpMessage {
    return apiRequest("get_msg", mapOf("message_id" to convertMessageId(messageId)))!!
}

/**
 * 下载文件到缓存目录
 *
 * @param url 链接地址
 * @param threadCount 下载线程数
 * @param headers 请求头 例如:Referer=xx这种格式
 * @return 文件绝对路径
 */
suspend fun BotApi.cqDownloadFile(
    url: String,
    threadCount: Int = 3,
    headers: List<String> = emptyList()
): String {
    return apiRequest<Map<String, String>>(
        "download_file", mapOf(
            "url" to url,
            "thread_count" to threadCount,
            "headers" to headers.joinToString("""[\r\n]""")
        )
    )!!["file"]!!
}

/**
 * 检查链接安全性
 *
 * @param url
 * @return 安全等级, 1: 安全 2: 未知 3: 危险
 */
suspend fun BotApi.cqCheckUrlSafely(url: String): Int {
    return apiRequest<Map<String, Int>>("check_url_safely", mapOf("url" to url))!!["level"]!!
}

/**
 * 获取合并转发消息
 *
 * @param messageId 消息id
 */
suspend fun BotApi.cqGetForwardMsg(messageId: String = this.receiveMessage.id): List<GoCQHttpMessage> {
    return apiRequest<Map<String, List<GoCQHttpMessage>>>(
        "get_forward_msg",
        mapOf("message_id" to convertMessageId(messageId))
    )!!["messages"]!!
}

/**
 * 获取群消息历史记录
 *
 * @param groupId 群组id
 * @param messageSeq 起始消息序号, 可通过GoCQHttpMessage获得
 */
suspend fun BotApi.cqGetGroupMsgHistory(
    groupId: String = this.receiveMessage.source.id,
    messageSeq: Long,
): List<GoCQHttpMessage> {
    return apiRequest<Map<String, List<GoCQHttpMessage>>>(
        "get_group_msg_history",
        mapOf(
            "group_id" to groupId,
            "message_seq" to messageSeq
        )
    )!!["messages"]!!
}

/**
 * 获取群精华消息
 *
 * @param groupId 群组id
 */
suspend fun BotApi.cqGetEssenceMsgList(groupId: String = this.receiveMessage.source.id): List<EssenceMsg> {
    return apiRequest("get_essence_msg_list", mapOf("group_id" to groupId))!!
}

/**
 * 获取群@全体成员剩余次数
 *
 * @param groupId 群组id
 */
suspend fun BotApi.cqGetGroupAtAllRemain(groupId: String = this.receiveMessage.source.id): GroupAtAllRemain {
    return apiRequest("get_group_at_all_remain", mapOf("group_id" to groupId))!!
}

/**
 * 标记消息已读
 *
 * @param messageId 消息id
 */
suspend fun BotApi.cqMarkMsgAsRead(messageId: String = this.receiveMessage.id) {
    apiRequest<Unit>("mark_msg_as_read", mapOf("message_id" to convertMessageId(messageId)))
}

/**
 * 群组踢人
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param reject 是否以后拒绝申请
 */
suspend fun BotApi.cqSetGroupKick(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    reject: Boolean = false
) {
    apiRequest<Unit>(
        "set_group_kick",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "reject_add_request" to reject,
        )
    )
}

/**
 * 设置群单人禁言
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param duration 禁言时间
 */
suspend fun BotApi.cqSetGroupBan(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    duration: Duration,
) {
    apiRequest<Unit>(
        "set_group_ban",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "duration" to duration.inWholeSeconds,
        )
    )
}

/**
 * 设置全员群禁言
 *
 * @param enable 启用或者关闭
 * @param groupId 群组id
 */
suspend fun BotApi.cqSetGroupWholeBan(
    groupId: String = this.receiveMessage.source.id,
    enable: Boolean = true,
) {
    apiRequest<Unit>(
        "set_group_whole_ban",
        mapOf(
            "group_id" to groupId,
            "enable" to enable,
        )
    )
}

/**
 * 群组设置管理员
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param enabled 启用管理员
 */
suspend fun BotApi.cqSetGroupAdmin(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    enabled: Boolean = true,
) {
    apiRequest<Unit>(
        "set_group_admin",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "enabled" to enabled
        )
    )
}

/**
 * 设置群成员备注
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param card 备注
 */
suspend fun BotApi.cqSetGroupCard(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    card: String
) {
    apiRequest<Unit>(
        "set_group_card",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "card" to card,
        )
    )
}

/**
 * 设置群名称
 *
 * @param groupId 群组id
 * @param groupName 群名称
 */
suspend fun BotApi.cqSetGroupName(
    groupId: String = this.receiveMessage.source.id,
    groupName: String
) {
    apiRequest<Unit>(
        "set_group_name",
        mapOf(
            "group_id" to groupId,
            "group_name" to groupName,
        )
    )
}

/**
 * 设置群头像
 *
 * @param groupId 群组id
 * @param file 文件(支持文件路径/网络url/base64编码)
 * @param cache 是否使用缓存
 */
suspend fun BotApi.cqSetGroupPortrait(
    groupId: String = this.receiveMessage.source.id,
    file: String,
    cache: Boolean = true
) {
    apiRequest<Unit>(
        "set_group_portrait",
        mapOf(
            "group_id" to groupId,
            "file" to file,
            "cache" to if (cache) "1" else "0"
        )
    )
}

/**
 * 设置群精华消息
 *
 * @param messageId 消息id
 */
suspend fun BotApi.cqSetEssenceMsg(messageId: String = this.receiveMessage.id) {
    apiRequest<Unit>("set_essence_msg", mapOf("message_id" to convertMessageId(messageId)))
}

/**
 * 移出精华消息
 *
 * @param messageId 消息id
 */
suspend fun BotApi.cqDeleteEssenceMsg(messageId: String = this.receiveMessage.id) {
    apiRequest<Unit>("delete_essence_msg", mapOf("message_id" to convertMessageId(messageId)))
}

/**
 * 群打卡
 *
 * @param groupId 群组id
 */
suspend fun BotApi.cqSendGroupSign(groupId: String = this.receiveMessage.source.id) {
    apiRequest<Unit>("send_group_sign", mapOf("group_id" to groupId))
}

/**
 * 发送群公告
 *
 * @param groupId 群组id
 * @param content 公告内容
 * @param image 图片路径
 */
suspend fun BotApi.cqSendGroupNotice(
    groupId: String = this.receiveMessage.source.id,
    content: String,
    image: String? = null
) {
    val request = mutableMapOf(
        "group_id" to groupId,
        "content" to content
    )
    image?.let { request["image"] = it }
    apiRequest<Unit>("_send_group_notice", request)
}

/**
 * 获取群公告
 *
 * @param groupId 群组id
 */
suspend fun BotApi.cqGetGroupNotice(groupId: String = this.receiveMessage.source.id): GroupNotice {
    return apiRequest("_get_group_notice", mapOf("group_id" to groupId))!!
}

/**
 * 上传群文件
 *
 * @param groupId 群组id
 * @param file 本地文件路径
 * @param name 文件储存名称
 * @param groupFolder 文件上传位置,不传为根目录
 */
suspend fun BotApi.cqUploadGroupFile(
    groupId: String = this.receiveMessage.source.id,
    file: String,
    name: String,
    groupFolder: GroupFolder? = null
) {
    val request = mutableMapOf(
        "group_id" to groupId,
        "file" to file,
        "name" to name,
    )
    groupFolder?.let { request["folder"] = groupFolder.folderId }
    apiRequest<Unit>("upload_group_file", request)
}

/**
 * 删除群文件
 */
suspend fun BotApi.cqDeleteGroupFile(groupFile: GroupFile) {
    apiRequest<Unit>(
        "delete_group_file",
        mapOf(
            "group_id" to groupFile.groupId,
            "file_id" to groupFile.fileId,
            "busid" to groupFile.busId
        )
    )
}

/**
 * 创建群文件文件夹,仅能在根目录创建文件夹
 *
 * @param groupId 群组id
 * @param name 文件夹名称
 */
suspend fun BotApi.cqCreateGroupFileFolder(
    groupId: String = this.receiveMessage.source.id,
    name: String
) {
    apiRequest<Unit>(
        "create_group_file_folder",
        mapOf(
            "group_id" to groupId,
            "name" to name,
            "parent_id" to "/"
        )
    )
}

/**
 * 删除群文件文件夹
 *
 * @param groupFolder 目录
 */
suspend fun BotApi.cqDeleteGroupFolder(groupFolder: GroupFolder) {
    apiRequest<Unit>(
        "delete_group_folder",
        mapOf(
            "group_id" to groupFolder.groupId,
            "folder_id" to groupFolder.folderId
        )
    )
}

/**
 * 获取群文件系统信息
 *
 * @param groupId 群组id
 */
suspend fun BotApi.cqGetGroupFileSystemInfo(groupId: String = this.receiveMessage.source.id): GroupFileSystemInfo {
    return apiRequest("get_group_file_system_info", mapOf("group_id" to groupId))!!
}

/**
 * 获取群根目录文件列表
 *
 * @param groupId 群组id
 */
suspend fun BotApi.cqGetGroupRootFiles(groupId: String = this.receiveMessage.source.id): GroupFileInfo {
    return apiRequest("get_group_root_files", mapOf("group_id" to groupId))!!
}

/**
 * 获取群子目录文件列表
 *
 * @param groupFolder 父目录
 */
suspend fun BotApi.cqGetGroupFilesByFolder(groupFolder: GroupFolder): GroupFileInfo {
    return apiRequest(
        "get_group_files_by_folder",
        mapOf(
            "group_id" to groupFolder.groupId,
            "folder_id" to groupFolder.folderId
        )
    )!!
}

/**
 * 获取群文件资源链接
 *
 * @param groupFile 群组文件
 */
suspend fun BotApi.cqGetGroupFileUrl(groupFile: GroupFile): String {
    return apiRequest<Map<String, String>>(
        "get_group_file_url",
        mapOf(
            "group_id" to groupFile.groupId,
            "file_id" to groupFile.fileId,
            "busid" to groupFile.busId
        )
    )!!["url"]!!
}

/**
 * 退出群组
 *
 * @param groupId 群组id
 * @param isDismiss 是否解散
 */
suspend fun BotApi.cqSetGroupLeave(
    groupId: String = this.receiveMessage.source.id,
    isDismiss: Boolean = false
) {
    apiRequest<Unit>(
        "set_group_leave",
        mapOf("group_id" to groupId, "is_dismiss" to isDismiss)
    )
}

/**
 * 设置群组专属头衔
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param specialTitle 头衔 为空则删除头衔
 */
suspend fun BotApi.cqSetGroupSpecialTitle(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    specialTitle: String = "",
) {
    apiRequest<Unit>(
        "set_group_special_title",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "special_title" to specialTitle,
        )
    )
}

/**
 * 上传私聊文件
 *
 * @param userId 用户id
 * @param file 本地文件路径
 * @param fileName 文件名称
 */
suspend fun BotApi.cqUploadPrivateFile(
    userId: String = this.receiveMessage.sender.id,
    file: String,
    fileName: String
) {
    apiRequest<Unit>(
        "upload_private_file",
        mapOf(
            "user_id" to userId,
            "file" to file,
            "name" to fileName,
        )
    )
}

/**
 * 处理加好友请求
 *
 * @param event 加好友事件
 * @param approve 是否同意
 * @param reason 如果拒绝,拒绝原因
 */
suspend fun BotApi.cqSetFriendAddRequest(event: RequestAddFriendEvent, approve: Boolean, reason: String = "") {
    apiRequest<Unit>(
        "set_friend_add_request",
        mapOf(
            "flag" to event.flag.toString(),
            "approve" to approve,
            "reason" to reason
        )
    )
}

/**
 * 删除好友
 *
 * @param userId 用户id
 */
suspend fun BotApi.cqDeleteFriend(userId: String = this.receiveMessage.sender.id) {
    apiRequest<Unit>("delete_friend", mapOf("user_id" to userId))
}

/**
 * 处理加群请求/邀请
 *
 * @param event 加群事件
 * @param approve 是否同意
 * @param reason 如果拒绝,拒绝原因
 */
suspend fun BotApi.cqSetGroupAddRequest(event: RequestJoinGroupEvent, approve: Boolean, reason: String = "") {
    apiRequest<Unit>(
        "set_group_add_request",
        mapOf(
            "flag" to event.flag.toString(),
            "sub_type" to event.type,
            "approve" to approve,
            "reason" to reason
        )
    )
}

/**
 * 图片OCR
 *
 * @param image 图片id
 */
suspend fun BotApi.cqOcrImage(image: String): OcrImage {
    return apiRequest("ocr_image", mapOf("image" to image))!!
}

/**
 * 获取登录号信息
 */
suspend fun BotApi.cqGetLoginInfo(): LoginInfo {
    return apiRequest("get_login_info")!!
}

/**
 * 获取陌生人信息
 *
 * @param userId 用户id
 */
suspend fun BotApi.cqGetStrangerInfo(userId: String = this.receiveMessage.sender.id): StrangerInfo {
    return apiRequest(
        "get_stranger_info",
        mapOf("user_id" to userId)
    )!!
}

/**
 * 获取好友列表
 */
suspend fun BotApi.cqGetFriendList(): List<FriendInfo> {
    return apiRequest("get_friend_list")!!
}

/**
 * 获取群信息
 *
 * @param groupId 群组id
 * @param cache 缓存
 */
suspend fun BotApi.cqGetGroupInfo(
    groupId: String = this.receiveMessage.source.id,
    cache: Boolean = true
): GroupInfo {
    return apiRequest(
        "get_group_info",
        mapOf("group_id" to groupId, "no_cache" to !cache)
    )!!
}

/**
 * 获取群列表
 *
 * @param cache 缓存
 */
suspend fun BotApi.cqGetGroupList(cache: Boolean = true): List<GroupInfo> {
    return apiRequest("get_group_list", mapOf("no_cache" to !cache))!!
}

/**
 * 获取群成员信息
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param cache 缓存
 */
suspend fun BotApi.cqGetGroupMemberInfo(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    cache: Boolean = true
): GroupMemberInfo {
    return apiRequest(
        "get_group_member_info",
        mapOf("group_id" to groupId, "user_id" to userId, "no_cache" to !cache)
    )!!
}

/**
 * 获取群成员列表
 *
 * @param groupId 群组id
 * @param cache 缓存
 */
suspend fun BotApi.cqGetGroupMemberList(
    groupId: String = this.receiveMessage.source.id,
    cache: Boolean = true
): List<GroupMemberRoughInfo> {
    return apiRequest(
        "get_group_member_list",
        mapOf("group_id" to groupId, "no_cache" to !cache)
    )!!
}

/**
 * 获取群荣誉信息
 *
 * @param groupId 群组id
 * @param type 要获取的群荣誉类型,可传入 talkative performer legend strong_newbie emotion 以分别获取单个类型的群荣誉数据 或传入 all 获取所有数据
 */
suspend fun BotApi.cqGetGroupHonorInfo(
    groupId: String = this.receiveMessage.source.id,
    type: String = "all"
): GroupHonorInfo {
    return apiRequest(
        "get_group_honor_info",
        mapOf("group_id" to groupId, "type" to type)
    )!!
}

/**
 * 获取语音
 *
 * @param file 收到的语音文件名(消息段的file参数)
 * @param outFormat 要转换到的格式,目前支持 mp3、amr、wma、m4a、spx、ogg、wav、flac
 * @return 转换后的语音文件路径
 */
suspend fun BotApi.cqGetRecord(file: String, outFormat: String = "mp3"): String {
    return apiRequest<Map<String, String>>(
        "get_record",
        mapOf("file" to file, "out_format" to outFormat)
    )!!["file"]!!
}

/**
 * 获取图片
 *
 * @param file 文件缓存名
 */
suspend fun BotApi.cqGetImage(file: String): Image? {
    return apiRequest("get_image", mapOf("file" to file))!!
}

/**
 * 获取头像
 *
 * @param userId 用户id
 */
fun BotApi.cqAvatar(userId: String = this.receiveMessage.sender.id): ImageUrl {
    return ImageUrl("https://q1.qlogo.cn/g?b=qq&nk=$userId&s=640")
}

/**
 * 检查是否可以发送图片
 */
suspend fun BotApi.cqCanSendImage(): Boolean {
    return apiRequest<Map<String, Boolean>>("can_send_image")!!["yes"]!!
}

/**
 * 检查是否可以发送语音
 */
suspend fun BotApi.cqCanSendRecord(): Boolean {
    return apiRequest<Map<String, Boolean>>("can_send_record")!!["yes"]!!
}

/**
 * 获取运行状态
 */
suspend fun BotApi.cqGetStatus(): Status {
    return apiRequest("get_status")!!
}

/**
 * 获取版本信息
 */
suspend fun BotApi.cqGetVersionInfo(): VersionInfo {
    return apiRequest("get_version_info")!!
}

/**
 * 清理缓存
 */
suspend fun BotApi.cqCleanCache() {
    apiRequest<Unit>("clean_cache")
}

/**
 * 设置登录号资料
 *
 * @param nickname 名称
 * @param company 公司
 * @param email 邮箱
 * @param college 学校
 * @param personalNote 个人说明
 */
suspend fun BotApi.cqSetQQProfile(
    nickname: String,
    company: String,
    email: String,
    college: String,
    personalNote: String
) {
    apiRequest<Unit>(
        "set_qq_profile",
        mapOf(
            "nickname" to nickname,
            "company" to company,
            "email" to email,
            "college" to college,
            "personal_note" to personalNote,
        )
    )
}

/**
 * 获取当前账号在线客户端列表
 */
suspend fun BotApi.cqGetOnlineClients(cache: Boolean = true): OnlineClients {
    return apiRequest(
        "get_online_clients",
        mapOf("no_cache" to !cache)
    )!!
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
    val result = Json.readObj<GoCqApiResult<T>>(resBytes)
    if (result.success()) {
        return result.data
    }
    throw IllegalStateException("GoCQHttp适配器请求API失败:${result.msg},${result.wording}")
}