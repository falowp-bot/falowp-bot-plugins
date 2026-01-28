package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode

/**
 * 群聊类扩展
 */
class NapCatGroupExpand {

    /**
     * 群信息
     */
    data class GroupInfo(
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: String,

        /**
         * 是否全员禁言
         */
        @field:JsonProperty("group_all_shut")
        val groupAllShut: Int,

        /**
         * 群备注
         */
        @field:JsonProperty("group_remark")
        val groupRemark: String,

        /**
         * 群名称
         */
        @field:JsonProperty("group_name")
        val groupName: String,

        /**
         * 成员人数
         */
        @field:JsonProperty("member_count")
        val memberCount: Int? = null,

        /**
         * 最大成员人数
         */
        @field:JsonProperty("max_member_count")
        val maxMemberCount: Int? = null
    )

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


}

/**
 * 设置群添加选项
 *
 * @param groupId 群组id
 * @param addType 加群方式（例如：0允许任何人 1需要验证 2不允许加群）
 * @param question 验证问题
 * @param answer 验证答案
 */
suspend fun NapCatBotApi.setGroupAddOption(
    groupId: String = this.receiveMessage.source.id,
    addType: Int,
    question: String? = null,
    answer: String? = null
) {
    apiRequestUnit(
        "set_group_add_option",
        mapOf(
            "group_id" to groupId,
            "add_type" to addType,
            "question" to question,
            "answer" to answer
        )
    )
}

/**
 * 设置群机器人添加选项
 *
 * @param groupId 群组id
 * @param enable 是否允许机器人被添加
 */
suspend fun NapCatBotApi.setGroupBotAddOption(
    groupId: String = this.receiveMessage.source.id,
    enable: Boolean
) {
    apiRequestUnit(
        "set_group_bot_add_option",
        mapOf(
            "group_id" to groupId,
            "enable" to enable
        )
    )
}

/**
 * 批量踢出群成员
 *
 * @param groupId 群组id
 * @param userIds 用户id列表
 * @param rejectAddRequest 是否拒绝再次申请
 */
suspend fun NapCatBotApi.batchGroupKick(
    groupId: String = this.receiveMessage.source.id,
    userIds: List<String>,
    rejectAddRequest: Boolean = false
) {
    apiRequestUnit(
        "batch_group_kick",
        mapOf(
            "group_id" to groupId,
            "user_ids" to userIds,
            "reject_add_request" to rejectAddRequest
        )
    )
}

/**
 * 设置群备注
 *
 * @param groupId 群组id
 * @param remark 群备注
 */
suspend fun NapCatBotApi.setGroupRemark(
    groupId: String = this.receiveMessage.source.id,
    remark: String
) {
    apiRequestUnit(
        "set_group_remark",
        mapOf(
            "group_id" to groupId,
            "remark" to remark
        )
    )
}

/**
 * 设置群头衔
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param specialTitle 群头衔
 * @param duration 持续时间（秒，0通常表示永久）
 */
suspend fun NapCatBotApi.setGroupSpecialTitle(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    specialTitle: String,
    duration: Long = 0
) {
    apiRequestUnit(
        "set_group_special_title",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "special_title" to specialTitle,
            "duration" to duration
        )
    )
}

/**
 * 获取群详细信息
 *
 * @param groupId 群组id
 * @return 群详细信息
 */
suspend fun NapCatBotApi.getGroupDetailInfo(
    groupId: String = this.receiveMessage.source.id
): JsonNode? {
    return apiRequest(
        "get_group_detail_info",
        mapOf("group_id" to groupId)
    )
}

/**
 * 群踢人
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param rejectAddRequest 是否拒绝再次申请
 */
suspend fun NapCatBotApi.setGroupKick(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    rejectAddRequest: Boolean = false
) {
    apiRequestUnit(
        "set_group_kick",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "reject_add_request" to rejectAddRequest
        )
    )
}

/**
 * 获取群系统消息
 *
 * @return 群系统消息
 */
suspend fun NapCatBotApi.getGroupSystemMsg(): JsonNode? {
    return apiRequest("get_group_system_msg")
}

/**
 * 群禁言
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param duration 禁言时长（秒）
 */
suspend fun NapCatBotApi.setGroupBan(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    duration: Long
) {
    apiRequestUnit(
        "set_group_ban",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "duration" to duration
        )
    )
}

/**
 * 获取群精华消息
 *
 * @param groupId 群组id
 * @return 群精华消息列表
 */
suspend fun NapCatBotApi.getEssenceMsgList(
    groupId: String = this.receiveMessage.source.id
): List<NapCatGroupExpand.EssenceMsg> {
    return apiRequest(
        "get_essence_msg_list",
        mapOf("group_id" to groupId)
    )
}

