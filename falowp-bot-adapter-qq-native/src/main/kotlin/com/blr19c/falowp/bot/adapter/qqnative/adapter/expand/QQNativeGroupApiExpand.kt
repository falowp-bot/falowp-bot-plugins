package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand

import com.blr19c.falowp.bot.adapter.qqnative.adapter.QQNativeBotApi
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativeGroupApiExpand
import tools.jackson.databind.JsonNode
import kotlin.time.Duration

/**
 * 删除群公告
 *
 * @param groupId 群组ID
 * @param noticeId 公告ID
 */
suspend fun QQNativeBotApi.delGroupNotice(groupId: String = this.receiveMessage.source.id, noticeId: String) {
    expandUnit("_del_group_notice", mapOf("group_id" to groupId, "notice_id" to noticeId))
}

/**
 * 获取群公告
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.getGroupNotice(groupId: String = this.receiveMessage.source.id): List<QQNativeGroupApiExpand.GroupNoticeItemItem> {
    return expandRequest("_get_group_notice", mapOf("group_id" to groupId))
}

/**
 * 移出精华消息
 *
 * @param messageId 消息ID
 * @param msgSeq 消息序列号
 * @param msgRandom 随机值
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.deleteEssenceMsg(
    messageId: String? = null,
    msgSeq: String? = null,
    msgRandom: String? = null,
    groupId: String = receiveMessage.source.id
) {
    expandUnit(
        "delete_essence_msg",
        mapOf("message_id" to messageId, "msg_seq" to msgSeq, "msg_random" to msgRandom, "group_id" to groupId)
    )
}

/**
 * 获取群精华消息
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.getEssenceMsgList(groupId: String = this.receiveMessage.source.id): List<QQNativeGroupApiExpand.EssenceMsg> {
    return expandRequest("get_essence_msg_list", mapOf("group_id" to groupId))
}

/**
 * 获取群详细信息
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.getGroupDetailInfo(groupId: String = this.receiveMessage.source.id): QQNativeGroupApiExpand.GroupDetailInfo {
    return expandRequest("get_group_detail_info", mapOf("group_id" to groupId))
}

/**
 * 获取群被忽略的加群请求
 */
suspend fun QQNativeBotApi.getGroupIgnoreAddRequest(): List<QQNativeGroupApiExpand.GroupIgnoreAddRequestItemItem> {
    return expandRequest("get_group_ignore_add_request")
}

/**
 * 获取群忽略通知
 */
suspend fun QQNativeBotApi.getGroupIgnoredNotifies(): QQNativeGroupApiExpand.GroupIgnoredNotifies {
    return expandRequest("get_group_ignored_notifies")
}

/**
 * 获取群信息
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.getGroupInfo(groupId: String = this.receiveMessage.source.id): QQNativeGroupApiExpand.GroupInfo {
    return expandRequest("get_group_info", mapOf("group_id" to groupId))
}

/**
 * 获取群列表
 *
 * @param noCache 是否禁用缓存
 */
suspend fun QQNativeBotApi.getGroupList(noCache: Boolean = true): List<QQNativeGroupApiExpand.GroupInfo> {
    return expandRequest("get_group_list", mapOf("no_cache" to noCache))
}

/**
 * 获取群成员信息
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param noCache 是否禁用缓存
 */
suspend fun QQNativeBotApi.getGroupMemberInfo(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    noCache: Boolean = true
): QQNativeGroupApiExpand.GroupMember {
    return expandRequest(
        "get_group_member_info",
        mapOf("group_id" to groupId, "user_id" to userId, "no_cache" to noCache)
    )
}

/**
 * 获取群成员列表
 *
 * @param groupId 群组ID
 * @param noCache 是否禁用缓存
 */
suspend fun QQNativeBotApi.getGroupMemberList(
    groupId: String = this.receiveMessage.source.id,
    noCache: Boolean = true
): List<QQNativeGroupApiExpand.GroupMember> {
    return expandRequest("get_group_member_list", mapOf("group_id" to groupId, "no_cache" to noCache))
}

/**
 * 设置群待办
 *
 * @param groupId 群组ID
 * @param messageId 消息ID
 * @param messageSeq 消息序列号
 */
suspend fun QQNativeBotApi.setGroupTodo(
    groupId: String = this.receiveMessage.source.id,
    messageId: String? = null,
    messageSeq: String? = null
) {
    expandUnit(
        "set_group_todo",
        mapOf("group_id" to groupId, "message_id" to messageId, "message_seq" to messageSeq)
    )
}

/**
 * 获取群禁言列表
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.getGroupShutList(groupId: String = this.receiveMessage.source.id): List<QQNativeGroupApiExpand.GroupShut> {
    return expandRequest("get_group_shut_list", mapOf("group_id" to groupId))
}

/**
 * 设置精华消息
 *
 * 将一条消息设置为群精华消息
 *
 * @param messageId 消息ID
 */
