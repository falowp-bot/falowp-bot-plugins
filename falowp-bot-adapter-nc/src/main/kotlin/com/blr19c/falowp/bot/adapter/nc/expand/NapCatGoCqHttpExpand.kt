@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatGoCqHttpExpand
 */
class NapCatGoCqHttpExpand {
    /**
     * ModelShowItemItemVariants
     */
    data class ModelShowItemItemVariants(
        /**
         * 显示名称
         */
        @field:JsonProperty("model_show")
        val modelShow: String,
        /**
         * 是否需要付费
         */
        @field:JsonProperty("need_pay")
        val needPay: Boolean
    )

    /**
     * ModelShowItemItem
     */
    data class ModelShowItemItem(
        /**
         * variants
         */
        @field:JsonProperty("variants")
        val variants: ModelShowItemItemVariants
    )

    /**
     * CheckUrlSafely
     */
    data class CheckUrlSafely(
        /**
         * 安全等级 (1: 安全, 2: 未知, 3: 危险)
         */
        @field:JsonProperty("level")
        val level: Long
    )

    /**
     * DownloadFile
     */
    data class DownloadFile(
        /**
         * 文件路径
         */
        @field:JsonProperty("file")
        val file: String
    )

    /**
     * ForwardMsg
     */
    data class ForwardMsg(
        /**
         * 消息列表
         */
        @field:JsonProperty("messages")
        val messages: List<String>?
    )

    /**
     * FriendMsgHistory
     */
    data class FriendMsgHistory(
        /**
         * 消息列表
         */
        @field:JsonProperty("messages")
        val messages: List<String>
    )

    /**
     * GroupAtAllRemain
     */
    data class GroupAtAllRemain(
        /**
         * 是否可以艾特全体
         */
        @field:JsonProperty("can_at_all")
        val canAtAll: Boolean,
        /**
         * 群艾特全体剩余次数
         */
        @field:JsonProperty("remain_at_all_count_for_group")
        val remainAtAllCountForGroup: Long,
        /**
         * 个人艾特全体剩余次数
         */
        @field:JsonProperty("remain_at_all_count_for_uin")
        val remainAtAllCountForUin: Long
    )

    /**
     * GroupFileSystemInfo
     */
    data class GroupFileSystemInfo(
        /**
         * 文件总数
         */
        @field:JsonProperty("file_count")
        val fileCount: Long,
        /**
         * 文件上限
         */
        @field:JsonProperty("limit_count")
        val limitCount: Long,
        /**
         * 已使用空间
         */
        @field:JsonProperty("used_space")
        val usedSpace: Long,
        /**
         * 总空间
         */
        @field:JsonProperty("total_space")
        val totalSpace: Long
    )

    /**
     * GroupFilesByFolder
     */
    data class GroupFilesByFolder(
        /**
         * 文件列表
         */
        @field:JsonProperty("files")
        val files: List<String>,
        /**
         * 文件夹列表
         */
        @field:JsonProperty("folders")
        val folders: List<String>
    )

    /**
     * GroupHonorInfo
     */
    data class GroupHonorInfo(
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: Long,
        /**
         * 当前龙王
         */
        @field:JsonProperty("current_talkative")
        val currentTalkative: JsonNode,
        /**
         * 龙王列表
         */
        @field:JsonProperty("talkative_list")
        val talkativeList: List<String>,
        /**
         * 群聊之火列表
         */
        @field:JsonProperty("performer_list")
        val performerList: List<String>,
        /**
         * 群聊炽热列表
         */
        @field:JsonProperty("legend_list")
        val legendList: List<String>,
        /**
         * 快乐源泉列表
         */
        @field:JsonProperty("emotion_list")
        val emotionList: List<String>,
        /**
         * 冒尖小春笋列表
         */
        @field:JsonProperty("strong_newbie_list")
        val strongNewbieList: List<String>
    )

    /**
     * GroupMsgHistory
     */
    data class GroupMsgHistory(
        /**
         * 消息列表
         */
        @field:JsonProperty("messages")
        val messages: List<String>
    )

    /**
     * GroupRootFiles
     */
    data class GroupRootFiles(
        /**
         * 文件列表
         */
        @field:JsonProperty("files")
        val files: List<String>,
        /**
         * 文件夹列表
         */
        @field:JsonProperty("folders")
        val folders: List<String>
    )

