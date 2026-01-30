@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.json.Json
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import kotlin.time.Duration

/**
 * NapCatGroupApiExpand 群组API
 */
class NapCatGroupApiExpand {

    /**
     * 群成员信息
     */
    data class GroupMember(
        /**
         * 群ID
         */
        @field:JsonProperty("group_id")
        val groupId: String,

        /**
         * 用户ID
         */
        @field:JsonProperty("user_id")
        val userId: String,

        /**
         * 昵称
         */
        @field:JsonProperty("nickname")
        val nickname: String,

        /**
         * 群成员角色
         */
        @field:JsonProperty("role")
        val role: String,

        /**
         * 群头衔
         */
        @field:JsonProperty("title")
        val title: String? = null,

        /**
         * 群名片
         */
        @field:JsonProperty("card")
        val card: String? = null,

        /**
         * 性别
         */
        @field:JsonProperty("sex")
        val sex: String? = null,

        /**
         * 年龄
         */
        @field:JsonProperty("age")
        val age: Int? = null,

        /**
         * 地区
         */
        @field:JsonProperty("area")
        val area: String? = null,

        /**
         * 群等级
         */
        @field:JsonProperty("level")
        val level: Int? = null,

        /**
         * QQ 等级
         */
        @field:JsonProperty("qq_level")
        val qqLevel: Int? = null,

        /**
         * 加群时间
         */
        @field:JsonProperty("join_time")
        val joinTime: Long? = null,

        /**
         * 最后发言时间
         */
        @field:JsonProperty("last_sent_time")
        val lastSentTime: Long? = null,

        /**
         * 头衔过期时间
         */
        @field:JsonProperty("title_expire_time")
        val titleExpireTime: Long? = null,

        /**
         * 是否机器人
         */
        @field:JsonProperty("is_robot")
        val isRobot: Boolean,

        /**
         * 禁言到期时间戳
         * 0 表示未禁言
         */
        @field:JsonProperty("shut_up_timestamp")
        val shutUpTimestamp: Long,
    ) {

        fun toUser(): ReceiveMessage.User {
            return ReceiveMessage.User(
                userId,
                if (card.isNullOrBlank()) nickname else card,
                NapCatBotApiSupport.apiAuth(userId, role),
                NapCatBotApiSupport.avatar(userId)
            )
        }
    }

    /**
     * GroupNoticeItemItemMessage
     */
    data class GroupNoticeItemItemMessage(
        /**
         * 文本内容
         */
        @field:JsonProperty("text")
        val text: String,
        /**
         * 图片列表
         */
        @field:JsonProperty("image")
        val image: List<String>,
        /**
         * 图片列表
         */
        @field:JsonProperty("images")
        val images: List<String>
    )

    /**
     * GroupNoticeItemItem
     */
    data class GroupNoticeItemItem(
        /**
         * 发送者QQ
         */
        @field:JsonProperty("sender_id")
        val senderId: Long,
        /**
         * 发布时间
         */
        @field:JsonProperty("publish_time")
        val publishTime: Long,
        /**
         * 公告ID
         */
        @field:JsonProperty("notice_id")
        val noticeId: String,
        /**
         * 公告内容
         */
        @field:JsonProperty("message")
        val message: GroupNoticeItemItemMessage,
        /**
         * settings
         */
        @field:JsonProperty("settings")
        val settings: String?,
        /**
         * 阅读数
         */
        @field:JsonProperty("read_num")
        val readNum: Long?
    )

    /**
     * 精华消息
     */
    data class EssenceMsg(
        /**
         * 消息 ID
         */
        @field:JsonProperty("message_id")
        val messageId: String,

        /**
         * 发送人
         */
        @field:JsonProperty("sender_id")
        val senderId: String,

        /**
         * 操作人
         */
        @field:JsonProperty("operator_id")
        val operatorId: String,

        /**
         * 操作时间
         */
        @field:JsonProperty("operator_time")
        val operatorTime: Long,

        /**
         * 消息内容
         */
        @field:JsonProperty("content")
        val content: List<NapCatMessage.Message>
    )