suspend fun QQNativeBotApi.setEssenceMsg(messageId: String, groupId: String = receiveMessage.source.id) {
    expandUnit("set_essence_msg", mapOf("message_id" to messageId, "group_id" to groupId))
}

/**
 * 处理加群请求
 *
 * @param flag 请求标识
 * @param approve 是否同意
 * @param reason 拒绝理由
 * @param count 处理数量
 */
suspend fun QQNativeBotApi.setGroupAddRequest(
    flag: String,
    approve: Boolean,
    reason: String? = null,
    count: Long = 100
) {
    expandUnit(
        "set_group_add_request",
        mapOf("flag" to flag, "approve" to approve, "reason" to reason, "count" to count)
    )
}

/**
 * 设置群管理员
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param enable 是否启用
 */
suspend fun QQNativeBotApi.setGroupAdmin(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    enable: Boolean
) {
    expandUnit("set_group_admin", mapOf("group_id" to groupId, "user_id" to userId, "enable" to enable))
}

/**
 * 群组禁言
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param duration 禁言时长
 */
suspend fun QQNativeBotApi.setGroupBan(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    duration: Duration
) {
    expandUnit(
        "set_group_ban",
        mapOf("group_id" to groupId, "user_id" to userId, "duration" to duration.inWholeSeconds)
    )
}

/**
 * 设置群名片
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param card 群名片
 */
suspend fun QQNativeBotApi.setGroupCard(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    card: String
) {
    expandUnit("set_group_card", mapOf("group_id" to groupId, "user_id" to userId, "card" to card))
}

/**
 * 群组踢人
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param rejectAddRequest 是否拒绝再次申请
 */
suspend fun QQNativeBotApi.setGroupKick(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    rejectAddRequest: Boolean? = null
) {
    expandUnit(
        "set_group_kick",
        mapOf("group_id" to groupId, "user_id" to userId, "reject_add_request" to rejectAddRequest)
    )
}

/**
 * 退出群组
 *
 * @param groupId 群组ID
 * @param isDismiss 是否解散
 */
suspend fun QQNativeBotApi.setGroupLeave(groupId: String = this.receiveMessage.source.id, isDismiss: Boolean = false) {
    expandUnit("set_group_leave", mapOf("group_id" to groupId, "is_dismiss" to isDismiss))
}

/**
 * 设置群名称
 *
 * @param groupId 群组ID
 * @param groupName 群名称
 */
suspend fun QQNativeBotApi.setGroupName(groupId: String = this.receiveMessage.source.id, groupName: String) {
    expandUnit("set_group_name", mapOf("group_id" to groupId, "group_name" to groupName))
}

/**
 * 全员禁言
 *
 * @param groupId 群组ID
 * @param enable 是否启用
 */
suspend fun QQNativeBotApi.setGroupWholeBan(groupId: String = this.receiveMessage.source.id, enable: Boolean? = null) {
    expandUnit("set_group_whole_ban", mapOf("group_id" to groupId, "enable" to enable))
}

/**
 * 删除群相册媒体
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param lloc 媒体位置标识
 */
suspend fun QQNativeBotApi.delGroupAlbumMedia(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    lloc: String
) {
    expandUnit("del_group_album_media", mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc))
}

/**
 * 发表群相册评论
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param lloc 媒体位置标识
 * @param content 评论内容
 */
suspend fun QQNativeBotApi.doGroupAlbumComment(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    lloc: String,
    content: String
) {
    expandUnit(
        "do_group_album_comment",
        mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc, "content" to content)
    )
}

/**
 * 获取群相册媒体列表
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param attachInfo 附加信息
 */
suspend fun QQNativeBotApi.getGroupAlbumMediaList(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    attachInfo: String = ""
): List<QQNativeGroupApiExpand.AlbumMedia> {
    val entries = mutableListOf<QQNativeGroupApiExpand.AlbumMedia>()
    val seen = mutableSetOf<String>()
    var cursor = attachInfo
    do {
        check(seen.add(cursor)) { "群相册媒体分页游标重复" }
        val page = getGroupAlbumMediaPage(groupId, albumId, cursor)
        for (row in page.path("media_list")) {
            entries.add(
                QQNativeGroupApiExpand.AlbumMedia(
                    row.path("image").get("lloc")?.asString(),
                    row.path("image").path("default_url").get("url")?.asString(), row
                )
            )
        }
        if (!page.path("next_has_more").asBoolean(false)) break
        cursor = page.path("next_attach_info").asString("")
        check(cursor.isNotBlank()) { "QQ 未返回群相册媒体下一页游标" }
    } while (true)
    return entries
}

