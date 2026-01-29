@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode

/**
 * NapCatGroupExtraExpand
 */
class NapCatGroupExtraExpand {
}

/**
 * 删除群相册媒体
 * @param groupId 群号
 * @param albumId 相册ID
 * @param lloc 媒体ID (lloc)
 */
suspend fun NapCatBotApi.delGroupAlbumMedia(groupId: String, albumId: String, lloc: String) {
    apiRequestUnit("del_group_album_media", mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc))
}

/**
 * 发表群相册评论
 * @param groupId 群号
 * @param albumId 相册 ID
 * @param lloc 图片 ID
 * @param content 评论内容
 */
suspend fun NapCatBotApi.doGroupAlbumComment(groupId: String, albumId: String, lloc: String, content: String) {
    apiRequestUnit("do_group_album_comment", mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc, "content" to content))
}

/**
 * 获取群相册媒体列表
 * @param groupId 群号
 * @param albumId 相册ID
 * @param attachInfo 附加信息（用于分页）
 */
suspend fun NapCatBotApi.getGroupAlbumMediaList(groupId: String, albumId: String, attachInfo: String): NapCatGroupExtraExpand.String {
    return apiRequest("get_group_album_media_list", mapOf("group_id" to groupId, "album_id" to albumId, "attach_info" to attachInfo))
}

/**
 * 获取群详细信息 (扩展)
 * @param groupId 群号
 */
suspend fun NapCatBotApi.getGroupInfoEx(groupId: String): NapCatGroupExtraExpand.String {
    return apiRequest("get_group_info_ex", mapOf("group_id" to groupId))
}

/**
 * 获取群相册列表
 * @param groupId 群号
 */
suspend fun NapCatBotApi.getQunAlbumList(groupId: String): NapCatGroupExtraExpand.List<String> {
    return apiRequest("get_qun_album_list", mapOf("group_id" to groupId))
}

/**
 * 群打卡
 * @param groupId 群号
 */
suspend fun NapCatBotApi.sendGroupSign(groupId: String) {
    apiRequestUnit("send_group_sign", mapOf("group_id" to groupId))
}

/**
 * 设置群加群选项
 * @param groupId 群号
 * @param addType 加群方式
 * @param groupQuestion 加群问题
 * @param groupAnswer 加群答案
 */
suspend fun NapCatBotApi.setGroupAddOption(groupId: String, addType: Long, groupQuestion: String? = null, groupAnswer: String? = null) {
    apiRequestUnit("set_group_add_option", mapOf("group_id" to groupId, "add_type" to addType, "group_question" to groupQuestion, "group_answer" to groupAnswer))
}

/**
 * 点赞群相册媒体
 * @param groupId 群号
 * @param albumId 相册ID
 * @param lloc 媒体ID (lloc)
 * @param id 点赞ID
 * @param set 是否点赞
 */
suspend fun NapCatBotApi.setGroupAlbumMediaLike(groupId: String, albumId: String, lloc: String, id: String, set: Boolean) {
    apiRequestUnit("set_group_album_media_like", mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc, "id" to id, "set" to set))
}

/**
 * 设置群备注
 *
 * 设置群备注
 * @param groupId 群号
 * @param remark 备注
 */
suspend fun NapCatBotApi.setGroupRemark(groupId: String, remark: String) {
    apiRequestUnit("set_group_remark", mapOf("group_id" to groupId, "remark" to remark))
}

/**
 * 设置群机器人加群选项
 * @param groupId 群号
 * @param robotMemberSwitch 机器人成员开关
 * @param robotMemberExamine 机器人成员审核
 */
suspend fun NapCatBotApi.setGroupRobotAddOption(groupId: String, robotMemberSwitch: Long? = null, robotMemberExamine: Long? = null) {
    apiRequestUnit("set_group_robot_add_option", mapOf("group_id" to groupId, "robot_member_switch" to robotMemberSwitch, "robot_member_examine" to robotMemberExamine))
}

/**
 * 设置群搜索选项
 * @param groupId 群号
 * @param noCodeFingerOpen 未知
 * @param noFingerOpen 未知
 */
suspend fun NapCatBotApi.setGroupSearch(groupId: String, noCodeFingerOpen: Long? = null, noFingerOpen: Long? = null) {
    apiRequestUnit("set_group_search", mapOf("group_id" to groupId, "no_code_finger_open" to noCodeFingerOpen, "no_finger_open" to noFingerOpen))
}

/**
 * 群打卡
 * @param groupId 群号
 */
suspend fun NapCatBotApi.setGroupSign(groupId: String) {
    apiRequestUnit("set_group_sign", mapOf("group_id" to groupId))
}

/**
 * 上传图片到群相册
 * @param groupId 群号
 * @param albumId 相册ID
 * @param albumName 相册名称
 * @param file 图片路径、URL或Base64
 */
suspend fun NapCatBotApi.uploadImageToQunAlbum(groupId: String, albumId: String, albumName: String, file: String) {
    apiRequestUnit("upload_image_to_qun_album", mapOf("group_id" to groupId, "album_id" to albumId, "album_name" to albumName, "file" to file))
}