    /**
     * GroupDetailInfo
     */
    data class GroupDetailInfo(
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: Long,
        /**
         * 群名称
         */
        @field:JsonProperty("group_name")
        val groupName: String,
        /**
         * 成员数量
         */
        @field:JsonProperty("member_count")
        val memberCount: Long,
        /**
         * 最大成员数量
         */
        @field:JsonProperty("max_member_count")
        val maxMemberCount: Long,
        /**
         * 全员禁言状态
         */
        @field:JsonProperty("group_all_shut")
        val groupAllShut: Long,
        /**
         * 群备注
         */
        @field:JsonProperty("group_remark")
        val groupRemark: String
    )

    /**
     * GroupIgnoreAddRequestItemItem
     */
    data class GroupIgnoreAddRequestItemItem(
        /**
         * 请求ID
         */
        @field:JsonProperty("request_id")
        val requestId: Long,
        /**
         * 邀请者QQ
         */
        @field:JsonProperty("invitor_uin")
        val invitorUin: Long,
        /**
         * 邀请者昵称
         */
        @field:JsonProperty("invitor_nick")
        val invitorNick: String?,
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: Long,
        /**
         * 验证信息
         */
        @field:JsonProperty("message")
        val message: String?,
        /**
         * 群名称
         */
        @field:JsonProperty("group_name")
        val groupName: String?,
        /**
         * 是否已处理
         */
        @field:JsonProperty("checked")
        val checked: Boolean,
        /**
         * 处理者QQ
         */
        @field:JsonProperty("actor")
        val actor: Long,
        /**
         * 请求者昵称
         */
        @field:JsonProperty("requester_nick")
        val requesterNick: String?
    )

    /**
     * GroupIgnoredNotifies
     */
    data class GroupIgnoredNotifies(
        /**
         * 邀请请求列表
         */
        @field:JsonProperty("invited_requests")
        val invitedRequests: List<String>,
        /**
         * 加入请求列表
         */
        @field:JsonProperty("join_requests")
        val joinRequests: List<String>
    )

    /**
     * GroupInfo
     */
    data class GroupInfo(
        /**
         * 是否全员禁言
         */
        @field:JsonProperty("group_all_shut")
        val groupAllShut: Long,
        /**
         * 群备注
         */
        @field:JsonProperty("group_remark")
        val groupRemark: String,
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: String,
        /**
         * 群名称
         */
        @field:JsonProperty("group_name")
        val groupName: String,
        /**
         * 成员人数
         */
        @field:JsonProperty("member_count")
        val memberCount: Long?,
        /**
         * 最大成员人数
         */
        @field:JsonProperty("max_member_count")
        val maxMemberCount: Long?
    )

