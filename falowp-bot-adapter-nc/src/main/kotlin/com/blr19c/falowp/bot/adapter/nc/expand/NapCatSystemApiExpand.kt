@file:Suppress("unused", "SpellCheckingInspection")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import java.net.URI
import java.util.*
import java.util.Locale.getDefault

/**
 * NapCatSystemApiExpand 系统API
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
     * ClientKey
     */
    data class ClientKey(
        /**
         * 客户端Key
         */
        @field:JsonProperty("clientkey")
        val clientKey: String?
    )

    /**
     * DoubtFriends
     */
    data class DoubtFriends(
        /**
         * 用户QQ
         */
        @field:JsonProperty("user_id")
        val userId: Long,
        /**
         * 昵称
         */
        @field:JsonProperty("nickname")
        val nickname: String,
        /**
         * 性别
         */
        @field:JsonProperty("sex")
        val sex: String,
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
         * QQ号
         */
        @field:JsonProperty("user_id")
        val userId: String,
        /**
         * 昵称
         */
        @field:JsonProperty("nickname")
        val nickname: String,
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

    /**
     * RkeyItemItem
     */
    data class RKeyItemItem(
        /**
         * 类型 (private/group)
         */
        @field:JsonProperty("type")
        val type: String,
        /**
         * RKey
         */
        @field:JsonProperty("rkey")
        val rKey: String,
        /**
         * 创建时间
         */
        @field:JsonProperty("created_at")
        val createdAt: Long,
        /**
         * 有效期
         */
        @field:JsonProperty("ttl")
        val ttl: Long
    )

    /**
     * RkeyServer
     */
    data class RkeyServer(
        /**
         * 私聊 RKey
         */
        @field:JsonProperty("private_rkey")
        val privateRKey: String?,
        /**
         * 群聊 RKey
         */
        @field:JsonProperty("group_rkey")
        val groupRKey: String?,
        /**
         * 过期时间
         */
        @field:JsonProperty("expired_time")
        val expiredTime: Long?,
        /**
         * 名称
         */
        @field:JsonProperty("name")
        val name: String
    )

    /**
     * NcGetUserStatus
     */
    data class NcGetUserStatus(
        /**
         * 在线状态
         */
        @field:JsonProperty("status")
        val status: Long,
        /**
         * 扩展状态
         */
        @field:JsonProperty("ext_status")
        val extStatus: Long
    )

}

/**
 * 是否可以发送图片
 */
suspend fun NapCatBotApi.canSendImage() {
    apiRequestUnit("can_send_image")
}

/**
 * 是否可以发送语音
 */
suspend fun NapCatBotApi.canSendRecord() {
    apiRequestUnit("can_send_record")
}

/**
 * 清理缓存
 */
suspend fun NapCatBotApi.cleanCache() {
    apiRequestUnit("clean_cache")
}

/**
 * 获取登录凭证
 *
 * @param domain 域名
 */
suspend fun NapCatBotApi.getCredentials(domain: String): NapCatSystemApiExpand.Credentials {
    return apiRequest("get_credentials", mapOf("domain" to domain))
}

/**
 * 获取 CSRF Token
 */
suspend fun NapCatBotApi.getCsrfToken(): NapCatSystemApiExpand.CsrfToken {
    return apiRequest("get_csrf_token")
}

/**
 * 获取可疑好友申请
 *
 * @param count 获取数量
 */
suspend fun NapCatBotApi.getDoubtFriendsAddRequest(count: Long): List<NapCatSystemApiExpand.DoubtFriends> {
    return apiRequest("get_doubt_friends_add_request", mapOf("count" to count))
}

/**
 * 获取群系统消息
 *
 * @param count 获取数量
 */
suspend fun NapCatBotApi.getGroupSystemMsg(count: Long): NapCatSystemApiExpand.GroupSystemMsg {
    return apiRequest("get_group_system_msg", mapOf("count" to count))
}

/**
 * 获取登录号信息
 */
suspend fun NapCatBotApi.getLoginInfo(): NapCatSystemApiExpand.LoginInfo {
    return apiRequest("get_login_info")
}

/**
 * 获取运行状态
 */
suspend fun NapCatBotApi.getStatus(): NapCatSystemApiExpand.Status {
    return apiRequest("get_status")
}

/**
 * 获取版本信息
 */
suspend fun NapCatBotApi.getVersionInfo(): NapCatSystemApiExpand.VersionInfo {
    return apiRequest("get_version_info")
}

/**
 * 获取Packet状态
 */
suspend fun NapCatBotApi.ncGetPacketStatus(): JsonNode {
    return apiRequest("nc_get_packet_status")
}

/**
 * 处理可疑好友申请
 *
 * 同意或拒绝系统的可疑好友申请
 *
 * @param flag 请求标识
 * @param approve 是否同意
 */
suspend fun NapCatBotApi.setDoubtFriendsAddRequest(flag: String, approve: Boolean) {
    apiRequestUnit("set_doubt_friends_add_request", mapOf("flag" to flag, "approve" to approve))
}

/**
 * 重启服务
 */
suspend fun NapCatBotApi.setRestart() {
    apiRequestUnit("set_restart")
}

/**
 * 退出登录
 */
suspend fun NapCatBotApi.botExit() {
    apiRequestUnit("bot_exit")
}

