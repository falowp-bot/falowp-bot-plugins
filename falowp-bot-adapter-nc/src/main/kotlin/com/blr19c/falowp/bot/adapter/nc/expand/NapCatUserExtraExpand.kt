@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import tools.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatUserExtraExpand
 */
class NapCatUserExtraExpand {
    /**
     * FriendsWithCategoryItemItemBuddyListItem
     */
    data class FriendsWithCategoryItemItemBuddyListItem(
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
     * FriendsWithCategoryItemItem
     */
    data class FriendsWithCategoryItemItem(
        /**
         * 分组ID
         */
        @field:JsonProperty("categoryId")
        val categoryId: Long,
        /**
         * 分组名称
         */
        @field:JsonProperty("categoryName")
        val categoryName: String,
        /**
         * 分组内好友数量
         */
        @field:JsonProperty("categoryMbCount")
        val categoryMbCount: Long,
        /**
         * 好友列表
         */
        @field:JsonProperty("buddyList")
        val buddyList: List<FriendsWithCategoryItemItemBuddyListItem>
    )

    /**
     * ProfileLikeFavoriteInfo
     */
    data class ProfileLikeFavoriteInfo(
        /**
         * 点赞用户信息
         */
        @field:JsonProperty("userInfos")
        val userInfos: List<String>,
        /**
         * 总点赞数
         */
        @field:JsonProperty("total_count")
        val totalCount: Long,
        /**
         * 最后点赞时间
         */
        @field:JsonProperty("last_time")
        val lastTime: Long,
        /**
         * 今日点赞数
         */
        @field:JsonProperty("today_count")
        val todayCount: Long
    )

    /**
     * ProfileLikeVoteInfo
     */
    data class ProfileLikeVoteInfo(
        /**
         * 总点赞数
         */
        @field:JsonProperty("total_count")
        val totalCount: Long,
        /**
         * 新增点赞数
         */
        @field:JsonProperty("new_count")
        val newCount: Long,
        /**
         * 新增附近点赞数
         */
        @field:JsonProperty("new_nearby_count")
        val newNearbyCount: Long,
        /**
         * 最后访问时间
         */
        @field:JsonProperty("last_visit_time")
        val lastVisitTime: Long,
        /**
         * 点赞用户信息
         */
        @field:JsonProperty("userInfos")
        val userInfos: List<String>
    )

    /**
     * ProfileLike
     */
    data class ProfileLike(
        /**
         * 用户UID
         */
        @field:JsonProperty("uid")
        val uid: String,
        /**
         * 时间
         */
        @field:JsonProperty("time")
        val time: String,
        /**
         * favoriteInfo
         */
        @field:JsonProperty("favoriteInfo")
        val favoriteInfo: ProfileLikeFavoriteInfo,
        /**
         * voteInfo
         */
        @field:JsonProperty("voteInfo")
        val voteInfo: ProfileLikeVoteInfo
    )

    /**
     * UnidirectionalFriendItem
     */
    data class UnidirectionalFriendItem(
        /**
         * QQ号
         */
        @field:JsonProperty("uin")
        val uin: Long,
        /**
         * 用户UID
         */
        @field:JsonProperty("uid")
        val uid: String,
        /**
         * 昵称
         */
        @field:JsonProperty("nick_name")
        val nickName: String,
        /**
         * 年龄
         */
        @field:JsonProperty("age")
        val age: Long,
        /**
         * 来源
         */
        @field:JsonProperty("source")
        val source: String
    )

}

/**
 * 获取带分组的好友列表
 */
suspend fun NapCatBotApi.getFriendsWithCategory(): NapCatUserExtraExpand.List<FriendsWithCategoryItemItem> {
    return apiRequest("get_friends_with_category")
}

/**
 * 获取资料点赞
 */
suspend fun NapCatBotApi.getProfileLike(userId: String? = null, start: Long, count: Long): NapCatUserExtraExpand.ProfileLike {
    return apiRequest("get_profile_like", mapOf("user_id" to userId, "start" to start, "count" to count))
}

/**
 * 获取单向好友列表
 */
suspend fun NapCatBotApi.getUnidirectionalFriendList(): NapCatUserExtraExpand.List<UnidirectionalFriendItem> {
    return apiRequest("get_unidirectional_friend_list")
}

/**
 * 设置自定义在线状态
 *
 * 设置自定义在线状态
 */
suspend fun NapCatBotApi.setDiyOnlineStatus(faceId: Long, faceType: Long, wording: String) {
    apiRequestUnit("set_diy_online_status", mapOf("face_id" to faceId, "face_type" to faceType, "wording" to wording))
}