    /**
     * 群禁言信息
     */
    data class GroupShut(
        /**
         * 用户 ID
         */
        @field:JsonProperty("user_id")
        val userId: Long,
        /**
         * 用户昵称
         */
        @field:JsonProperty("nickname")
        val nickname: String,
        /**
         * 禁言截止时间
         */
        @field:JsonProperty("shut_up_time")
        val shutUpTime: Long
    )

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
 * 删除群公告
 *
 * 删除群聊中的公告
 *
 * @param groupId 群组ID
 * @param noticeId 公告ID
 */
suspend fun NapCatBotApi.delGroupNotice(groupId: String = this.receiveMessage.source.id, noticeId: String) {
    apiRequestUnit("_del_group_notice", mapOf("group_id" to groupId, "notice_id" to noticeId))
}

/**
 * 获取群公告
 *
 * 获取指定群聊中的公告列表
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.getGroupNotice(groupId: String = this.receiveMessage.source.id): List<NapCatGroupApiExpand.GroupNoticeItemItem> {
    return apiRequest("_get_group_notice", mapOf("group_id" to groupId))
}

/**
 * 移出精华消息
 *
 * 将一条消息从群精华消息列表中移出
 *
 * @param messageId 消息ID
 * @param msgSeq 消息序列号
 * @param msgRandom 随机值
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.deleteEssenceMsg(
    messageId: Long? = null,
    msgSeq: String? = null,
    msgRandom: String? = null,
    groupId: String? = null
) {
    apiRequestUnit(
        "delete_essence_msg",
        mapOf("message_id" to messageId, "msg_seq" to msgSeq, "msg_random" to msgRandom, "group_id" to groupId)
    )
}

/**
 * 获取群精华消息
 *
 * 获取指定群聊中的精华消息列表
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.getEssenceMsgList(groupId: String = this.receiveMessage.source.id): List<NapCatGroupApiExpand.EssenceMsg> {
    return apiRequest("get_essence_msg_list", mapOf("group_id" to groupId))
}

/**
 * 获取群详细信息
 *
 * 获取群聊的详细信息，包括成员数、最大成员数等
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.getGroupDetailInfo(groupId: String = this.receiveMessage.source.id): NapCatGroupApiExpand.GroupDetailInfo {
    return apiRequest("get_group_detail_info", mapOf("group_id" to groupId))
}

/**
 * 获取群被忽略的加群请求
 */
suspend fun NapCatBotApi.getGroupIgnoreAddRequest(): List<NapCatGroupApiExpand.GroupIgnoreAddRequestItemItem> {
    return apiRequest("get_group_ignore_add_request")
}

/**
 * 获取群忽略通知
 *
 * 获取被忽略的入群申请和邀请通知
 */
suspend fun NapCatBotApi.getGroupIgnoredNotifies(): NapCatGroupApiExpand.GroupIgnoredNotifies {
    return apiRequest("get_group_ignored_notifies")
}

/**
 * 获取群信息
 *
 * 获取群聊的基本信息
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.getGroupInfo(groupId: String = this.receiveMessage.source.id): NapCatGroupApiExpand.GroupInfo {
    return apiRequest("get_group_info", mapOf("group_id" to groupId))
}

/**
 * 获取群列表
 *
 * 获取当前账号的群聊列表
 *
 * @param noCache 是否禁用缓存
 */
suspend fun NapCatBotApi.getGroupList(noCache: Boolean = true): List<NapCatGroupApiExpand.GroupInfo> {
    return apiRequest("get_group_list", mapOf("no_cache" to noCache))
}

/**
 * 获取群成员信息
 *
 * 获取群聊中指定成员的信息
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param noCache 是否禁用缓存
 */
suspend fun NapCatBotApi.getGroupMemberInfo(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    noCache: Boolean = true
): NapCatGroupApiExpand.GroupMember {
    return apiRequest("get_group_member_info", mapOf("group_id" to groupId, "user_id" to userId, "no_cache" to noCache))
}

/**
 * 获取群成员列表
 *
 * 获取群聊中的所有成员列表
 *
 * @param groupId 群组ID
 * @param noCache 是否禁用缓存
 */
suspend fun NapCatBotApi.getGroupMemberList(
    groupId: String = this.receiveMessage.source.id,
    noCache: Boolean = true
): List<NapCatGroupApiExpand.GroupMember> {
    return apiRequest("get_group_member_list", mapOf("group_id" to groupId, "no_cache" to noCache))
}


/**
 * 设置群待办
 *
 * 将指定消息设置为群待办
 *
 * @param groupId 群组ID
 * @param messageId 消息ID
 * @param messageSeq 消息序列号
 */
suspend fun NapCatBotApi.setGroupTodo(
    groupId: String = this.receiveMessage.source.id,
    messageId: String? = null,
    messageSeq: String? = null
) {
    apiRequestUnit(
        "set_group_todo",
        mapOf("group_id" to groupId, "message_id" to messageId, "message_seq" to messageSeq)
    )
}

/**
 * 获取群禁言列表
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.getGroupShutList(groupId: String = this.receiveMessage.source.id): List<NapCatGroupApiExpand.GroupShut> {
    return apiRequest("get_group_shut_list", mapOf("group_id" to groupId))
}

/**
 * 设置精华消息
 *
 * 将一条消息设置为群精华消息
 *
 * @param messageId 消息ID
 */
suspend fun NapCatBotApi.setEssenceMsg(messageId: String) {
    apiRequestUnit("set_essence_msg", mapOf("message_id" to messageId))
}

/**
 * 处理加群请求
 *
 * 同意或拒绝加群请求或邀请
 *
 * @param flag 请求标识
 * @param approve 是否同意
 * @param reason 拒绝理由
 * @param count 处理数量
 */
suspend fun NapCatBotApi.setGroupAddRequest(
    flag: String,
    approve: Boolean,
    reason: String? = null,
    count: Long = 100
) {
    apiRequestUnit(
        "set_group_add_request",
        mapOf("flag" to flag, "approve" to approve, "reason" to reason, "count" to count)
    )
}

/**
 * 设置群管理员
 *
 * 设置或取消群聊中的管理员
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param enable 是否启用
 */
suspend fun NapCatBotApi.setGroupAdmin(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    enable: Boolean
) {
    apiRequestUnit("set_group_admin", mapOf("group_id" to groupId, "user_id" to userId, "enable" to enable))
}

/**
 * 群组禁言
 *
 * 禁言群聊中的指定成员
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param duration 禁言时长
 */
suspend fun NapCatBotApi.setGroupBan(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    duration: Duration
) {
    apiRequestUnit(
        "set_group_ban",
        mapOf("group_id" to groupId, "user_id" to userId, "duration" to duration.inWholeSeconds)
    )
}

/**
 * 设置群名片
 *
 * 设置群聊中指定成员的群名片
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param card 群名片
 */
suspend fun NapCatBotApi.setGroupCard(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    card: String
) {
    apiRequestUnit("set_group_card", mapOf("group_id" to groupId, "user_id" to userId, "card" to card))
}

/**
 * 群组踢人
 *
 * 将指定成员踢出群聊
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param rejectAddRequest 是否拒绝再次申请
 */
suspend fun NapCatBotApi.setGroupKick(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    rejectAddRequest: Boolean? = null
) {
    apiRequestUnit(
        "set_group_kick",
        mapOf("group_id" to groupId, "user_id" to userId, "reject_add_request" to rejectAddRequest)
    )
}

/**
 * 退出群组
 *
 * 退出或解散指定群聊
 *
 * @param groupId 群组ID
 * @param isDismiss 是否解散
 */
suspend fun NapCatBotApi.setGroupLeave(groupId: String = this.receiveMessage.source.id, isDismiss: Boolean = false) {
    apiRequestUnit("set_group_leave", mapOf("group_id" to groupId, "is_dismiss" to isDismiss))
}

/**
 * 设置群名称
 *
 * 修改指定群聊的名称
 *
 * @param groupId 群组ID
 * @param groupName 群名称
 */
suspend fun NapCatBotApi.setGroupName(groupId: String = this.receiveMessage.source.id, groupName: String) {
    apiRequestUnit("set_group_name", mapOf("group_id" to groupId, "group_name" to groupName))
}

/**
 * 全员禁言
 *
 * 开启或关闭指定群聊的全员禁言
 *
 * @param groupId 群组ID
 * @param enable 是否启用
 */
suspend fun NapCatBotApi.setGroupWholeBan(groupId: String = this.receiveMessage.source.id, enable: Boolean? = null) {
    apiRequestUnit("set_group_whole_ban", mapOf("group_id" to groupId, "enable" to enable))
}

/**
 * 删除群相册媒体
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param lloc 媒体位置标识
 */
suspend fun NapCatBotApi.delGroupAlbumMedia(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    lloc: String
) {
    apiRequestUnit("del_group_album_media", mapOf("group_id" to groupId, "album_id" to albumId, "lloc" to lloc))
}

/**
 * 发表群相册评论
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param lloc 媒体位置标识
 * @param content 评论内容
 */
suspend fun NapCatBotApi.doGroupAlbumComment(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    lloc: String,
    content: String
) {
    apiRequestUnit(
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
suspend fun NapCatBotApi.getGroupAlbumMediaList(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    attachInfo: String
): List<NapCatGroupApiExpand.AlbumMedia> {
    val data = apiRequest<JsonNode>(
        "get_group_album_media_list",
        mapOf("group_id" to groupId, "album_id" to albumId, "attach_info" to attachInfo)
    )
    return Json.convertValue(data.path("media_list"))
}

/**
 * 获取群详细信息 (扩展)
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.getGroupInfoEx(groupId: String = this.receiveMessage.source.id): JsonNode {
    return apiRequest("get_group_info_ex", mapOf("group_id" to groupId))
}

/**
 * 获取群相册列表
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.getQunAlbumList(groupId: String = this.receiveMessage.source.id): List<NapCatGroupApiExpand.Album> {
    return apiRequest("get_qun_album_list", mapOf("group_id" to groupId))
}

/**
 * 群打卡
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.sendGroupSign(groupId: String = this.receiveMessage.source.id) {
    apiRequestUnit("send_group_sign", mapOf("group_id" to groupId))
}

/**
 * 设置群加群选项
 *
 * @param groupId 群组ID
 * @param addType 加群方式
 * @param groupQuestion 验证问题
 * @param groupAnswer 验证答案
 */
suspend fun NapCatBotApi.setGroupAddOption(
    groupId: String = this.receiveMessage.source.id,
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
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param lloc 媒体位置标识
 * @param id 媒体ID
 * @param set 是否点赞
 */
suspend fun NapCatBotApi.setGroupAlbumMediaLike(
    groupId: String = this.receiveMessage.source.id,
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
 *
 * @param groupId 群组ID
 * @param remark 群备注
 */
suspend fun NapCatBotApi.setGroupRemark(groupId: String = this.receiveMessage.source.id, remark: String) {
    apiRequestUnit("set_group_remark", mapOf("group_id" to groupId, "remark" to remark))
}

/**
 * 设置群机器人加群选项
 *
 * @param groupId 群组ID
 * @param robotMemberSwitch 机器人加群开关
 * @param robotMemberExamine 机器人加群审核
 */
suspend fun NapCatBotApi.setGroupRobotAddOption(
    groupId: String = this.receiveMessage.source.id,
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
 *
 * @param groupId 群组ID
 * @param noCodeFingerOpen 是否开启群号检索
 * @param noFingerOpen 是否开启群名检索
 */
suspend fun NapCatBotApi.setGroupSearch(
    groupId: String = this.receiveMessage.source.id,
    noCodeFingerOpen: Long? = null,
    noFingerOpen: Long? = null
) {
    apiRequestUnit(
        "set_group_search",
        mapOf("group_id" to groupId, "no_code_finger_open" to noCodeFingerOpen, "no_finger_open" to noFingerOpen)
    )
}

/**
 * 群打卡
 *
 * @param groupId 群组ID
 */
suspend fun NapCatBotApi.setGroupSign(groupId: String = this.receiveMessage.source.id) {
    apiRequestUnit("set_group_sign", mapOf("group_id" to groupId))
}

/**
 * 上传图片到群相册
 *
 * @param groupId 群组ID
 * @param albumId 相册ID
 * @param albumName 相册名称
 * @param file 图片文件
 */
suspend fun NapCatBotApi.uploadImageToQunAlbum(
    groupId: String = this.receiveMessage.source.id,
    albumId: String,
    albumName: String,
    file: String
) {
    apiRequestUnit(
        "upload_image_to_qun_album",
        mapOf("group_id" to groupId, "album_id" to albumId, "album_name" to albumName, "file" to file)
    )
}

/**
 * 批量踢出群成员
 *
 * 从指定群聊中批量踢出多个成员
 *
 * @param groupId 群组ID
 * @param userId 用户ID列表
 * @param rejectAddRequest 是否拒绝再次申请
 */
suspend fun NapCatBotApi.setGroupKickMembers(
    groupId: String = this.receiveMessage.source.id,
    userId: List<String>,
    rejectAddRequest: Boolean = false
) {
    apiRequestUnit(
        "set_group_kick_members",
        mapOf("group_id" to groupId, "user_id" to userId, "reject_add_request" to rejectAddRequest)
    )
}

/**
 * 设置专属头衔
 *
 * 设置群聊中指定成员的专属头衔
 *
 * @param groupId 群组ID
 * @param userId 用户ID
 * @param specialTitle 专属头衔
 */
suspend fun NapCatBotApi.setGroupSpecialTitle(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    specialTitle: String
) {
    apiRequestUnit(
        "set_group_special_title",
        mapOf("group_id" to groupId, "user_id" to userId, "special_title" to specialTitle)
    )
}
