@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatGroupApiExpand
 */
class NapCatGroupApiExpand {
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
     * EssenceMsgItem
     */
    data class EssenceMsgItem(
        /**
         * 消息序号
         */
        @field:JsonProperty("msg_seq")
        val msgSeq: Long,
        /**
         * 消息随机数
         */
        @field:JsonProperty("msg_random")
        val msgRandom: Long,
        /**
         * 发送者QQ
         */
        @field:JsonProperty("sender_id")
        val senderId: Long,
        /**
         * 发送者昵称
         */
        @field:JsonProperty("sender_nick")
        val senderNick: String,
        /**
         * 操作者QQ
         */
        @field:JsonProperty("operator_id")
        val operatorId: Long,
        /**
         * 操作者昵称
         */
        @field:JsonProperty("operator_nick")
        val operatorNick: String,
        /**
         * 消息ID
         */
        @field:JsonProperty("message_id")
        val messageId: Long,
        /**
         * 操作时间
         */
        @field:JsonProperty("operator_time")
        val operatorTime: Long,
        /**
         * 消息内容
         */
        @field:JsonProperty("content")
        val content: List<String>
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
         * 邀请请求列表
         */
        @field:JsonProperty("InvitedRequest")
        val InvitedRequest: List<String>,
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
        val groupId: Long,
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
     * GroupItem
     */
    data class GroupItem(
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
        val groupId: Long,
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
     * GroupMemberInfo
     */
    data class GroupMemberInfo(
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: Long,
        /**
         * QQ号
         */
        @field:JsonProperty("user_id")
        val userId: Long,
        /**
         * 昵称
         */
        @field:JsonProperty("nickname")
        val nickname: String,
        /**
         * 名片
         */
        @field:JsonProperty("card")
        val card: String?,
        /**
         * 性别
         */
        @field:JsonProperty("sex")
        val sex: String?,
        /**
         * 年龄
         */
        @field:JsonProperty("age")
        val age: Long?,
        /**
         * 入群时间戳
         */
        @field:JsonProperty("join_time")
        val joinTime: Long?,
        /**
         * 最后发言时间戳
         */
        @field:JsonProperty("last_sent_time")
        val lastSentTime: Long?,
        /**
         * 等级
         */
        @field:JsonProperty("level")
        val level: String?,
        /**
         * QQ等级
         */
        @field:JsonProperty("qq_level")
        val qqLevel: Long?,
        /**
         * 角色 (owner/admin/member)
         */
        @field:JsonProperty("role")
        val role: String?,
        /**
         * 头衔
         */
        @field:JsonProperty("title")
        val title: String?,
        /**
         * 地区
         */
        @field:JsonProperty("area")
        val area: String?,
        /**
         * 是否不良记录
         */
        @field:JsonProperty("unfriendly")
        val unfriendly: Boolean?,
        /**
         * 头衔过期时间
         */
        @field:JsonProperty("title_expire_time")
        val titleExpireTime: Long?,
        /**
         * 是否允许修改名片
         */
        @field:JsonProperty("card_changeable")
        val cardChangeable: Boolean?,
        /**
         * 禁言截止时间戳
         */
        @field:JsonProperty("shut_up_timestamp")
        val shutUpTimestamp: Long?,
        /**
         * 是否为机器人
         */
        @field:JsonProperty("is_robot")
        val isRobot: Boolean?,
        /**
         * Q龄
         */
        @field:JsonProperty("qage")
        val qage: Long?
    )

}

/**
 * 删除群公告
 *
 * 删除群聊中的公告
 */
suspend fun NapCatBotApi.delGroupNotice(groupId: String, noticeId: String) {
    apiRequestUnit("_del_group_notice", mapOf("group_id" to groupId, "notice_id" to noticeId))
}

/**
 * 获取群公告
 *
 * 获取指定群聊中的公告列表
 */
suspend fun NapCatBotApi.getGroupNotice(groupId: String): NapCatGroupApiExpand.List<GroupNoticeItemItem> {
    return apiRequest("_get_group_notice", mapOf("group_id" to groupId))
}

/**
 * 移出精华消息
 *
 * 将一条消息从群精华消息列表中移出
 */
suspend fun NapCatBotApi.deleteEssenceMsg(messageId: Long? = null, msgSeq: String? = null, msgRandom: String? = null, groupId: String? = null) {
    apiRequestUnit("delete_essence_msg", mapOf("message_id" to messageId, "msg_seq" to msgSeq, "msg_random" to msgRandom, "group_id" to groupId))
}

/**
 * 获取群精华消息
 *
 * 获取指定群聊中的精华消息列表
 */
suspend fun NapCatBotApi.getEssenceMsgList(groupId: String): NapCatGroupApiExpand.List<EssenceMsgItem> {
    return apiRequest("get_essence_msg_list", mapOf("group_id" to groupId))
}

/**
 * 获取群详细信息
 *
 * 获取群聊的详细信息，包括成员数、最大成员数等
 */
suspend fun NapCatBotApi.getGroupDetailInfo(groupId: String): NapCatGroupApiExpand.GroupDetailInfo {
    return apiRequest("get_group_detail_info", mapOf("group_id" to groupId))
}

/**
 * 获取群被忽略的加群请求
 */
suspend fun NapCatBotApi.getGroupIgnoreAddRequest(): NapCatGroupApiExpand.List<GroupIgnoreAddRequestItemItem> {
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
 */
suspend fun NapCatBotApi.getGroupInfo(groupId: String): NapCatGroupApiExpand.GroupInfo {
    return apiRequest("get_group_info", mapOf("group_id" to groupId))
}

/**
 * 获取群列表
 *
 * 获取当前帐号的群聊列表
 */
suspend fun NapCatBotApi.getGroupList(noCache: Boolean? = null): NapCatGroupApiExpand.List<GroupItem> {
    return apiRequest("get_group_list", mapOf("no_cache" to noCache))
}

/**
 * 获取群成员信息
 *
 * 获取群聊中指定成员的信息
 */
suspend fun NapCatBotApi.getGroupMemberInfo(groupId: String, userId: String, noCache: Boolean? = null): NapCatGroupApiExpand.GroupMemberInfo {
    return apiRequest("get_group_member_info", mapOf("group_id" to groupId, "user_id" to userId, "no_cache" to noCache))
}

/**
 * 获取群成员列表
 *
 * 获取群聊中的所有成员列表
 */
suspend fun NapCatBotApi.getGroupMemberList(groupId: String, noCache: Boolean? = null): NapCatGroupApiExpand.List<String> {
    return apiRequest("get_group_member_list", mapOf("group_id" to groupId, "no_cache" to noCache))
}

/**
 * 获取群禁言列表
 */
suspend fun NapCatBotApi.getGroupShutList(groupId: String): NapCatGroupApiExpand.List<String> {
    return apiRequest("get_group_shut_list", mapOf("group_id" to groupId))
}

/**
 * 设置精华消息
 *
 * 将一条消息设置为群精华消息
 */
suspend fun NapCatBotApi.setEssenceMsg(messageId: Long) {
    apiRequestUnit("set_essence_msg", mapOf("message_id" to messageId))
}

/**
 * 处理加群请求
 *
 * 同意或拒绝加群请求或邀请
 */
suspend fun NapCatBotApi.setGroupAddRequest(flag: String, approve: Boolean? = null, reason: String? = null, count: Long? = null) {
    apiRequestUnit("set_group_add_request", mapOf("flag" to flag, "approve" to approve, "reason" to reason, "count" to count))
}

/**
 * 设置群管理员
 *
 * 设置或取消群聊中的管理员
 */
suspend fun NapCatBotApi.setGroupAdmin(groupId: String, userId: String, enable: Boolean? = null) {
    apiRequestUnit("set_group_admin", mapOf("group_id" to groupId, "user_id" to userId, "enable" to enable))
}

/**
 * 群组禁言
 *
 * 禁言群聊中的指定成员
 */
suspend fun NapCatBotApi.setGroupBan(groupId: String, userId: String, duration: Long) {
    apiRequestUnit("set_group_ban", mapOf("group_id" to groupId, "user_id" to userId, "duration" to duration))
}

/**
 * 设置群名片
 *
 * 设置群聊中指定成员的群名片
 */
suspend fun NapCatBotApi.setGroupCard(groupId: String, userId: String, card: String? = null) {
    apiRequestUnit("set_group_card", mapOf("group_id" to groupId, "user_id" to userId, "card" to card))
}

/**
 * 群组踢人
 *
 * 将指定成员踢出群聊
 */
suspend fun NapCatBotApi.setGroupKick(groupId: String, userId: String, rejectAddRequest: Boolean? = null) {
    apiRequestUnit("set_group_kick", mapOf("group_id" to groupId, "user_id" to userId, "reject_add_request" to rejectAddRequest))
}

/**
 * 退出群组
 *
 * 退出或解散指定群聊
 */
suspend fun NapCatBotApi.setGroupLeave(groupId: String, isDismiss: Boolean? = null) {
    apiRequestUnit("set_group_leave", mapOf("group_id" to groupId, "is_dismiss" to isDismiss))
}

/**
 * 设置群名称
 *
 * 修改指定群聊的名称
 */
suspend fun NapCatBotApi.setGroupName(groupId: String, groupName: String) {
    apiRequestUnit("set_group_name", mapOf("group_id" to groupId, "group_name" to groupName))
}

/**
 * 全员禁言
 *
 * 开启或关闭指定群聊的全员禁言
 */
suspend fun NapCatBotApi.setGroupWholeBan(groupId: String, enable: Boolean? = null) {
    apiRequestUnit("set_group_whole_ban", mapOf("group_id" to groupId, "enable" to enable))
}
