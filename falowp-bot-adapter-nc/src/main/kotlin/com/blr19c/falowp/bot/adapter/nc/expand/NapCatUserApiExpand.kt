@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatUserApiExpand 用户API
 */
class NapCatUserApiExpand {
    /**
     * Cookies
     */
    data class Cookies(
        /**
         * Cookies
         */
        @field:JsonProperty("cookies")
        val cookies: String,
        /**
         * CSRF Token
         */
        @field:JsonProperty("bkn")
        val bkn: String
    )

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

    /**
     * RecentContactItemItem
     */
    data class RecentContactItemItem(
        /**
         * lastestMsg
         */
        @field:JsonProperty("lastestMsg")
        val lastestMsg: String,
        /**
         * 对象QQ
         */
        @field:JsonProperty("peerUin")
        val peerUin: String,
        /**
         * 备注
         */
        @field:JsonProperty("remark")
        val remark: String,
        /**
         * 消息时间
         */
        @field:JsonProperty("msgTime")
        val msgTime: String,
        /**
         * 聊天类型
         */
        @field:JsonProperty("chatType")
        val chatType: Long,
        /**
         * 消息ID
         */
        @field:JsonProperty("msgId")
        val msgId: String,
        /**
         * 发送者昵称
         */
        @field:JsonProperty("sendNickName")
        val sendNickName: String,
        /**
         * 发送者群名片
         */
        @field:JsonProperty("sendMemberName")
        val sendMemberName: String,
        /**
         * 对象名称
         */
        @field:JsonProperty("peerName")
        val peerName: String
    )

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
 * 获取 Cookies
 *
 * 获取指定域名的 Cookies
 *
 * @param domain 域名
 */
suspend fun NapCatBotApi.getCookies(domain: String): NapCatUserApiExpand.Cookies {
    return apiRequest("get_cookies", mapOf("domain" to domain))
}

/**
 * 获取好友列表
 *
 * 获取当前帐号的好友列表
 *
 * @param noCache 是否禁用缓存
 */
suspend fun NapCatBotApi.getFriendList(noCache: Boolean = true): List<NapCatUserApiExpand.FriendUser> {
    return apiRequest("get_friend_list", mapOf("no_cache" to noCache))
}

/**
 * 获取最近会话
 *
 * 获取最近会话
 *
 * @param count 获取数量
 */
suspend fun NapCatBotApi.getRecentContact(count: Long): List<NapCatUserApiExpand.RecentContactItemItem> {
    return apiRequest("get_recent_contact", mapOf("count" to count))
}

/**
 * 点赞
 *
 * 给指定用户点赞
 *
 * @param userId 用户ID
 * @param times 点赞次数
 */
suspend fun NapCatBotApi.sendLike(userId: String = this.receiveMessage.sender.id, times: Long) {
    apiRequestUnit("send_like", mapOf("user_id" to userId, "times" to times))
}

/**
 * 处理加好友请求
 *
 * 同意或拒绝加好友请求
 *
 * @param flag 请求标识
 * @param approve 是否同意
 * @param remark 备注
 */
suspend fun NapCatBotApi.setFriendAddRequest(flag: String, approve: String? = null, remark: String? = null) {
    apiRequestUnit("set_friend_add_request", mapOf("flag" to flag, "approve" to approve, "remark" to remark))
}

/**
 * 设置好友备注
 *
 * 设置好友备注
 *
 * @param userId 用户ID
 * @param remark 备注
 */
suspend fun NapCatBotApi.setFriendRemark(userId: String = this.receiveMessage.sender.id, remark: String) {
    apiRequestUnit("set_friend_remark", mapOf("user_id" to userId, "remark" to remark))
}

/**
 * 获取带分组的好友列表
 */
suspend fun NapCatBotApi.getFriendsWithCategory(): List<NapCatUserApiExpand.FriendsWithCategoryItemItem> {
    return apiRequest("get_friends_with_category")
}

/**
 * 获取资料点赞
 *
 * @param userId 用户ID
 * @param start 起始位置
 * @param count 获取数量
 */
suspend fun NapCatBotApi.getProfileLike(
    userId: String? = null,
    start: Long,
    count: Long
): NapCatUserApiExpand.ProfileLike {
    return apiRequest("get_profile_like", mapOf("user_id" to userId, "start" to start, "count" to count))
}

/**
 * 获取单向好友列表
 */
suspend fun NapCatBotApi.getUnidirectionalFriendList(): List<NapCatUserApiExpand.UnidirectionalFriendItem> {
    return apiRequest("get_unidirectional_friend_list")
}

/**
 * 设置自定义在线状态
 *
 * 设置自定义在线状态
 *
 * @param faceId 表情ID
 * @param faceType 表情类型
 * @param wording 状态文案
 */
suspend fun NapCatBotApi.setDiyOnlineStatus(faceId: Long, faceType: Long, wording: String) {
    apiRequestUnit("set_diy_online_status", mapOf("face_id" to faceId, "face_type" to faceType, "wording" to wording))
}

/**
 * 设置QQ头像
 *
 * 修改当前账号的QQ头像
 *
 * @param file 头像文件
 */
suspend fun NapCatBotApi.setQQAvatar(file: String) {
    apiRequestUnit("set_qq_avatar", mapOf("file" to file))
}

/**
 * 设置个性签名
 *
 * 修改当前登录账号的个性签名
 *
 * @param longNick 个性签名内容
 */
suspend fun NapCatBotApi.setSelfLongNick(longNick: String) {
    apiRequestUnit("set_self_longnick", mapOf("longNick" to longNick))
}
