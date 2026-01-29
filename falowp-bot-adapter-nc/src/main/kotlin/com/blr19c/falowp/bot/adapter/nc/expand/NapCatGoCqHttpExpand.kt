@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GetModelShowDataItemVariants
 */
data class GetModelShowDataItemVariants(
    @field:JsonProperty("model_show")
    val modelShow: String,
    @field:JsonProperty("need_pay")
    val needPay: Boolean
)

/**
 * GetModelShowDataItem
 */
data class GetModelShowDataItem(
    @field:JsonProperty("variants")
    val variants: GetModelShowDataItemVariants
)

/**
 * DownloadFileData
 */
data class DownloadFileData(
    @field:JsonProperty("file")
    val file: String
)

/**
 * GetForwardMsgData
 */
data class GetForwardMsgData(
    @field:JsonProperty("messages")
    val messages: List<Any>
)

/**
 * GetFriendMsgHistoryData
 */
data class GetFriendMsgHistoryData(
    @field:JsonProperty("messages")
    val messages: List<Any>
)

/**
 * GetGroupAtAllRemainData
 */
data class GetGroupAtAllRemainData(
    @field:JsonProperty("can_at_all")
    val canAtAll: Boolean,
    @field:JsonProperty("remain_at_all_count_for_group")
    val remainAtAllCountForGroup: Long,
    @field:JsonProperty("remain_at_all_count_for_uin")
    val remainAtAllCountForUin: Long
)

/**
 * GetGroupFileSystemInfoData
 */
data class GetGroupFileSystemInfoData(
    @field:JsonProperty("file_count")
    val fileCount: Long,
    @field:JsonProperty("limit_count")
    val limitCount: Long,
    @field:JsonProperty("used_space")
    val usedSpace: Long,
    @field:JsonProperty("total_space")
    val totalSpace: Long
)

/**
 * GetGroupFilesByFolderData
 */
data class GetGroupFilesByFolderData(
    @field:JsonProperty("files")
    val files: List<Any>,
    @field:JsonProperty("folders")
    val folders: List<Any>?
)

/**
 * GetGroupHonorInfoData
 */
data class GetGroupHonorInfoData(
    @field:JsonProperty("group_id")
    val groupId: String,
    @field:JsonProperty("current_talkative")
    val currentTalkative: Any,
    @field:JsonProperty("talkative_list")
    val talkativeList: List<Any>,
    @field:JsonProperty("performer_list")
    val performerList: List<Any>,
    @field:JsonProperty("legend_list")
    val legendList: List<Any>,
    @field:JsonProperty("emotion_list")
    val emotionList: List<Any>,
    @field:JsonProperty("strong_newbie_list")
    val strongNewbieList: List<Any>
)

/**
 * GetGroupMsgHistoryData
 */
data class GetGroupMsgHistoryData(
    @field:JsonProperty("messages")
    val messages: List<Any>
)

/**
 * GetGroupRootFilesData
 */
data class GetGroupRootFilesData(
    @field:JsonProperty("files")
    val files: List<Any>,
    @field:JsonProperty("folders")
    val folders: List<Any>
)

/**
 * GetStrangerInfoData
 */
data class GetStrangerInfoData(
    @field:JsonProperty("user_id")
    val userId: Long,
    @field:JsonProperty("uid")
    val uid: String,
    @field:JsonProperty("uin")
    val uin: String,
    @field:JsonProperty("nickname")
    val nickname: String,
    @field:JsonProperty("age")
    val age: Long,
    @field:JsonProperty("qid")
    val qid: String,
    @field:JsonProperty("qqLevel")
    val qqLevel: Long,
    @field:JsonProperty("sex")
    val sex: String,
    @field:JsonProperty("long_nick")
    val longNick: String,
    @field:JsonProperty("reg_time")
    val regTime: Long,
    @field:JsonProperty("is_vip")
    val isVip: Boolean,
    @field:JsonProperty("is_years_vip")
    val isYearsVip: Boolean,
    @field:JsonProperty("vip_level")
    val vipLevel: Long,
    @field:JsonProperty("remark")
    val remark: String,
    @field:JsonProperty("status")
    val status: Long,
    @field:JsonProperty("login_days")
    val loginDays: Long
)

/**
 * NapCatGoCqHttpExpand
 */
