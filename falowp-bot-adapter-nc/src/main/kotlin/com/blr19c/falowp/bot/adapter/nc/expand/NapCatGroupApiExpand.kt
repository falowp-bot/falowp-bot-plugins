@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GetGroupNoticeDataItemMessageItem
 */
data class GetGroupNoticeDataItemMessageItem(
    @field:JsonProperty("id")
    val id: String,
    @field:JsonProperty("height")
    val height: String,
    @field:JsonProperty("width")
    val width: String
)

/**
 * GetGroupNoticeDataItem
 */
data class GetGroupNoticeDataItem(
    @field:JsonProperty("notice_id")
    val noticeId: String,
    @field:JsonProperty("sender_id")
    val senderId: Long,
    @field:JsonProperty("publish_time")
    val publishTime: Long,
    @field:JsonProperty("message")
    val message: List<GetGroupNoticeDataItemMessageItem>
)

/**
 * GetEssenceMsgListDataItem
 */
data class GetEssenceMsgListDataItem(
    @field:JsonProperty("msg_seq")
    val msgSeq: Long,
    @field:JsonProperty("msg_random")
    val msgRandom: Long,
    @field:JsonProperty("sender_id")
    val senderId: Long,
    @field:JsonProperty("sender_nick")
    val senderNick: String,
    @field:JsonProperty("operator_id")
    val operatorId: Long,
    @field:JsonProperty("operator_nick")
    val operatorNick: String,
    @field:JsonProperty("message_id")
    val messageId: Long,
    @field:JsonProperty("operator_time")
    val operatorTime: Long,
    @field:JsonProperty("content")
    val content: List<Any>
)

/**
 * GetGroupDetailInfoData
 */
data class GetGroupDetailInfoData(
    @field:JsonProperty("group_all_shut")
    val groupAllShut: Long,
    @field:JsonProperty("group_remark")
    val groupRemark: String,
    @field:JsonProperty("group_id")
    val groupId: Long,
    @field:JsonProperty("group_name")
    val groupName: String,
    @field:JsonProperty("member_count")
    val memberCount: Long,
    @field:JsonProperty("max_member_count")
    val maxMemberCount: Long
)

/**
 * GetGroupIgnoreAddRequestDataItem
 */
data class GetGroupIgnoreAddRequestDataItem(
    @field:JsonProperty("request_id")
    val requestId: Long,
    @field:JsonProperty("invitor_uin")
    val invitorUin: Long,
    @field:JsonProperty("invitor_nick")
    val invitorNick: String,
    @field:JsonProperty("group_id")
    val groupId: Long,
    @field:JsonProperty("message")
    val message: String,
    @field:JsonProperty("group_name")
    val groupName: String,
    @field:JsonProperty("checked")
    val checked: Boolean,
    @field:JsonProperty("actor")
    val actor: Long,
    @field:JsonProperty("requester_nick")
    val requesterNick: String
)

/**
 * GetGroupIgnoredNotifiesData
 */
data class GetGroupIgnoredNotifiesData(
    @field:JsonProperty("InvitedRequest")
    val InvitedRequest: List<Any>,
    @field:JsonProperty("join_requests")
    val joinRequests: List<Any>
)

/**
 * GetGroupShutListDataItem
 */
