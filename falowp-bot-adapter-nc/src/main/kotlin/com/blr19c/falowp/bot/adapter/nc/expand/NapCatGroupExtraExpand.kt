@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.system.json.Json
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * NapCatGroupExtraExpand
 */
class NapCatGroupExtraExpand {

    /**
     * 相册信息
     */
    data class Album(
        /**
         * 相册 ID
         */
        @field:JsonProperty("album_id")
        val albumId: String,
        /**
         * 相册名称
         */
        @field:JsonProperty("album_name")
        val albumName: String,
        /**
         * 相册封面 URL
         */
        @field:JsonProperty("cover_url")
        val coverUrl: String,
        /**
         * 创建时间（Unix 时间戳，秒）
         */
        @field:JsonProperty("create_time")
        val createTime: Long
    )

    /**
     * 相册媒体信息
     */
    data class AlbumMedia(
        /**
         * 媒体 ID
         */
        @field:JsonProperty("media_id")
        val mediaId: String,
        /**
         * 媒体访问 URL
         */
        @field:JsonProperty("url")
        val url: String
    )

}

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
    apiRequestUnit(
        "do_group_album_comment",
        mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc, "content" to content)
    )
}

/**
 * 获取群相册媒体列表
 */
suspend fun NapCatBotApi.getGroupAlbumMediaList(
    groupId: String,
    albumId: String,
    attachInfo: String
): List<NapCatGroupExtraExpand.AlbumMedia> {
    val data = apiRequest<JsonNode>(
        "get_group_album_media_list",
        mapOf("group_id" to groupId, "album_id" to albumId, "attach_info" to attachInfo)
    )
    return Json.convertValue(data.path("media_list"))
}

/**
 * 获取群详细信息 (扩展)
 */
suspend fun NapCatBotApi.getGroupInfoEx(groupId: String): JsonNode {
    return apiRequest("get_group_info_ex", mapOf("group_id" to groupId))
}

/**
 * 获取群相册列表
 */
suspend fun NapCatBotApi.getQunAlbumList(groupId: String): List<NapCatGroupExtraExpand.Album> {
    return apiRequest("get_qun_album_list", mapOf("group_id" to groupId))
}

/**
 * 群打卡
 */
suspend fun NapCatBotApi.sendGroupSign(groupId: String) {
    apiRequestUnit("send_group_sign", mapOf("group_id" to groupId))
}

/**
 * 设置群加群选项
 */
suspend fun NapCatBotApi.setGroupAddOption(
    groupId: String,
    addType: Long,
    groupQuestion: String? = null,
    groupAnswer: String? = null
) {
    apiRequestUnit(
        "set_group_add_option",
        mapOf(
            "group_id" to groupId,
            "add_type" to addType,
            "group_question" to groupQuestion,
            "group_answer" to groupAnswer
        )
    )
}

/**
 * 点赞群相册媒体
 */
suspend fun NapCatBotApi.setGroupAlbumMediaLike(
    groupId: String,
    albumId: String,
    lloc: String,
    id: String,
    set: Boolean
) {
    apiRequestUnit(
        "set_group_album_media_like",
        mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc, "id" to id, "set" to set)
    )
}

/**
 * 设置群备注
 *
 * 设置群备注
 */
suspend fun NapCatBotApi.setGroupRemark(groupId: String, remark: String) {
    apiRequestUnit("set_group_remark", mapOf("group_id" to groupId, "remark" to remark))
}

/**
 * 设置群机器人加群选项
 */
suspend fun NapCatBotApi.setGroupRobotAddOption(
    groupId: String,
    robotMemberSwitch: Long? = null,
    robotMemberExamine: Long? = null
) {
    apiRequestUnit(
        "set_group_robot_add_option",
        mapOf(
            "group_id" to groupId,
            "robot_member_switch" to robotMemberSwitch,
            "robot_member_examine" to robotMemberExamine
        )
    )
}

/**
 * 设置群搜索选项
 */
suspend fun NapCatBotApi.setGroupSearch(groupId: String, noCodeFingerOpen: Long? = null, noFingerOpen: Long? = null) {
    apiRequestUnit(
        "set_group_search",
        mapOf("group_id" to groupId, "no_code_finger_open" to noCodeFingerOpen, "no_finger_open" to noFingerOpen)
    )
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
    apiRequestUnit(
        "upload_image_to_qun_album",
        mapOf("group_id" to groupId, "album_id" to albumId, "album_name" to albumName, "file" to file)
    )
}