class NapCatGoCqHttpExpand

/**
 * .对事件执行快速操作
 *
 * 相当于http的快速操作
 */
suspend fun NapCatBotApi..handleQuickOperation(context: NapCatRawData, operation: NapCatRawData) {
    apiRequestUnit(".handle_quick_operation", mapOf("context" to context, "operation" to operation))
}

/**
 * _获取在线机型
 */
suspend fun NapCatBotApi.getModelShow(model: String): List<GetModelShowDataItem> {
    return apiRequest("_get_model_show", mapOf("model" to model))
}

/**
 * _发送群公告
 */
suspend fun NapCatBotApi.sendGroupNotice(groupId: Long, content: String, image: String? = null, pinned: Any? = null, type: Any? = null, confirmRequired: Any? = null, isShowEditCard: Any? = null, tipWindowType: Any? = null) {
    apiRequestUnit("_send_group_notice", mapOf("group_id" to groupId, "content" to content, "image" to image, "pinned" to pinned, "type" to type, "confirm_required" to confirmRequired, "is_show_edit_card" to isShowEditCard, "tip_window_type" to tipWindowType))
}

/**
 * _设置在线机型
 */
suspend fun NapCatBotApi.setModelShow() {
    apiRequestUnit("_set_model_show")
}

/**
 * 检查链接安全性
 */
suspend fun NapCatBotApi.checkUrlSafely(): NapCatRawData {
    return apiRequest("check_url_safely")
}

/**
 * 创建群文件文件夹
 */
suspend fun NapCatBotApi.createGroupFileFolder(groupId: Long, folderName: String) {
    apiRequestUnit("create_group_file_folder", mapOf("group_id" to groupId, "folder_name" to folderName))
}

/**
 * 删除好友
 */
suspend fun NapCatBotApi.deleteFriend(userId: Long? = null, friendId: Long? = null, tempBlock: Boolean, tempBothDel: Boolean) {
    apiRequestUnit("delete_friend", mapOf("user_id" to userId, "friend_id" to friendId, "temp_block" to tempBlock, "temp_both_del" to tempBothDel))
}

/**
 * 删除群文件
 */
suspend fun NapCatBotApi.deleteGroupFile(groupId: Long, fileId: String) {
    apiRequestUnit("delete_group_file", mapOf("group_id" to groupId, "file_id" to fileId))
}

/**
 * 删除群文件夹
 */
suspend fun NapCatBotApi.deleteGroupFolder(groupId: Long, folderId: String) {
    apiRequestUnit("delete_group_folder", mapOf("group_id" to groupId, "folder_id" to folderId))
}

/**
 * 下载文件到缓存目录
 */
suspend fun NapCatBotApi.downloadFile(url: String? = null, base64: String? = null, name: String? = null, headers: String? = null): DownloadFileData {
    return apiRequest("download_file", mapOf("url" to url, "base64" to base64, "name" to name, "headers" to headers))
}

/**
 * 获取合并转发消息
 */
suspend fun NapCatBotApi.getForwardMsg(messageId: String): GetForwardMsgData {
    return apiRequest("get_forward_msg", mapOf("message_id" to messageId))
}

/**
 * 获取好友历史消息
 */
suspend fun NapCatBotApi.getFriendMsgHistory(userId: Long, messageSeq: Long? = null, count: Long? = null, reverseOrder: Boolean? = null): GetFriendMsgHistoryData {
    return apiRequest("get_friend_msg_history", mapOf("user_id" to userId, "message_seq" to messageSeq, "count" to count, "reverseOrder" to reverseOrder))
}

/**
 * 获取群 @全体成员 剩余次数
 */
suspend fun NapCatBotApi.getGroupAtAllRemain(groupId: Long): GetGroupAtAllRemainData {
    return apiRequest("get_group_at_all_remain", mapOf("group_id" to groupId))
}

/**
 * 获取群文件系统信息
 */
suspend fun NapCatBotApi.getGroupFileSystemInfo(groupId: Long): GetGroupFileSystemInfoData {
    return apiRequest("get_group_file_system_info", mapOf("group_id" to groupId))
}

/**
 * 获取群子目录文件列表
 */
