package com.blr19c.falowp.bot.adapter.nc.expand.bak

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode

/**
 * 账号类扩展
 */
class NapCatAccountExpand {

    /**
     * 好友信息
     */
    data class FriendUser(
        /**
         * 用户ID
         */
        @field:JsonProperty("user_id")
        val userId: String,
        /**
         * 好友分组ID
         */
        @field:JsonProperty("category_id")
        val categoryId: Int,
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
         * 出生年份
         */
        @field:JsonProperty("birthday_year")
        val birthdayYear: Int?,
        /**
         * 出生月份
         */
        @field:JsonProperty("birthday_month")
        val birthdayMonth: Int?,
        /**
         * 出生日
         */
        @field:JsonProperty("birthday_day")
        val birthdayDay: Int?,
        /**
         * 年龄
         */
        @field:JsonProperty("age")
        val age: Int?,
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
         * 性别
         */
        @field:JsonProperty("sex")
        val sex: String,
        /**
         * 等级
         */
        @field:JsonProperty("level")
        val level: Int
    ) {

        fun toUser(): ReceiveMessage.User {
            return ReceiveMessage.User(
                userId,
                if (remark.isNullOrBlank()) nickname else remark,
                NapCatBotApiSupport.apiAuth(userId),
                NapCatBotApiSupport.avatar(userId)
            )
        }
    }
}

/**
 * 设置私聊信息已读
 *
 * @param userId 用户id
 */
suspend fun NapCatBotApi.markPrivateMsgAsRead(userId: String = this.receiveMessage.sender.id) {
    apiRequestUnit("mark_private_msg_as_read", mapOf("user_id" to userId))
}

/**
 * 设置群聊信息已读
 *
 * @param groupId 群组id
 */
suspend fun NapCatBotApi.markGroupMsgAsRead(groupId: String = this.receiveMessage.source.id) {
    apiRequestUnit("mark_group_msg_as_read", mapOf("group_id" to groupId))
}

/**
 * 设置所有消息已读
 */
suspend fun NapCatBotApi.markAllAsRead() {
    apiRequestUnit("_mark_all_as_read")
}

/**
 * 获取最近的聊天记录
 */
suspend fun NapCatBotApi.getRecentContact(): ArrayNode {
    return apiRequest<ArrayNode>("get_recent_contact") ?: Json.objectMapper().createArrayNode()
}

/**
 * 点赞
 *
 * @param userId 用户id
 * @param count 点赞次数（默认1）
 */
suspend fun NapCatBotApi.likeUser(userId: String = this.receiveMessage.sender.id, count: Int = 1) {
    apiRequestUnit("send_like", mapOf("user_id" to userId, "times" to count))
}


/**
 * 处理好友请求
 *
 * @param flag 请求标识
 * @param approve 是否同意
 * @param remark 备注
 */
suspend fun NapCatBotApi.handleFriendRequest(flag: String, approve: Boolean, remark: String? = null) {
    apiRequestUnit(
        "set_friend_add_request", mapOf("flag" to flag, "approve" to approve, "remark" to remark)
    )
}

/**
 * 获取账号信息
 */
suspend fun NapCatBotApi.getAccountInfo(): JsonNode? {
    return apiRequest("get_account_info")
}

/**
 * 获取好友列表
 */
suspend fun NapCatBotApi.getFriendList(): List<NapCatAccountExpand.FriendUser> {
    return apiRequest("get_friend_list")
}

/**
 * 获取好友分组列表
 */
suspend fun NapCatBotApi.getFriendGroupList(): ArrayNode {
    return apiRequest<ArrayNode>("get_friend_group_list") ?: Json.objectMapper().createArrayNode()
}

/**
 * 设置账号信息
 *
 * @param nickname 昵称
 * @param gender 性别（0未知 1男 2女）
 * @param birthday 生日时间戳
 */
suspend fun NapCatBotApi.setAccountInfo(nickname: String, gender: Int, birthday: Long) {
    apiRequestUnit(
        "set_account_info", mapOf("nickname" to nickname, "gender" to gender, "birthday" to birthday)
    )
}

/**
 * 删除好友
 *
 * @param userId 用户id
 */
suspend fun NapCatBotApi.deleteFriend(userId: String = this.receiveMessage.sender.id) {
    apiRequestUnit("delete_friend", mapOf("user_id" to userId))
}

/**
 * 获取推荐好友/群聊卡片
 */
