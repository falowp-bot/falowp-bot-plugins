@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatSystemApiExpand
 */
class NapCatSystemApiExpand {
    /**
     * Credentials
     */
    data class Credentials(
        /**
         * Cookies
         */
        @field:JsonProperty("cookies")
        val cookies: String,
        /**
         * CSRF Token
         */
        @field:JsonProperty("token")
        val token: Long
    )

    /**
     * CsrfToken
     */
    data class CsrfToken(
        /**
         * CSRF Token
         */
        @field:JsonProperty("token")
        val token: Long
    )

    /**
     * GroupSystemMsgInvitedRequestsItem
     */
    data class GroupSystemMsgInvitedRequestsItem(
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
        val invitorNick: String,
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
         * 附言
         */
        @field:JsonProperty("message")
        val message: String,
        /**
         * 是否已处理
         */
        @field:JsonProperty("checked")
        val checked: Boolean,
        /**
         * 操作者QQ
         */
        @field:JsonProperty("actor")
        val actor: Long,
        /**
         * 申请者昵称
         */
        @field:JsonProperty("requester_nick")
        val requesterNick: String
    )

    /**
     * GroupSystemMsgInvitedRequestItem
     */
    data class GroupSystemMsgInvitedRequestItem(
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
        val invitorNick: String,
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
         * 附言
         */
        @field:JsonProperty("message")
        val message: String,
        /**
         * 是否已处理
         */
        @field:JsonProperty("checked")
        val checked: Boolean,
        /**
         * 操作者QQ
         */
        @field:JsonProperty("actor")
        val actor: Long,
        /**
         * 申请者昵称
         */
        @field:JsonProperty("requester_nick")
        val requesterNick: String
    )

    /**
     * GroupSystemMsgJoinRequestsItem
     */
    data class GroupSystemMsgJoinRequestsItem(
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
        val invitorNick: String,
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
         * 附言
         */
        @field:JsonProperty("message")
        val message: String,
        /**
         * 是否已处理
         */
        @field:JsonProperty("checked")
        val checked: Boolean,
        /**
         * 操作者QQ
         */
        @field:JsonProperty("actor")
        val actor: Long,
        /**
         * 申请者昵称
         */
        @field:JsonProperty("requester_nick")
        val requesterNick: String
    )

    /**
     * GroupSystemMsg
     */
    data class GroupSystemMsg(
        /**
         * 进群邀请列表
         */
        @field:JsonProperty("invited_requests")
        val invitedRequests: List<GroupSystemMsgInvitedRequestsItem>,
        /**
         * 进群邀请列表 (兼容)
         */
        @field:JsonProperty("InvitedRequest")
        val InvitedRequest: List<GroupSystemMsgInvitedRequestItem>,
        /**
         * 进群申请列表
         */
        @field:JsonProperty("join_requests")
        val joinRequests: List<GroupSystemMsgJoinRequestsItem>
    )

    /**
     * LoginInfo
     */
    data class LoginInfo(
        /**
         * 出生年份
         */
        @field:JsonProperty("birthday_year")
        val birthdayYear: Long?,
        /**
         * 出生月份
         */
        @field:JsonProperty("birthday_month")
        val birthdayMonth: Long?,
        /**
         * 出生日期
         */
        @field:JsonProperty("birthday_day")
        val birthdayDay: Long?,
        /**
         * 手机号
         */
        @field:JsonProperty("phone_num")
        val phoneNum: String?,
        /**
         * 邮箱
         */
        @field:JsonProperty("email")
        val email: String?,
        /**
         * 分组ID
         */
        @field:JsonProperty("category_id")
        val categoryId: Long?,
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
         * 备注
         */
        @field:JsonProperty("remark")
        val remark: String?,
        /**
         * 性别
         */
        @field:JsonProperty("sex")
        val sex: String?,
        /**
         * 等级
         */
        @field:JsonProperty("level")
        val level: Long?,
        /**
         * 年龄
         */
        @field:JsonProperty("age")
        val age: Long?,
        /**
         * QID
         */
        @field:JsonProperty("qid")
        val qid: String?,
        /**
         * 登录天数
         */
        @field:JsonProperty("login_days")
        val loginDays: Long?,
        /**
         * 分组名称
         */
        @field:JsonProperty("categoryName")
        val categoryName: String?,
        /**
         * 分组ID
         */
        @field:JsonProperty("categoryId")
        val categoryId: Long?
    )