    /**
     * StrangerInfo
     */
    data class StrangerInfo(
        /**
         * 用户QQ
         */
        @field:JsonProperty("user_id")
        val userId: Long,
        /**
         * UID
         */
        @field:JsonProperty("uid")
        val uid: String,
        /**
         * 昵称
         */
        @field:JsonProperty("nickname")
        val nickname: String,
        /**
         * 年龄
         */
        @field:JsonProperty("age")
        val age: Long,
        /**
         * QID
         */
        @field:JsonProperty("qid")
        val qid: String,
        /**
         * QQ等级
         */
        @field:JsonProperty("qqLevel")
        val qqLevel: Long,
        /**
         * 性别
         */
        @field:JsonProperty("sex")
        val sex: String,
        /**
         * 个性签名
         */
        @field:JsonProperty("long_nick")
        val longNick: String,
        /**
         * 注册时间
         */
        @field:JsonProperty("reg_time")
        val regTime: Long,
        /**
         * 是否VIP
         */
        @field:JsonProperty("is_vip")
        val isVip: Boolean,
        /**
         * 是否年费VIP
         */
        @field:JsonProperty("is_years_vip")
        val isYearsVip: Boolean,
        /**
         * VIP等级
         */
        @field:JsonProperty("vip_level")
        val vipLevel: Long,
        /**
         * 备注
         */
        @field:JsonProperty("remark")
        val remark: String,
        /**
         * 状态
         */
        @field:JsonProperty("status")
        val status: Long,
        /**
         * 登录天数
         */
        @field:JsonProperty("login_days")
        val loginDays: Long
    )

}

/**
 * 处理快速操作
 *
 * 处理来自事件上报的快速操作请求
 */
suspend fun NapCatBotApi..handleQuickOperation(context: Context, operation: Operation) {
    apiRequestUnit(".handle_quick_operation", mapOf("context" to context, "operation" to operation))
}

/**
 * 获取机型显示
 *
 * 获取当前账号可用的设备机型显示名称列表
 */
suspend fun NapCatBotApi.getModelShow(model: String? = null): NapCatGoCqHttpExpand.List<ModelShowItemItem> {
    return apiRequest("_get_model_show", mapOf("model" to model))
}

/**
 * 发送群公告
 *
 * 在指定群聊中发布新的公告
 */
suspend fun NapCatBotApi.sendGroupNotice(groupId: String, content: String, image: String? = null, pinned: Long, type: Long, confirmRequired: Long, isShowEditCard: Long, tipWindowType: Long) {
    apiRequestUnit("_send_group_notice", mapOf("group_id" to groupId, "content" to content, "image" to image, "pinned" to pinned, "type" to type, "confirm_required" to confirmRequired, "is_show_edit_card" to isShowEditCard, "tip_window_type" to tipWindowType))
}

/**
 * 设置机型
 *
 * 设置当前账号的设备机型名称
 */
suspend fun NapCatBotApi.setModelShow() {
    apiRequestUnit("_set_model_show")
}

/**
 * 检查URL安全性
 *
 * 检查指定URL的安全等级
 */
suspend fun NapCatBotApi.checkUrlSafely(url: String): NapCatGoCqHttpExpand.CheckUrlSafely {
    return apiRequest("check_url_safely", mapOf("url" to url))
}

/**
 * 创建群文件目录
 *
 * 在群文件系统中创建新的文件夹
 */
suspend fun NapCatBotApi.createGroupFileFolder(groupId: String, folderName: String? = null, name: String? = null) {
    apiRequestUnit("create_group_file_folder", mapOf("group_id" to groupId, "folder_name" to folderName, "name" to name))
}

/**
 * 删除好友
 *
 * 从好友列表中删除指定用户
 */
suspend fun NapCatBotApi.deleteFriend(friendId: String? = null, userId: String? = null, tempBlock: Boolean? = null, tempBothDel: Boolean? = null) {
    apiRequestUnit("delete_friend", mapOf("friend_id" to friendId, "user_id" to userId, "temp_block" to tempBlock, "temp_both_del" to tempBothDel))
}

/**
 * 删除群文件
 *
 * 在群文件系统中删除指定的文件
 */
suspend fun NapCatBotApi.deleteGroupFile(groupId: String, fileId: String) {
    apiRequestUnit("delete_group_file", mapOf("group_id" to groupId, "file_id" to fileId))
}

