@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GetFriendsWithCategoryDataItem
 */
data class GetFriendsWithCategoryDataItem(
    @field:JsonProperty("categoryId")
    val categoryId: Long,
    @field:JsonProperty("categorySortId")
    val categorySortId: Long,
    @field:JsonProperty("categoryName")
    val categoryName: String,
    @field:JsonProperty("categoryMbCount")
    val categoryMbCount: Long,
    @field:JsonProperty("onlineCount")
    val onlineCount: Long,
    @field:JsonProperty("buddyList")
    val buddyList: List<Any>
)

/**
 * GetProfileLikeDataFavoriteInfo
 */
data class GetProfileLikeDataFavoriteInfo(
    @field:JsonProperty("total_count")
    val totalCount: Long,
    @field:JsonProperty("last_time")
    val lastTime: Long,
    @field:JsonProperty("today_count")
    val todayCount: Long,
    @field:JsonProperty("userInfos")
    val userInfos: List<Any>
)

/**
 * GetProfileLikeDataVoteInfo
 */
data class GetProfileLikeDataVoteInfo(
    @field:JsonProperty("total_count")
    val totalCount: Long,
    @field:JsonProperty("new_count")
    val newCount: Long,
    @field:JsonProperty("new_nearby_count")
    val newNearbyCount: Long,
    @field:JsonProperty("last_visit_time")
    val lastVisitTime: Long,
    @field:JsonProperty("userInfos")
    val userInfos: List<Any>
)

/**
 * GetProfileLikeData
 */
data class GetProfileLikeData(
    @field:JsonProperty("uid")
    val uid: String,
    @field:JsonProperty("time")
    val time: Long,
    @field:JsonProperty("favoriteInfo")
    val favoriteInfo: GetProfileLikeDataFavoriteInfo,
    @field:JsonProperty("voteInfo")
    val voteInfo: GetProfileLikeDataVoteInfo
)

/**
 * GetUnidirectionalFriendListDataItem
 */
data class GetUnidirectionalFriendListDataItem(
    @field:JsonProperty("uin")
    val uin: Long,
    @field:JsonProperty("uid")
    val uid: String,
    @field:JsonProperty("nick_name")
    val nickName: String,
    @field:JsonProperty("age")
    val age: Long,
    @field:JsonProperty("source")
    val source: String
)

/**
 * NapCatUserExtraExpand
 */
class NapCatUserExtraExpand

/**
 * 获取好友分组列表
 */
suspend fun NapCatBotApi.getFriendsWithCategory(): List<GetFriendsWithCategoryDataItem> {
    return apiRequest("get_friends_with_category")
}

/**
 * 获取点赞列表
 */
suspend fun NapCatBotApi.getProfileLike(userId: Long? = null, start: Long? = null, count: Long? = null): GetProfileLikeData {
    return apiRequest("get_profile_like", mapOf("user_id" to userId, "start" to start, "count" to count))
}

/**
 * 获取单向好友列表
 */
suspend fun NapCatBotApi.getUnidirectionalFriendList(): List<GetUnidirectionalFriendListDataItem> {
    return apiRequest("get_unidirectional_friend_list")
}

/**
 * 设置自定义在线状态
 */
suspend fun NapCatBotApi.setDiyOnlineStatus(faceId: Any, faceType: Any? = null, wording: String? = null) {
    apiRequestUnit("set_diy_online_status", mapOf("face_id" to faceId, "face_type" to faceType, "wording" to wording))
}