/**
 * 获取自定义表情
 *
 * @param count 获取数量
 */
suspend fun NapCatBotApi.fetchCustomFace(count: Long): List<URI> {
    return apiRequest("fetch_custom_face", mapOf("count" to count))
}

/**
 * 获取收藏列表
 *
 * @param category 收藏分类
 * @param count 获取数量
 */
suspend fun NapCatBotApi.getCollectionList(category: String, count: String): String {
    return apiRequest("get_collection_list", mapOf("category" to category, "count" to count))
}

/**
 * 获取小程序 Ark
 *
 * @param type 类型
 * @param title 标题
 * @param desc 描述
 * @param picUrl 图片URL
 * @param jumpUrl 跳转URL
 */
suspend fun NapCatBotApi.getMiniAppArk(
    type: String,
    title: String,
    desc: String,
    picUrl: String,
    jumpUrl: String
): JsonNode {
    return apiRequest<JsonNode>(
        "get_mini_app_ark",
        mapOf("type" to type, "title" to title, "desc" to desc, "picUrl" to picUrl, "jumpUrl" to jumpUrl)
    ).path("data")
}

/**
 * 获取自定义小程序 Ark
 *
 * @param appId 小程序ID
 * @param title 标题
 * @param desc 描述
 * @param picUrl 图片URL
 * @param pathUrl 小程序内URL
 * @param docUrl 浏览器打开URL
 * @param iconUrl 小程序图标
 */
suspend fun NapCatBotApi.getCustomMiniAppArk(
    appId: String,
    title: String,
    desc: String,
    picUrl: String,
    pathUrl: String,
    docUrl: String,
    iconUrl: String
): JsonNode {
    return apiRequest<JsonNode>(
        "get_mini_app_ark",
        mapOf(
            "title" to title,
            "desc" to desc,
            "picUrl" to picUrl,
            "jumpUrl" to pathUrl,
            "webUrl" to docUrl,
            "iconUrl" to iconUrl,
            "appId" to appId,
            "scene" to "1036",
            "templateType" to "1",
            "businessType" to "0",
            "verType" to "3",
            "shareType" to "0",
            "versionId" to UUID.randomUUID().toString().replace("-", "").lowercase(getDefault()),
            "sdkId" to "V1_PC_MINISDK_99.99.99_1_APP_A",
            "withShareTicket" to "0",
            "rawArkData" to "false"
        )
    ).path("data")
}

/**
 * 获取扩展 RKey
 */
suspend fun NapCatBotApi.getRKey(): List<NapCatSystemApiExpand.RKeyItemItem> {
    return apiRequest("get_rkey")
}

/**
 * 获取 RKey 服务器
 */
suspend fun NapCatBotApi.getRKeyServer(): NapCatSystemApiExpand.RkeyServer {
    return apiRequest("get_rkey_server")
}

/**
 * 获取机器人 UIN 范围
 */
suspend fun NapCatBotApi.getRobotUinRange(): List<String> {
    return apiRequest("get_robot_uin_range")
}

/**
 * 获取 RKey
 */
suspend fun NapCatBotApi.ncGetRKey(): List<String> {
    return apiRequest("nc_get_rkey")
}

/**
 * 获取用户在线状态
 *
 * @param userId 用户ID
 */
suspend fun NapCatBotApi.ncGetUserStatus(userId: String = this.receiveMessage.sender.id): NapCatSystemApiExpand.NcGetUserStatus {
    return apiRequest("nc_get_user_status", mapOf("user_id" to userId))
}

/**
 * 发送原始数据包
 *
 * @param cmd 命令字
 * @param data 数据内容
 * @param rsp 响应配置
 */
suspend fun NapCatBotApi.sendPacket(cmd: String, data: String, rsp: String) {
    apiRequestUnit("send_packet", mapOf("cmd" to cmd, "data" to data, "rsp" to rsp))
}

/**
 * 设置输入状态
 *
 * @param userId 用户ID
 * @param eventType 事件类型
 */
suspend fun NapCatBotApi.setInputStatus(userId: String = this.receiveMessage.sender.id, eventType: Long) {
    apiRequestUnit("set_input_status", mapOf("user_id" to userId, "event_type" to eventType))
}

/**
 * 创建收藏
 *
 * @param rawData 原始数据
 * @param brief 简要描述
 */
suspend fun NapCatBotApi.createCollection(rawData: String, brief: String) {
    apiRequestUnit("create_collection", mapOf("rawData" to rawData, "brief" to brief))
}

/**
 * 获取ClientKey
 */
suspend fun NapCatBotApi.getClientKey(): NapCatSystemApiExpand.ClientKey {
    return apiRequest("get_clientkey")
}

/**
 * 图片 OCR 识别
 *
 * 识别图片中的文字内容(仅Windows端支持)
 *
 * @param image 图片数据
 */
suspend fun NapCatBotApi.ocrImage(image: String) {
    apiRequestUnit("ocr_image", mapOf("image" to image))
}

/**
 * 英文单词翻译
 *
 * @param words 英文单词列表
 */
suspend fun NapCatBotApi.translateEn2zh(words: List<String>) {
    apiRequestUnit("translate_en2zh", mapOf("words" to words))
}