/**
 * 删除群文件目录
 *
 * 在群文件系统中删除指定的文件夹
 */
suspend fun NapCatBotApi.deleteGroupFolder(groupId: String, folderId: String? = null, folder: String? = null) {
    apiRequestUnit("delete_group_folder", mapOf("group_id" to groupId, "folder_id" to folderId, "folder" to folder))
}

/**
 * 下载文件
 *
 * 下载网络文件到本地临时目录
 */
suspend fun NapCatBotApi.downloadFile(url: String? = null, base64: String? = null, name: String? = null, headers: String? = null): NapCatGoCqHttpExpand.DownloadFile {
    return apiRequest("download_file", mapOf("url" to url, "base64" to base64, "name" to name, "headers" to headers))
}

/**
 * 获取合并转发消息
 *
 * 获取合并转发消息的具体内容
 */
suspend fun NapCatBotApi.getForwardMsg(messageId: String? = null, id: String? = null): NapCatGoCqHttpExpand.ForwardMsg {
    return apiRequest("get_forward_msg", mapOf("message_id" to messageId, "id" to id))
}

/**
 * 获取好友历史消息
 *
 * 获取指定好友的历史聊天记录
 */
suspend fun NapCatBotApi.getFriendMsgHistory(userId: String, messageSeq: String? = null, count: Long, reverseOrder: Boolean, disableGetUrl: Boolean, parseMultMsg: Boolean, quickReply: Boolean, reverseOrder: Boolean): NapCatGoCqHttpExpand.FriendMsgHistory {
    return apiRequest("get_friend_msg_history", mapOf("user_id" to userId, "message_seq" to messageSeq, "count" to count, "reverse_order" to reverseOrder, "disable_get_url" to disableGetUrl, "parse_mult_msg" to parseMultMsg, "quick_reply" to quickReply, "reverseOrder" to reverseOrder))
}

/**
 * 获取群艾特全体剩余次数
 *
 * 获取指定群聊中艾特全体成员的剩余次数
 */
suspend fun NapCatBotApi.getGroupAtAllRemain(groupId: String): NapCatGoCqHttpExpand.GroupAtAllRemain {
    return apiRequest("get_group_at_all_remain", mapOf("group_id" to groupId))
}

/**
 * 获取群文件系统信息
 *
 * 获取群聊文件系统的空间及状态信息
 */
suspend fun NapCatBotApi.getGroupFileSystemInfo(groupId: String): NapCatGoCqHttpExpand.GroupFileSystemInfo {
    return apiRequest("get_group_file_system_info", mapOf("group_id" to groupId))
}

/**
 * 获取群文件夹文件列表
 *
 * 获取指定群文件夹下的文件及子文件夹列表
 */
suspend fun NapCatBotApi.getGroupFilesByFolder(groupId: String, folderId: String? = null, folder: String? = null, fileCount: Long): NapCatGoCqHttpExpand.GroupFilesByFolder {
    return apiRequest("get_group_files_by_folder", mapOf("group_id" to groupId, "folder_id" to folderId, "folder" to folder, "file_count" to fileCount))
}

/**
 * 获取群荣誉信息
 *
 * 获取指定群聊的荣誉信息，如龙王等
 */
suspend fun NapCatBotApi.getGroupHonorInfo(groupId: String, type: String? = null): NapCatGoCqHttpExpand.GroupHonorInfo {
    return apiRequest("get_group_honor_info", mapOf("group_id" to groupId, "type" to type))
}

/**
 * 获取群历史消息
 *
 * 获取指定群聊的历史聊天记录
 */
suspend fun NapCatBotApi.getGroupMsgHistory(groupId: String, messageSeq: String? = null, count: Long, reverseOrder: Boolean, disableGetUrl: Boolean, parseMultMsg: Boolean, quickReply: Boolean, reverseOrder: Boolean): NapCatGoCqHttpExpand.GroupMsgHistory {
    return apiRequest("get_group_msg_history", mapOf("group_id" to groupId, "message_seq" to messageSeq, "count" to count, "reverse_order" to reverseOrder, "disable_get_url" to disableGetUrl, "parse_mult_msg" to parseMultMsg, "quick_reply" to quickReply, "reverseOrder" to reverseOrder))
}