    /**
     * Status
     */
    data class Status(
        /**
         * 是否在线
         */
        @field:JsonProperty("online")
        val online: Boolean,
        /**
         * 状态是否良好
         */
        @field:JsonProperty("good")
        val good: Boolean,
        /**
         * stat
         */
        @field:JsonProperty("stat")
        val stat: String
    )

    /**
     * VersionInfo
     */
    data class VersionInfo(
        /**
         * 应用名称
         */
        @field:JsonProperty("app_name")
        val appName: String,
        /**
         * 协议版本
         */
        @field:JsonProperty("protocol_version")
        val protocolVersion: String,
        /**
         * 应用版本
         */
        @field:JsonProperty("app_version")
        val appVersion: String
    )

}

/**
 * 是否可以发送图片
 *
 * 检查是否可以发送图片
 */
suspend fun NapCatBotApi.canSendImage() {
    apiRequestUnit("can_send_image")
}

/**
 * 是否可以发送语音
 *
 * 检查是否可以发送语音
 */
suspend fun NapCatBotApi.canSendRecord() {
    apiRequestUnit("can_send_record")
}

/**
 * 清理缓存
 *
 * 清理缓存
 */
suspend fun NapCatBotApi.cleanCache() {
    apiRequestUnit("clean_cache")
}

/**
 * 获取登录凭证
 *
 * 获取登录凭证
 * @param domain 需要获取 cookies 的域名
 */
suspend fun NapCatBotApi.getCredentials(domain: String): NapCatSystemApiExpand.Credentials {
    return apiRequest("get_credentials", mapOf("domain" to domain))
}

/**
 * 获取 CSRF Token
 *
 * 获取 CSRF Token
 */
suspend fun NapCatBotApi.getCsrfToken(): NapCatSystemApiExpand.CsrfToken {
    return apiRequest("get_csrf_token")
}

/**
 * 获取可疑好友申请
 *
 * 获取系统的可疑好友申请列表
 * @param count 获取数量
 */
suspend fun NapCatBotApi.getDoubtFriendsAddRequest(count: Long): NapCatSystemApiExpand.String {
    return apiRequest("get_doubt_friends_add_request", mapOf("count" to count))
}

/**
 * 获取群系统消息
 *
 * 获取群系统消息
 * @param count 获取的消息数量
 */
suspend fun NapCatBotApi.getGroupSystemMsg(count: Long): NapCatSystemApiExpand.GroupSystemMsg {
    return apiRequest("get_group_system_msg", mapOf("count" to count))
}

/**
 * 获取登录号信息
 *
 * 获取当前登录帐号的信息
 */
suspend fun NapCatBotApi.getLoginInfo(): NapCatSystemApiExpand.LoginInfo {
    return apiRequest("get_login_info")
}

/**
 * 获取运行状态
 *
 * 获取运行状态
 */
suspend fun NapCatBotApi.getStatus(): NapCatSystemApiExpand.Status {
    return apiRequest("get_status")
}

/**
 * 获取版本信息
 *
 * 获取版本信息
 */
suspend fun NapCatBotApi.getVersionInfo(): NapCatSystemApiExpand.VersionInfo {
    return apiRequest("get_version_info")
}

/**
 * 获取Packet状态
 *
 * 获取底层Packet服务的运行状态
 */
suspend fun NapCatBotApi.ncGetPacketStatus(): JsonNode {
    return apiRequest("nc_get_packet_status")
}

/**
 * 处理可疑好友申请
 *
 * 同意或拒绝系统的可疑好友申请
 * @param flag 请求 flag
 * @param approve 是否同意 (强制为 true)
 */
suspend fun NapCatBotApi.setDoubtFriendsAddRequest(flag: String, approve: Boolean) {
    apiRequestUnit("set_doubt_friends_add_request", mapOf("flag" to flag, "approve" to approve))
}

/**
 * 重启服务
 *
 * 重启服务
 */
suspend fun NapCatBotApi.setRestart() {
    apiRequestUnit("set_restart")
}