/**
 * 单页原生响应，保留分页游标和图片/视频完整字段
 */
suspend fun QQNativeBotApi.getGroupAlbumMediaPage(
    groupId: String = receiveMessage.source.id,
    albumId: String,
    attachInfo: String = ""
): JsonNode = expandRequest(
    "get_group_album_media_list",
    mapOf("group_id" to groupId, "album_id" to albumId, "attach_info" to attachInfo)
)

/**
 * 获取群详细信息 (扩展)
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.getGroupInfoEx(groupId: String = this.receiveMessage.source.id): JsonNode {
    return expandRequest("get_group_info_ex", mapOf("group_id" to groupId))
}

/**
 * 获取群相册列表
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.getQunAlbumList(groupId: String = this.receiveMessage.source.id): List<QQNativeGroupApiExpand.Album> {
    return expandRequest("get_qun_album_list", mapOf("group_id" to groupId))
}

/**
 * 群打卡
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.sendGroupSign(groupId: String = this.receiveMessage.source.id) {
    expandUnit("send_group_sign", mapOf("group_id" to groupId))
}

/**
 * 设置群加群选项
 *
 * @param groupId 群组ID
 * @param addType 加群方式
 * @param groupQuestion 验证问题
 * @param groupAnswer 验证答案
 */
suspend fun QQNativeBotApi.setGroupAddOption(
    groupId: String = this.receiveMessage.source.id,
    addType: Long,
    groupQuestion: String? = null,
    groupAnswer: String? = null
) {
    expandUnit(
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
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param lloc 媒体位置标识
 * @param id 媒体ID
 * @param set 是否点赞
 */
suspend fun QQNativeBotApi.setGroupAlbumMediaLike(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    lloc: String,
    id: String,
    set: Boolean
) {
    expandUnit(
        "set_group_album_media_like",
        mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc, "id" to id, "set" to set)
    )
}

/**
 * 设置群备注
 *
 * 设置群备注
 *
 * @param groupId 群组ID
 * @param remark 群备注
 */
suspend fun QQNativeBotApi.setGroupRemark(groupId: String = this.receiveMessage.source.id, remark: String) {
    expandUnit("set_group_remark", mapOf("group_id" to groupId, "remark" to remark))
}

/**
 * 设置群机器人加群选项
 *
 * @param groupId 群组ID
 * @param robotMemberSwitch 机器人加群开关
 * @param robotMemberExamine 机器人加群审核
 */
suspend fun QQNativeBotApi.setGroupRobotAddOption(
    groupId: String = this.receiveMessage.source.id,
    robotMemberSwitch: Long? = null,
    robotMemberExamine: Long? = null
) {
    expandUnit(
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
 *
 * @param groupId 群组ID
 * @param noCodeFingerOpen 是否开启群号检索
 * @param noFingerOpen 是否开启群名检索
 */
suspend fun QQNativeBotApi.setGroupSearch(
    groupId: String = this.receiveMessage.source.id,
    noCodeFingerOpen: Long? = null,
    noFingerOpen: Long? = null
) {
    expandUnit(
        "set_group_search",
        mapOf("group_id" to groupId, "no_code_finger_open" to noCodeFingerOpen, "no_finger_open" to noFingerOpen)
    )
}

/**
 * 群打卡
 *
 * @param groupId 群组ID
 */
suspend fun QQNativeBotApi.setGroupSign(groupId: String = this.receiveMessage.source.id) {
    expandUnit("set_group_sign", mapOf("group_id" to groupId))
}

/**
 * 上传图片到群相册
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param albumName 相册名称
 * @param file 图片文件
 */
suspend fun QQNativeBotApi.uploadImageToQunAlbum(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    albumName: String,
    file: String
) {
    expandUnit(
        "upload_image_to_qun_album",
        mapOf("group_id" to groupId, "album_id" to albumId, "album_name" to albumName, "file" to file)
    )
}

/**
 * 批量踢出群成员
 *
 * @param groupId 群组ID
 * @param userId 用户ID列表
 * @param rejectAddRequest 是否拒绝再次申请
 */
suspend fun QQNativeBotApi.setGroupKickMembers(
    groupId: String = this.receiveMessage.source.id,
    userId: List<String>,
    rejectAddRequest: Boolean = false
) {
    expandUnit(
        "set_group_kick_members",
        mapOf("group_id" to groupId, "user_id" to userId, "reject_add_request" to rejectAddRequest)
    )
}

/**
 * 设置群聊中指定成员的专属头衔
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param specialTitle 专属头衔
 */
suspend fun QQNativeBotApi.setGroupSpecialTitle(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    specialTitle: String
) {
    expandUnit(
        "set_group_special_title",
        mapOf("group_id" to groupId, "user_id" to userId, "special_title" to specialTitle)
    )
}