/**
 * 获取群根目录文件列表
 *
 * 获取群文件根目录下的所有文件和文件夹
 */
suspend fun NapCatBotApi.getGroupRootFiles(groupId: String, fileCount: Long): NapCatGoCqHttpExpand.GroupRootFiles {
    return apiRequest("get_group_root_files", mapOf("group_id" to groupId, "file_count" to fileCount))
}

/**
 * 获取在线客户端
 *
 * 获取当前登录账号的在线客户端列表
 */
suspend fun NapCatBotApi.getOnlineClients(): NapCatGoCqHttpExpand.List<String> {
    return apiRequest("get_online_clients")
}

/**
 * 获取陌生人信息
 *
 * 获取指定非好友用户的信息
 */
suspend fun NapCatBotApi.getStrangerInfo(userId: String, noCache: Boolean): NapCatGoCqHttpExpand.StrangerInfo {
    return apiRequest("get_stranger_info", mapOf("user_id" to userId, "no_cache" to noCache))
}

/**
 * 发送合并转发消息
 *
 * 发送合并转发消息
 */
suspend fun NapCatBotApi.sendForwardMsg(messageType: String? = null, userId: String? = null, groupId: String? = null, message: List<MessageItem>, autoEscape: Boolean? = null, source: String? = null, news: List<NewsItem>? = null, summary: String? = null, prompt: String? = null) {
    apiRequestUnit("send_forward_msg", mapOf("message_type" to messageType, "user_id" to userId, "group_id" to groupId, "message" to message, "auto_escape" to autoEscape, "source" to source, "news" to news, "summary" to summary, "prompt" to prompt))
}

/**
 * 发送群合并转发消息
 */
suspend fun NapCatBotApi.sendGroupForwardMsg(messageType: String? = null, userId: String? = null, groupId: String? = null, message: List<MessageItem>, autoEscape: Boolean? = null, source: String? = null, news: List<NewsItem>? = null, summary: String? = null, prompt: String? = null) {
    apiRequestUnit("send_group_forward_msg", mapOf("message_type" to messageType, "user_id" to userId, "group_id" to groupId, "message" to message, "auto_escape" to autoEscape, "source" to source, "news" to news, "summary" to summary, "prompt" to prompt))
}

/**
 * 发送私聊合并转发消息
 */
suspend fun NapCatBotApi.sendPrivateForwardMsg(messageType: String? = null, userId: String? = null, groupId: String? = null, message: List<MessageItem>, autoEscape: Boolean? = null, source: String? = null, news: List<NewsItem>? = null, summary: String? = null, prompt: String? = null) {
    apiRequestUnit("send_private_forward_msg", mapOf("message_type" to messageType, "user_id" to userId, "group_id" to groupId, "message" to message, "auto_escape" to autoEscape, "source" to source, "news" to news, "summary" to summary, "prompt" to prompt))
}

/**
 * 设置群头像
 *
 * 修改指定群聊的头像
 */
suspend fun NapCatBotApi.setGroupPortrait(file: String, groupId: String) {
    apiRequestUnit("set_group_portrait", mapOf("file" to file, "group_id" to groupId))
}

/**
 * 设置QQ资料
 *
 * 修改当前账号的昵称、个性签名等资料
 */
suspend fun NapCatBotApi.setQqProfile(nickname: String, personalNote: String? = null, sex: Long? = null) {
    apiRequestUnit("set_qq_profile", mapOf("nickname" to nickname, "personal_note" to personalNote, "sex" to sex))
}

/**
 * 上传群文件
 *
 * 上传资源路径或URL指定的文件到指定群聊的文件系统中
 */
suspend fun NapCatBotApi.uploadGroupFile(groupId: String, file: String, name: String, folder: String? = null, folderId: String? = null, uploadFile: Boolean) {
    apiRequestUnit("upload_group_file", mapOf("group_id" to groupId, "file" to file, "name" to name, "folder" to folder, "folder_id" to folderId, "upload_file" to uploadFile))
}

/**
 * 上传私聊文件
 *
 * 上传本地文件到指定私聊会话中
 */
suspend fun NapCatBotApi.uploadPrivateFile(userId: String, file: String, name: String, uploadFile: Boolean) {
    apiRequestUnit("upload_private_file", mapOf("user_id" to userId, "file" to file, "name" to name, "upload_file" to uploadFile))
}