suspend fun NapCatBotApi.getGroupFilesByFolder(groupId: Long, folderId: String? = null, folder: String? = null, fileCount: Long? = null): GetGroupFilesByFolderData {
    return apiRequest("get_group_files_by_folder", mapOf("group_id" to groupId, "folder_id" to folderId, "folder" to folder, "file_count" to fileCount))
}

/**
 * 获取群荣誉
 *
 * |  type                   |         类型                    |
|  ----------------- | ------------------------ |
| all                       |  所有（默认）             |
| talkative              | 群聊之火                     |
| performer           | 群聊炽焰                     |
| legend                | 龙王                             |
| strong_newbie   | 冒尖小春笋（R.I.P）     |
| emotion              | 快乐源泉                      |
 */
suspend fun NapCatBotApi.getGroupHonorInfo(groupId: Long, type: String? = null): GetGroupHonorInfoData {
    return apiRequest("get_group_honor_info", mapOf("group_id" to groupId, "type" to type))
}

/**
 * 获取群历史消息
 */
suspend fun NapCatBotApi.getGroupMsgHistory(groupId: Long, messageSeq: Long? = null, count: Long? = null, reverseOrder: Boolean? = null): GetGroupMsgHistoryData {
    return apiRequest("get_group_msg_history", mapOf("group_id" to groupId, "message_seq" to messageSeq, "count" to count, "reverseOrder" to reverseOrder))
}

/**
 * 获取群根目录文件列表
 */
suspend fun NapCatBotApi.getGroupRootFiles(groupId: Long, fileCount: Long? = null): GetGroupRootFilesData {
    return apiRequest("get_group_root_files", mapOf("group_id" to groupId, "file_count" to fileCount))
}

/**
 * 获取当前账号在线客户端列表
 */
suspend fun NapCatBotApi.getOnlineClients(): List<String> {
    return apiRequest("get_online_clients")
}

/**
 * 获取账号信息
 */
suspend fun NapCatBotApi.getStrangerInfo(userId: Long): GetStrangerInfoData {
    return apiRequest("get_stranger_info", mapOf("user_id" to userId))
}

/**
 * 发送合并转发消息
 */
suspend fun NapCatBotApi.sendForwardMsg(groupId: Long? = null, userId: Long? = null, messages: List<Any>, news: List<NewsItem>, prompt: String, summary: String, source: String) {
    apiRequestUnit("send_forward_msg", mapOf("group_id" to groupId, "user_id" to userId, "messages" to messages, "news" to news, "prompt" to prompt, "summary" to summary, "source" to source))
}

/**
 * 发送群合并转发消息
 */
suspend fun NapCatBotApi.sendGroupForwardMsg(groupId: Long, messages: List<Any>, news: List<NewsItem>, prompt: String, summary: String, source: String) {
    apiRequestUnit("send_group_forward_msg", mapOf("group_id" to groupId, "messages" to messages, "news" to news, "prompt" to prompt, "summary" to summary, "source" to source))
}

/**
 * 发送私聊合并转发消息
 */
suspend fun NapCatBotApi.sendPrivateForwardMsg(userId: Long, messages: List<MessagesItem>) {
    apiRequestUnit("send_private_forward_msg", mapOf("user_id" to userId, "messages" to messages))
}

/**
 * 设置群头像
 */
suspend fun NapCatBotApi.setGroupPortrait(groupId: Long, file: String) {
    apiRequestUnit("set_group_portrait", mapOf("group_id" to groupId, "file" to file))
}

/**
 * 设置账号信息
 */
suspend fun NapCatBotApi.setQqProfile(nickname: String, personalNote: String? = null, sex: String? = null) {
    apiRequestUnit("set_qq_profile", mapOf("nickname" to nickname, "personal_note" to personalNote, "sex" to sex))
}

/**
 * 上传群文件
 */
suspend fun NapCatBotApi.uploadGroupFile(groupId: Long, file: String, name: String, folder: String? = null, folderId: String? = null) {
    apiRequestUnit("upload_group_file", mapOf("group_id" to groupId, "file" to file, "name" to name, "folder" to folder, "folder_id" to folderId))
}

/**
 * 上传私聊文件
 */
suspend fun NapCatBotApi.uploadPrivateFile(userId: Long, file: String, name: String) {
    apiRequestUnit("upload_private_file", mapOf("user_id" to userId, "file" to file, "name" to name))
}
