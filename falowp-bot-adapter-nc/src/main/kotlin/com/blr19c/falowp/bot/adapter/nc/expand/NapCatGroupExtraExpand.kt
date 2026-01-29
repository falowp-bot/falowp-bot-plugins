@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GetGroupInfoExDataExtInfoGroupOwnerId
 */
data class GetGroupInfoExDataExtInfoGroupOwnerId(
    @field:JsonProperty("memberUin")
    val memberUin: String,
    @field:JsonProperty("memberUid")
    val memberUid: String,
    @field:JsonProperty("memberQid")
    val memberQid: String
)

/**
 * GetGroupInfoExDataExtInfoGroupBindGuildIds
 */
data class GetGroupInfoExDataExtInfoGroupBindGuildIds(
    @field:JsonProperty("guildIds")
    val guildIds: List<String>
)

/**
 * GetGroupInfoExDataExtInfoGroupExtFlameData
 */
data class GetGroupInfoExDataExtInfoGroupExtFlameData(
    @field:JsonProperty("switchState")
    val switchState: Int,
    @field:JsonProperty("state")
    val state: Int,
    @field:JsonProperty("dayNums")
    val dayNums: List<String>,
    @field:JsonProperty("version")
    val version: Int,
    @field:JsonProperty("updateTime")
    val updateTime: String,
    @field:JsonProperty("isDisplayDayNum")
    val isDisplayDayNum: Boolean
)

/**
 * GetGroupInfoExDataExtInfoGroupExcludeGuildIds
 */
data class GetGroupInfoExDataExtInfoGroupExcludeGuildIds(
    @field:JsonProperty("guildIds")
    val guildIds: List<String>
)

/**
 * GetGroupInfoExDataExtInfo
 */
data class GetGroupInfoExDataExtInfo(
    @field:JsonProperty("groupInfoExtSeq")
    val groupInfoExtSeq: Long,
    @field:JsonProperty("reserve")
    val reserve: Long,
    @field:JsonProperty("luckyWordId")
    val luckyWordId: String,
    @field:JsonProperty("lightCharNum")
    val lightCharNum: Long,
    @field:JsonProperty("luckyWord")
    val luckyWord: String,
    @field:JsonProperty("starId")
    val starId: Long,
    @field:JsonProperty("essentialMsgSwitch")
    val essentialMsgSwitch: Long,
    @field:JsonProperty("todoSeq")
    val todoSeq: Long,
    @field:JsonProperty("blacklistExpireTime")
    val blacklistExpireTime: Long,
    @field:JsonProperty("isLimitGroupRtc")
    val isLimitGroupRtc: Long,
    @field:JsonProperty("companyId")
    val companyId: Long,
    @field:JsonProperty("hasGroupCustomPortrait")
    val hasGroupCustomPortrait: Long,
    @field:JsonProperty("bindGuildId")
    val bindGuildId: String,
    @field:JsonProperty("groupOwnerId")
    val groupOwnerId: GetGroupInfoExDataExtInfoGroupOwnerId,
    @field:JsonProperty("essentialMsgPrivilege")
    val essentialMsgPrivilege: Long,
    @field:JsonProperty("msgEventSeq")
    val msgEventSeq: String,
    @field:JsonProperty("inviteRobotSwitch")
    val inviteRobotSwitch: Long,
    @field:JsonProperty("gangUpId")
    val gangUpId: String,
    @field:JsonProperty("qqMusicMedalSwitch")
    val qqMusicMedalSwitch: Long,
    @field:JsonProperty("showPlayTogetherSwitch")
    val showPlayTogetherSwitch: Long,
    @field:JsonProperty("groupFlagPro1")
    val groupFlagPro1: String,
    @field:JsonProperty("groupBindGuildIds")
    val groupBindGuildIds: GetGroupInfoExDataExtInfoGroupBindGuildIds,
    @field:JsonProperty("viewedMsgDisappearTime")
    val viewedMsgDisappearTime: String,
    @field:JsonProperty("groupExtFlameData")
    val groupExtFlameData: GetGroupInfoExDataExtInfoGroupExtFlameData,
    @field:JsonProperty("groupBindGuildSwitch")
    val groupBindGuildSwitch: Long,
    @field:JsonProperty("groupAioBindGuildId")
    val groupAioBindGuildId: String,
    @field:JsonProperty("groupExcludeGuildIds")
    val groupExcludeGuildIds: GetGroupInfoExDataExtInfoGroupExcludeGuildIds,
    @field:JsonProperty("fullGroupExpansionSwitch")
    val fullGroupExpansionSwitch: Long,
    @field:JsonProperty("fullGroupExpansionSeq")
    val fullGroupExpansionSeq: String,
    @field:JsonProperty("inviteRobotMemberSwitch")
    val inviteRobotMemberSwitch: Long,
    @field:JsonProperty("inviteRobotMemberExamine")
    val inviteRobotMemberExamine: Long,
    @field:JsonProperty("groupSquareSwitch")
    val groupSquareSwitch: Long
)

