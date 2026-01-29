@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GetCookiesData
 */
data class GetCookiesData(
    @field:JsonProperty("cookies")
    val cookies: String,
    @field:JsonProperty("bkn")
    val bkn: String
)

/**
 * GetRecentContactDataItemLastestMsgSender
 */
data class GetRecentContactDataItemLastestMsgSender(
    @field:JsonProperty("user_id")
    val userId: Long,
    @field:JsonProperty("nickname")
    val nickname: String,
    @field:JsonProperty("sex")
    val sex: String?,
    @field:JsonProperty("age")
    val age: Long?,
    @field:JsonProperty("card")
    val card: String,
    @field:JsonProperty("role")
    val role: String?
)

/**
 * GetRecentContactDataItemLastestMsg
 */
data class GetRecentContactDataItemLastestMsg(
    @field:JsonProperty("self_id")
    val selfId: Long,
    @field:JsonProperty("user_id")
    val userId: Long,
    @field:JsonProperty("time")
    val time: Long,
    @field:JsonProperty("real_seq")
    val realSeq: String,
    @field:JsonProperty("message_type")
    val messageType: String,
    @field:JsonProperty("sender")
    val sender: GetRecentContactDataItemLastestMsgSender,
    @field:JsonProperty("raw_message")
    val rawMessage: String,
    @field:JsonProperty("font")
    val font: Long,
    @field:JsonProperty("sub_type")
    val subType: String,
    @field:JsonProperty("message")
    val message: List<Any>,
    @field:JsonProperty("message_format")
    val messageFormat: String,
    @field:JsonProperty("post_type")
    val postType: String,
    @field:JsonProperty("group_id")
    val groupId: Long?
)

/**
 * GetRecentContactDataItem
 */
data class GetRecentContactDataItem(
    @field:JsonProperty("lastestMsg")
    val lastestMsg: GetRecentContactDataItemLastestMsg?,
    @field:JsonProperty("peerUin")
    val peerUin: String,
    @field:JsonProperty("remark")
    val remark: String,
    @field:JsonProperty("msgTime")
    val msgTime: String,
    @field:JsonProperty("chatType")
    val chatType: Long,
    @field:JsonProperty("msgId")
    val msgId: String,
    @field:JsonProperty("sendNickName")
    val sendNickName: String,
    @field:JsonProperty("sendMemberName")
    val sendMemberName: String,
    @field:JsonProperty("peerName")
    val peerName: String
)

/**
 * NapCatUserApiExpand
 */
class NapCatUserApiExpand

/**
 * 获取cookies
 */
suspend fun NapCatBotApi.getCookies(domain: String): GetCookiesData {
    return apiRequest("get_cookies", mapOf("domain" to domain))
}

/**
 * 获取好友列表
 */
suspend fun NapCatBotApi.getFriendList(noCache: Boolean): List<NapCatRawData> {
    return apiRequest("get_friend_list", mapOf("no_cache" to noCache))
}

/**
 * 最近消息列表
 *
 * 获取的最新消息是每个会话最新的消息
 */
suspend fun NapCatBotApi.getRecentContact(count: Long? = null): List<GetRecentContactDataItem> {
    return apiRequest("get_recent_contact", mapOf("count" to count))
}

/**
 * 点赞
 */
suspend fun NapCatBotApi.sendLike(userId: Long, times: Long? = null) {
    apiRequestUnit("send_like", mapOf("user_id" to userId, "times" to times))
}

/**
 * 处理好友请求
 */
suspend fun NapCatBotApi.setFriendAddRequest(flag: String, approve: Boolean, remark: String) {
    apiRequestUnit("set_friend_add_request", mapOf("flag" to flag, "approve" to approve, "remark" to remark))
}

/**
 * 设置好友备注
 */
suspend fun NapCatBotApi.setFriendRemark(userId: Long, remark: String) {
    apiRequestUnit("set_friend_remark", mapOf("user_id" to userId, "remark" to remark))
}