suspend fun NapCatBotApi.getRecommendContact(): ArrayNode {
    return apiRequest<ArrayNode>("get_recommend_contact") ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取推荐群聊卡片
 */
suspend fun NapCatBotApi.getRecommendGroup(): ArrayNode {
    return apiRequest<ArrayNode>("get_recommend_group") ?: Json.objectMapper().createArrayNode()
}

/**
 * 设置在线状态
 *
 * @param status 在线状态
 */
suspend fun NapCatBotApi.setOnlineStatus(status: String) {
    apiRequestUnit("set_online_status", mapOf("status" to status))
}

/**
 * 设置自定义在线状态
 *
 * @param wording 状态文案
 * @param emojiId 表情ID
 */
suspend fun NapCatBotApi.setCustomOnlineStatus(wording: String, emojiId: String) {
    apiRequestUnit(
        "set_custom_online_status", mapOf("wording" to wording, "emoji_id" to emojiId)
    )
}


/**
 * 设置头像
 *
 * @param image 图片数据
 */
suspend fun NapCatBotApi.setAvatar(image: ImageUrl) {
    apiRequestUnit("set_avatar", mapOf("image" to image.toBase64()))
}

/**
 * 创建收藏
 *
 * @param content 收藏内容
 * @param type 收藏类型
 */
suspend fun NapCatBotApi.createCollection(content: String, type: Int) {
    apiRequestUnit(
        "create_collection", mapOf("content" to content, "type" to type)
    )
}

/**
 * 设置个性签名
 *
 * @param signature 签名内容
 */
suspend fun NapCatBotApi.setSignature(signature: String) {
    apiRequestUnit("set_signature", mapOf("signature" to signature))
}

/**
 * 获取收藏表情
 */
suspend fun NapCatBotApi.getCollectionEmoji(): ArrayNode {
    return apiRequest<ArrayNode>("get_collection_emoji") ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取点赞列表
 */
suspend fun NapCatBotApi.getLikeList(): ArrayNode {
    return apiRequest<ArrayNode>("get_like_list") ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取用户状态
 *
 * @param userId 用户id
 */
suspend fun NapCatBotApi.getUserStatus(userId: String = this.receiveMessage.sender.id): JsonNode? {
    return apiRequest("get_user_status", mapOf("user_id" to userId))
}

/**
 * 获取小程序卡片
 *
 * @param appId 小程序appid
 */
suspend fun NapCatBotApi.getMiniAppCard(appId: String): JsonNode? {
    return apiRequest("get_mini_app_card", mapOf("app_id" to appId))
}

/**
 * 获取单向好友列表
 */
suspend fun NapCatBotApi.getUnidirectionalFriendList(): ArrayNode {
    return apiRequest<ArrayNode>("get_unidirectional_friend_list") ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取登录号信息
 */
suspend fun NapCatBotApi.getLoginInfo(): JsonNode? {
    return apiRequest("get_login_info")
}

/**
 * 获取状态
 */
suspend fun NapCatBotApi.getStatus(): JsonNode? {
    return apiRequest("get_status")
}

/**
 * 获取当前账号在线客户端列表
 */
suspend fun NapCatBotApi.getOnlineClients(): ArrayNode {
    return apiRequest<ArrayNode>("get_online_clients") ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取在线机型
 */
suspend fun NapCatBotApi.getOnlineModel(): JsonNode? {
    return apiRequest("_get_online_model")
}

/**
 * 设置在线机型
 *
 * @param model 机型
 */
suspend fun NapCatBotApi.setOnlineModel(model: String) {
    apiRequestUnit("_set_online_model", mapOf("model" to model))
}

/**
 * 获取被过滤好友请求
 */
suspend fun NapCatBotApi.getFilteredFriendRequest(): ArrayNode {
    return apiRequest<ArrayNode>("get_filtered_friend_request") ?: Json.objectMapper().createArrayNode()
}

/**
 * 处理被过滤好友请求
 *
 * @param flag 请求标识
 * @param approve 是否同意
 */
suspend fun NapCatBotApi.handleFilteredFriendRequest(flag: String, approve: Boolean) {
    apiRequestUnit(
        "handle_filtered_friend_request", mapOf("flag" to flag, "approve" to approve)
    )
}

/**
 * 设置好友备注
 *
 * @param userId 用户id
 * @param remark 备注
 */
suspend fun NapCatBotApi.setFriendRemark(userId: String = this.receiveMessage.sender.id, remark: String) {
    apiRequestUnit(
        "set_friend_remark", mapOf("user_id" to userId, "remark" to remark)
    )
}