data class GetGroupShutListDataItem(
    @field:JsonProperty("uid")
    val uid: String,
    @field:JsonProperty("qid")
    val qid: String,
    @field:JsonProperty("uin")
    val uin: String,
    @field:JsonProperty("nick")
    val nick: String,
    @field:JsonProperty("remark")
    val remark: String,
    @field:JsonProperty("cardType")
    val cardType: Long,
    @field:JsonProperty("cardName")
    val cardName: String,
    @field:JsonProperty("role")
    val role: Long,
    @field:JsonProperty("avatarPath")
    val avatarPath: String,
    @field:JsonProperty("shutUpTime")
    val shutUpTime: Long,
    @field:JsonProperty("isDelete")
    val isDelete: Boolean,
    @field:JsonProperty("isSpecialConcerned")
    val isSpecialConcerned: Boolean,
    @field:JsonProperty("isSpecialShield")
    val isSpecialShield: Boolean,
    @field:JsonProperty("isRobot")
    val isRobot: Boolean,
    @field:JsonProperty("groupHonor")
    val groupHonor: NapCatRawData,
    @field:JsonProperty("memberRealLevel")
    val memberRealLevel: Long,
    @field:JsonProperty("memberLevel")
    val memberLevel: Long,
    @field:JsonProperty("globalGroupLevel")
    val globalGroupLevel: Long,
    @field:JsonProperty("globalGroupPoint")
    val globalGroupPoint: Long,
    @field:JsonProperty("memberTitleId")
    val memberTitleId: Long,
    @field:JsonProperty("memberSpecialTitle")
    val memberSpecialTitle: String,
    @field:JsonProperty("specialTitleExpireTime")
    val specialTitleExpireTime: String,
    @field:JsonProperty("userShowFlag")
    val userShowFlag: Long,
    @field:JsonProperty("userShowFlagNew")
    val userShowFlagNew: Long,
    @field:JsonProperty("richFlag")
    val richFlag: Long,
    @field:JsonProperty("mssVipType")
    val mssVipType: Long,
    @field:JsonProperty("bigClubLevel")
    val bigClubLevel: Long,
    @field:JsonProperty("bigClubFlag")
    val bigClubFlag: Long,
    @field:JsonProperty("autoRemark")
    val autoRemark: String,
    @field:JsonProperty("creditLevel")
    val creditLevel: Long,
    @field:JsonProperty("joinTime")
    val joinTime: Long,
    @field:JsonProperty("lastSpeakTime")
    val lastSpeakTime: Long,
    @field:JsonProperty("memberFlag")
    val memberFlag: Long,
    @field:JsonProperty("memberFlagExt")
    val memberFlagExt: Long,
    @field:JsonProperty("memberMobileFlag")
    val memberMobileFlag: Long,
    @field:JsonProperty("memberFlagExt2")
    val memberFlagExt2: Long,
    @field:JsonProperty("isSpecialShielded")
    val isSpecialShielded: Boolean,
    @field:JsonProperty("cardNameId")
    val cardNameId: Long
)

/**
 * NapCatGroupApiExpand
 */
class NapCatGroupApiExpand

/**
 * _删除群公告
 */
suspend fun NapCatBotApi.delGroupNotice(groupId: Long, noticeId: String) {
    apiRequestUnit("_del_group_notice", mapOf("group_id" to groupId, "notice_id" to noticeId))
}

/**
 * _获取群公告
 */
suspend fun NapCatBotApi.getGroupNotice(groupId: Long): List<GetGroupNoticeDataItem> {
    return apiRequest("_get_group_notice", mapOf("group_id" to groupId))
}

/**
 * 删除群精华消息
 */
suspend fun NapCatBotApi.deleteEssenceMsg(messageId: Long) {
    apiRequestUnit("delete_essence_msg", mapOf("message_id" to messageId))
}

/**
 * 获取群精华消息
 */
suspend fun NapCatBotApi.getEssenceMsgList(groupId: Long): List<GetEssenceMsgListDataItem> {
    return apiRequest("get_essence_msg_list", mapOf("group_id" to groupId))
}

/**
 * 获取群详细信息
 */
suspend fun NapCatBotApi.getGroupDetailInfo(groupId: Long): GetGroupDetailInfoData {
    return apiRequest("get_group_detail_info", mapOf("group_id" to groupId))
}

/**
 * 获取被过滤的加群请求
 */
suspend fun NapCatBotApi.getGroupIgnoreAddRequest(): List<GetGroupIgnoreAddRequestDataItem> {
    return apiRequest("get_group_ignore_add_request")
}

