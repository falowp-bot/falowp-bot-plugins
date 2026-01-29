@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatSystemExtraExpand
 */
class NapCatSystemExtraExpand {
    /**
     * MiniAppArk
     */
    data class MiniAppArk(
        /**
         * data
         */
        @field:JsonProperty("data")
        val data: String
    )

    /**
     * RkeyItemItem
     */
    data class RkeyItemItem(
        /**
         * 类型 (private/group)
         */
        @field:JsonProperty("type")
        val type: String,
        /**
         * RKey
         */
        @field:JsonProperty("rkey")
        val rkey: String,
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
        val privateRkey: String?,
        /**
         * 群聊 RKey
         */
        @field:JsonProperty("group_rkey")
        val groupRkey: String?,
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
 * 退出登录
 */
suspend fun NapCatBotApi.botExit() {
    apiRequestUnit("bot_exit")
}

/**
 * 获取自定义表情
 * @param count 获取数量
 */
suspend fun NapCatBotApi.fetchCustomFace(count: Long): NapCatSystemExtraExpand.List<String> {
    return apiRequest("fetch_custom_face", mapOf("count" to count))
}

/**
 * 获取收藏列表
 * @param category 分类ID
 * @param count 获取数量
 */
suspend fun NapCatBotApi.getCollectionList(category: String, count: String): NapCatSystemExtraExpand.String {
    return apiRequest("get_collection_list", mapOf("category" to category, "count" to count))
}

/**
 * 获取小程序 Ark
 */
suspend fun NapCatBotApi.getMiniAppArk(): NapCatSystemExtraExpand.MiniAppArk {
    return apiRequest("get_mini_app_ark")
}

/**
 * 获取扩展 RKey
 */
suspend fun NapCatBotApi.getRkey(): NapCatSystemExtraExpand.List<RkeyItemItem> {
    return apiRequest("get_rkey")
}

/**
 * 获取 RKey 服务器
 */
suspend fun NapCatBotApi.getRkeyServer(): NapCatSystemExtraExpand.RkeyServer {
    return apiRequest("get_rkey_server")
}

/**
 * 获取机器人 UIN 范围
 */
suspend fun NapCatBotApi.getRobotUinRange(): NapCatSystemExtraExpand.List<String> {
    return apiRequest("get_robot_uin_range")
}

/**
 * 获取 RKey
 */
suspend fun NapCatBotApi.ncGetRkey(): NapCatSystemExtraExpand.List<String> {
    return apiRequest("nc_get_rkey")
}

/**
 * 获取用户在线状态
 * @param userId QQ号
 */
suspend fun NapCatBotApi.ncGetUserStatus(userId: String): NapCatSystemExtraExpand.NcGetUserStatus {
    return apiRequest("nc_get_user_status", mapOf("user_id" to userId))
}

/**
 * 发送原始数据包
 * @param cmd 命令字
 * @param data 十六进制数据
 * @param rsp 是否等待响应
 */
suspend fun NapCatBotApi.sendPacket(cmd: String, data: String, rsp: String) {
    apiRequestUnit("send_packet", mapOf("cmd" to cmd, "data" to data, "rsp" to rsp))
}

/**
 * 设置输入状态
 * @param userId QQ号
 * @param eventType 事件类型
 */
suspend fun NapCatBotApi.setInputStatus(userId: String, eventType: Long) {
    apiRequestUnit("set_input_status", mapOf("user_id" to userId, "event_type" to eventType))
}