/**
 * GetGroupInfoExData
 */
data class GetGroupInfoExData(
    @field:JsonProperty("groupCode")
    val groupCode: String,
    @field:JsonProperty("resultCode")
    val resultCode: Long,
    @field:JsonProperty("extInfo")
    val extInfo: GetGroupInfoExDataExtInfo
)

/**
 * NapCatGroupExtraExpand
 */
class NapCatGroupExtraExpand

/**
 * 删除群相册媒体
 */
suspend fun NapCatBotApi.delGroupAlbumMedia(groupId: String, albumId: String, lloc: String) {
    apiRequestUnit("del_group_album_media", mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc))
}

/**
 * 发表群相册评论
 */
suspend fun NapCatBotApi.doGroupAlbumComment(groupId: String, albumId: String, lloc: String, content: String) {
    apiRequestUnit("do_group_album_comment", mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc, "content" to content))
}

/**
 * 获取群相册媒体列表
 */
suspend fun NapCatBotApi.getGroupAlbumMediaList(groupId: String, albumId: String, attachInfo: String): String {
    return apiRequest("get_group_album_media_list", mapOf("group_id" to groupId, "album_id" to albumId, "attach_info" to attachInfo))
}

/**
 * 获取群信息ex
 */
suspend fun NapCatBotApi.getGroupInfoEx(groupId: Long): GetGroupInfoExData {
    return apiRequest("get_group_info_ex", mapOf("group_id" to groupId))
}

/**
 * 获取群相册列表
 */
suspend fun NapCatBotApi.getQunAlbumList(groupId: String): List<String> {
    return apiRequest("get_qun_album_list", mapOf("group_id" to groupId))
}

/**
 * 群打卡
 */
suspend fun NapCatBotApi.sendGroupSign(groupId: String) {
    apiRequestUnit("send_group_sign", mapOf("group_id" to groupId))
}

/**
 * 设置群添加选项
 */
suspend fun NapCatBotApi.setGroupAddOption(groupId: Long, addType: String, groupQuestion: String? = null, groupAnswer: String? = null) {
    apiRequestUnit("set_group_add_option", mapOf("group_id" to groupId, "add_type" to addType, "group_question" to groupQuestion, "group_answer" to groupAnswer))
}

/**
 * 点赞群相册媒体
 */
suspend fun NapCatBotApi.setGroupAlbumMediaLike(groupId: String, albumId: String, lloc: String, id: String, set: Boolean) {
    apiRequestUnit("set_group_album_media_like", mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc, "id" to id, "set" to set))
}

/**
 * 设置群备注
 */
suspend fun NapCatBotApi.setGroupRemark(groupId: String, remark: String) {
    apiRequestUnit("set_group_remark", mapOf("group_id" to groupId, "remark" to remark))
}

/**
 * 设置群机器人添加选项
 */
suspend fun NapCatBotApi.setGroupRobotAddOption(groupId: Long, robotMemberSwitch: Long? = null, robotMemberExamine: Long? = null) {
    apiRequestUnit("set_group_robot_add_option", mapOf("group_id" to groupId, "robot_member_switch" to robotMemberSwitch, "robot_member_examine" to robotMemberExamine))
}

/**
 * 设置群搜索
 */
suspend fun NapCatBotApi.setGroupSearch(groupId: Long, noCodeFingerOpen: Long? = null, noFingerOpen: Long? = null) {
    apiRequestUnit("set_group_search", mapOf("group_id" to groupId, "no_code_finger_open" to noCodeFingerOpen, "no_finger_open" to noFingerOpen))
}

/**
 * 群打卡
 */
suspend fun NapCatBotApi.setGroupSign(groupId: String) {
    apiRequestUnit("set_group_sign", mapOf("group_id" to groupId))
}

/**
 * 上传图片到群相册
 */
suspend fun NapCatBotApi.uploadImageToQunAlbum(groupId: String, albumId: String, albumName: String, file: String) {
    apiRequestUnit("upload_image_to_qun_album", mapOf("group_id" to groupId, "album_id" to albumId, "album_name" to albumName, "file" to file))
}