/**
 * 全体禁言
 *
 * @param groupId 群组id
 * @param enable 是否开启全体禁言
 */
suspend fun NapCatBotApi.setGroupWholeBan(
    groupId: String = this.receiveMessage.source.id,
    enable: Boolean
) {
    apiRequestUnit(
        "set_group_whole_ban",
        mapOf(
            "group_id" to groupId,
            "enable" to enable
        )
    )
}

/**
 * 设置群头像
 *
 * @param groupId 群组id
 * @param image 图片base64或url
 */
suspend fun NapCatBotApi.setGroupAvatar(
    groupId: String = this.receiveMessage.source.id,
    image: String
) {
    apiRequestUnit(
        "set_group_portrait",
        mapOf(
            "group_id" to groupId,
            "image" to image
        )
    )
}


/**
 * 设置群管理
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param enable 是否设为管理员
 */
suspend fun NapCatBotApi.setGroupAdmin(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    enable: Boolean
) {
    apiRequestUnit(
        "set_group_admin",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "enable" to enable
        )
    )
}

/**
 * 设置群成员名片
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param card 群名片
 */
suspend fun NapCatBotApi.setGroupCard(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    card: String
) {
    apiRequestUnit(
        "set_group_card",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "card" to card
        )
    )
}

/**
 * 设置群精华消息
 *
 * @param messageId 消息id
 */
suspend fun NapCatBotApi.setEssenceMsg(messageId: Long) {
    apiRequestUnit(
        "set_essence_msg",
        mapOf("message_id" to messageId)
    )
}

/**
 * 设置群名
 *
 * @param groupId 群组id
 * @param groupName 群名
 */
suspend fun NapCatBotApi.setGroupName(
    groupId: String = this.receiveMessage.source.id,
    groupName: String
) {
    apiRequestUnit(
        "set_group_name",
        mapOf(
            "group_id" to groupId,
            "group_name" to groupName
        )
    )
}

/**
 * 删除群精华消息
 *
 * @param messageId 消息id
 */
suspend fun NapCatBotApi.deleteEssenceMsg(messageId: Long) {
    apiRequestUnit(
        "delete_essence_msg",
        mapOf("message_id" to messageId)
    )
}

/**
 * 删除群公告
 *
 * @param groupId 群组id
 * @param noticeId 公告id
 */
suspend fun NapCatBotApi.deleteGroupNotice(
    groupId: String = this.receiveMessage.source.id,
    noticeId: String
) {
    apiRequestUnit(
        "_delete_group_notice",
        mapOf(
            "group_id" to groupId,
            "notice_id" to noticeId
        )
    )
}

/**
 * 退群
 *
 * @param groupId 群组id
 * @param isDismiss 是否解散（仅群主可用）
 */
suspend fun NapCatBotApi.setGroupLeave(
    groupId: String = this.receiveMessage.source.id,
    isDismiss: Boolean = false
) {
    apiRequestUnit(
        "set_group_leave",
        mapOf(
            "group_id" to groupId,
            "is_dismiss" to isDismiss
        )
    )
}

/**
 * 发送群公告
 *
 * @param groupId 群组id
 * @param content 公告内容
 * @param image 图片base64或url
 */
suspend fun NapCatBotApi.sendGroupNotice(
    groupId: String = this.receiveMessage.source.id,
    content: String,
    image: ImageUrl? = null
) {
    apiRequestUnit(
        "_send_group_notice",
        mapOf(
            "group_id" to groupId,
            "content" to content,
            "image" to image?.toBase64()
        )
    )
}

/**
 * 设置群搜索
 *
 * @param groupId 群组id
 * @param enable 是否允许被搜索到
 */
suspend fun NapCatBotApi.setGroupSearch(
    groupId: String = this.receiveMessage.source.id,
    enable: Boolean
) {
    apiRequestUnit(
        "set_group_search",
        mapOf(
            "group_id" to groupId,
            "enable" to enable
        )
    )
}

/**
 * 获取群公告
 *
 * @param groupId 群组id
 * @return 群公告列表/详情
 */
suspend fun NapCatBotApi.getGroupNotice(
    groupId: String = this.receiveMessage.source.id
): JsonNode? {
    return apiRequest(
        "_get_group_notice",
        mapOf("group_id" to groupId)
    )
}

/**
 * 处理加群请求
 *
 * @param flag 请求标识
 * @param subType 请求类型（add/invite等）
 * @param approve 是否同意
 * @param reason 拒绝理由
 */