/**
 * 获取群过滤系统消息
 */
suspend fun NapCatBotApi.getGroupIgnoredNotifies(): GetGroupIgnoredNotifiesData {
    return apiRequest("get_group_ignored_notifies")
}

/**
 * 获取群信息
 */
suspend fun NapCatBotApi.getGroupInfo(groupId: Long): NapCatRawData {
    return apiRequest("get_group_info", mapOf("group_id" to groupId))
}

/**
 * 获取群列表
 */
suspend fun NapCatBotApi.getGroupList(noCache: Boolean): List<NapCatRawData> {
    return apiRequest("get_group_list", mapOf("no_cache" to noCache))
}

/**
 * 获取群成员信息
 */
suspend fun NapCatBotApi.getGroupMemberInfo(groupId: Long, userId: Long, noCache: Boolean): NapCatRawData {
    return apiRequest("get_group_member_info", mapOf("group_id" to groupId, "user_id" to userId, "no_cache" to noCache))
}

/**
 * 获取群成员列表
 */
suspend fun NapCatBotApi.getGroupMemberList(groupId: Long, noCache: Boolean? = null): List<NapCatRawData> {
    return apiRequest("get_group_member_list", mapOf("group_id" to groupId, "no_cache" to noCache))
}

/**
 * 获取群禁言列表
 */
suspend fun NapCatBotApi.getGroupShutList(groupId: Long): List<GetGroupShutListDataItem> {
    return apiRequest("get_group_shut_list", mapOf("group_id" to groupId))
}

/**
 * 设置群精华消息
 */
suspend fun NapCatBotApi.setEssenceMsg(messageId: Long) {
    apiRequestUnit("set_essence_msg", mapOf("message_id" to messageId))
}

/**
 * 处理加群请求
 */
suspend fun NapCatBotApi.setGroupAddRequest(flag: String, approve: Boolean, reason: String? = null) {
    apiRequestUnit("set_group_add_request", mapOf("flag" to flag, "approve" to approve, "reason" to reason))
}

/**
 * 设置群管理
 */
suspend fun NapCatBotApi.setGroupAdmin(groupId: Long, userId: Long, enable: Boolean) {
    apiRequestUnit("set_group_admin", mapOf("group_id" to groupId, "user_id" to userId, "enable" to enable))
}

/**
 * 群禁言
 */
suspend fun NapCatBotApi.setGroupBan(groupId: Long, userId: Long, duration: Long) {
    apiRequestUnit("set_group_ban", mapOf("group_id" to groupId, "user_id" to userId, "duration" to duration))
}

/**
 * 设置群成员名片
 */
suspend fun NapCatBotApi.setGroupCard(groupId: Long, userId: Long, card: String? = null) {
    apiRequestUnit("set_group_card", mapOf("group_id" to groupId, "user_id" to userId, "card" to card))
}

/**
 * 群踢人
 */
suspend fun NapCatBotApi.setGroupKick(groupId: Long, userId: Long, rejectAddRequest: Boolean? = null) {
    apiRequestUnit("set_group_kick", mapOf("group_id" to groupId, "user_id" to userId, "reject_add_request" to rejectAddRequest))
}

/**
 * 退群
 */
suspend fun NapCatBotApi.setGroupLeave(groupId: Long, isDismiss: Boolean? = null) {
    apiRequestUnit("set_group_leave", mapOf("group_id" to groupId, "is_dismiss" to isDismiss))
}

/**
 * 设置群名
 */
suspend fun NapCatBotApi.setGroupName(groupId: Long, groupName: String) {
    apiRequestUnit("set_group_name", mapOf("group_id" to groupId, "group_name" to groupName))
}

/**
 * 全体禁言
 */
suspend fun NapCatBotApi.setGroupWholeBan(groupId: Long, enable: Boolean) {
    apiRequestUnit("set_group_whole_ban", mapOf("group_id" to groupId, "enable" to enable))
}
