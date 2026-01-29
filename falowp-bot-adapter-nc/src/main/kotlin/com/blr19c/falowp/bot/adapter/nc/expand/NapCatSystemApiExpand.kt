@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * CanSendImageData
 */
data class CanSendImageData(
    @field:JsonProperty("yes")
    val yes: Boolean
)

/**
 * CanSendRecordData
 */
data class CanSendRecordData(
    @field:JsonProperty("yes")
    val yes: Boolean
)

/**
 * GetCredentialsData
 */
data class GetCredentialsData(
    @field:JsonProperty("cookies")
    val cookies: String,
    @field:JsonProperty("token")
    val token: Long
)

/**
 * GetCsrfTokenData
 */
data class GetCsrfTokenData(
    @field:JsonProperty("token")
    val token: Long
)

/**
 * GetDoubtFriendsAddRequestDataItem
 */
data class GetDoubtFriendsAddRequestDataItem(
    @field:JsonProperty("flag")
    val flag: String,
    @field:JsonProperty("uin")
    val uin: String,
    @field:JsonProperty("nick")
    val nick: String,
    @field:JsonProperty("source")
    val source: String,
    @field:JsonProperty("reason")
    val reason: String,
    @field:JsonProperty("msg")
    val msg: String,
    @field:JsonProperty("group_code")
    val groupCode: String,
    @field:JsonProperty("time")
    val time: String,
    @field:JsonProperty("type")
    val type: String
)

/**
 * GetGroupSystemMsgData
 */
data class GetGroupSystemMsgData(
    @field:JsonProperty("InvitedRequest")
    val InvitedRequest: List<Any>,
    @field:JsonProperty("join_requests")
    val joinRequests: List<Any>
)

/**
 * GetLoginInfoData
 */
data class GetLoginInfoData(
    @field:JsonProperty("user_id")
    val userId: Long,
    @field:JsonProperty("nickname")
    val nickname: String
)

/**
 * GetStatusData
 */
data class GetStatusData(
    @field:JsonProperty("online")
    val online: Boolean,
    @field:JsonProperty("good")
    val good: Boolean,
    @field:JsonProperty("stat")
    val stat: NapCatRawData
)

/**
 * GetVersionInfoData
 */
data class GetVersionInfoData(
    @field:JsonProperty("app_name")
    val appName: String,
    @field:JsonProperty("protocol_version")
    val protocolVersion: String,
    @field:JsonProperty("app_version")
    val appVersion: String
)

/**
 * NapCatSystemApiExpand
 */
class NapCatSystemApiExpand

/**
 * 检查是否可以发送图片
 */
suspend fun NapCatBotApi.canSendImage(): CanSendImageData {
    return apiRequest("can_send_image")
}

/**
 * 检查是否可以发送语音
 */
suspend fun NapCatBotApi.canSendRecord(): CanSendRecordData {
    return apiRequest("can_send_record")
}

/**
 * 清空缓存
 */
suspend fun NapCatBotApi.cleanCache() {
    apiRequestUnit("clean_cache")
}

/**
 * 获取 QQ 相关接口凭证
 */
suspend fun NapCatBotApi.getCredentials(domain: String): GetCredentialsData {
    return apiRequest("get_credentials", mapOf("domain" to domain))
}

/**
 * 获取 CSRF Token
 */
suspend fun NapCatBotApi.getCsrfToken(): GetCsrfTokenData {
    return apiRequest("get_csrf_token")
}

/**
 * 获取被过滤好友请求
 */
suspend fun NapCatBotApi.getDoubtFriendsAddRequest(count: Long): List<GetDoubtFriendsAddRequestDataItem> {
    return apiRequest("get_doubt_friends_add_request", mapOf("count" to count))
}

/**
 * 获取群系统消息
 */
suspend fun NapCatBotApi.getGroupSystemMsg(count: Long): GetGroupSystemMsgData {
    return apiRequest("get_group_system_msg", mapOf("count" to count))
}

/**
 * 获取登录号信息
 */
suspend fun NapCatBotApi.getLoginInfo(): GetLoginInfoData {
    return apiRequest("get_login_info")
}

/**
 * 获取状态
 */
suspend fun NapCatBotApi.getStatus(): GetStatusData {
    return apiRequest("get_status")
}

/**
 * 获取版本信息
 */
suspend fun NapCatBotApi.getVersionInfo(): GetVersionInfoData {
    return apiRequest("get_version_info")
}

/**
 * 获取packet状态
 */
suspend fun NapCatBotApi.ncGetPacketStatus(): Any {
    return apiRequest("nc_get_packet_status")
}

/**
 * 处理被过滤好友请求
 *
 *  在 4.7.43 版本中 
approve 的值无效
调用该接口既是同意好友请求！！！
调用该接口既是同意好友请求！！！
调用该接口既是同意好友请求！！！
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