suspend fun NapCatBotApi.handleGroupAddRequest(
    flag: String,
    subType: String,
    approve: Boolean,
    reason: String? = null
) {
    apiRequestUnit(
        "set_group_add_request",
        mapOf(
            "flag" to flag,
            "sub_type" to subType,
            "approve" to approve,
            "reason" to reason
        )
    )
}

/**
 * 获取群信息
 *
 * @param groupId 群组id
 * @param noCache 是否不使用缓存
 * @return 群信息
 */
suspend fun NapCatBotApi.getGroupInfo(
    groupId: String = this.receiveMessage.source.id,
    noCache: Boolean = false
): JsonNode? {
    return apiRequest(
        "get_group_info",
        mapOf(
            "group_id" to groupId,
            "no_cache" to noCache
        )
    )
}

/**
 * 获取群列表
 *
 * @return 群列表
 */
suspend fun NapCatBotApi.getGroupList(): List<NapCatGroupExpand.GroupInfo> {
    return apiRequest("get_group_list")
}

/**
 * 获取群成员信息
 *
 * @param groupId 群组id
 * @param userId 用户id
 * @param noCache 是否不使用缓存
 * @return 群成员信息
 */
suspend fun NapCatBotApi.getGroupMemberInfo(
    groupId: String = this.receiveMessage.source.id,
    userId: String = this.receiveMessage.sender.id,
    noCache: Boolean = false
): NapCatGroupExpand.GroupMember {
    return apiRequest(
        "get_group_member_info",
        mapOf(
            "group_id" to groupId,
            "user_id" to userId,
            "no_cache" to noCache
        )
    )
}

/**
 * 获取群成员列表
 *
 * @param groupId 群组id
 * @return 群成员列表
 */
suspend fun NapCatBotApi.getGroupMemberList(
    groupId: String = this.receiveMessage.source.id,
    noCache: Boolean = false
): List<NapCatGroupExpand.GroupMember> {
    return apiRequest(
        "get_group_member_list",
        mapOf("group_id" to groupId, "no_cache" to noCache)
    ) ?: emptyList()
}

/**
 * 获取群荣誉
 *
 * @param groupId 群组id
 * @param type 类型（talkative / performer / legend / strong_newbie / emotion 等）
 * @return 群荣誉信息
 */
suspend fun NapCatBotApi.getGroupHonorInfo(
    groupId: String = this.receiveMessage.source.id,
    type: String
): JsonNode? {
    return apiRequest(
        "get_group_honor_info",
        mapOf(
            "group_id" to groupId,
            "type" to type
        )
    )
}

/**
 * 获取群信息ex
 *
 * @param groupId 群组id
 * @return 群信息ex
 */
suspend fun NapCatBotApi.getGroupInfoEx(
    groupId: String = this.receiveMessage.source.id
): JsonNode? {
    return apiRequest(
        "get_group_info_ex",
        mapOf("group_id" to groupId)
    )
}

/**
 * 获取群 @全体成员 剩余次数
 *
 * @param groupId 群组id
 * @return 剩余次数信息
 */
suspend fun NapCatBotApi.getGroupAtAllRemain(
    groupId: String = this.receiveMessage.source.id
): JsonNode? {
    return apiRequest(
        "get_group_at_all_remain",
        mapOf("group_id" to groupId)
    )
}

/**
 * 获取群禁言列表
 *
 * @param groupId 群组id
 * @return 群禁言列表
 */
suspend fun NapCatBotApi.getGroupBanList(
    groupId: String = this.receiveMessage.source.id
): ArrayNode {
    return apiRequest<ArrayNode>(
        "get_group_ban_list",
        mapOf("group_id" to groupId)
    ) ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取群过滤系统消息
 *
 * @return 群过滤系统消息
 */
suspend fun NapCatBotApi.getGroupFilteredSystemMsg(): JsonNode? {
    return apiRequest("get_group_filtered_system_msg")
}

/**
 * 群打卡
 *
 * @param groupId 群组id
 */
suspend fun NapCatBotApi.groupSignIn(
    groupId: String = this.receiveMessage.source.id
) {
    apiRequestUnit(
        "group_sign_in",
        mapOf("group_id" to groupId)
    )
}

/**
 * 设置群代办
 *
 * @param groupId 群组id
 * @param title 代办标题
 * @param content 代办内容
 * @param enable 是否启用
 */
suspend fun NapCatBotApi.setGroupTodo(
    groupId: String = this.receiveMessage.source.id,
    title: String,
    content: String,
    enable: Boolean = true
) {
    apiRequestUnit(
        "set_group_todo",
        mapOf(
            "group_id" to groupId,
            "title" to title,
            "content" to content,
            "enable" to enable
        )
    )
}


