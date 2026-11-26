package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * 私聊扩展数据模型
 */
class QQNativePrivateApiExpand {
    /**
     * 登录凭据
     */
    data class Cookies(
        /**
         * 登录凭据
         */
        val cookies: String,
        /**
         * 跨站请求伪造防护令牌（CSRF Token）
         */
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
        val nickname: String,
        /**
         * 备注
         */
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
        val age: Int?,
        /**
         * 手机号
         */
        @field:JsonProperty("phone_num")
        val phoneNum: String?,
        /**
         * 邮箱
         */
        val email: String?,
        /**
         * 性别
         */
        val sex: String,
        /**
         * 等级
         */
        val level: Int?
    )

    /**
     * 最近会话
     */
    data class RecentContactItemItem(
        /**
         * 最近一条消息
         */
        val lastestMsg: JsonNode?,
        /**
         * 对象QQ
         */
        val peerUin: String,
        /**
         * 备注
         */
        val remark: String,
        /**
         * 消息时间
         */
        val msgTime: String,
        /**
         * 聊天类型
         */
        val chatType: Long,
        /**
         * 消息ID
         */
        val msgId: String,
        /**
         * 发送者昵称
         */
        val sendNickName: String,
        /**
         * 发送者群名片
         */
        val sendMemberName: String,
        /**
         * 对象名称
         */
        val peerName: String
    )

    /**
     * 分组中的好友资料
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
        val nickname: String,
        /**
         * 备注
         */
        val remark: String?,
        /**
         * 性别
         */
        val sex: String?,
        /**
         * 等级
         */
        val level: Long?,
        /**
         * 年龄
         */
        val age: Long?,
        /**
         * 用户 QID
         */
        val qid: String?,
        /**
         * 登录天数
         */
        @field:JsonProperty("login_days")
        val loginDays: Long?,
        /**
         * 分组名称
         */
        val categoryName: String?,
    )

    /**
     * 好友分组
     */
    data class FriendsWithCategoryItemItem(
        /**
         * 分组ID
         */
        val categoryId: Long,
        /**
         * 分组名称
         */
        val categoryName: String,
        /**
         * 分组内好友数量
         */
        val categoryMbCount: Long,
        /**
         * 好友列表
         */
        val buddyList: List<FriendsWithCategoryItemItemBuddyListItem>
    )

    /**
     * 资料点赞记录
     */
    data class ProfileLikeFavoriteInfo(
        /**
         * 点赞用户信息
         */
        val userInfos: List<JsonNode>,
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
     * 资料获赞统计
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
        val userInfos: List<JsonNode>
    )

    /**
     * 资料点赞信息
     */
    data class ProfileLike(
        /**
         * 用户UID
         */
        val uid: String,
        /**
         * 时间
         */
        val time: String,
        /**
         * 点赞记录
         */
        val favoriteInfo: ProfileLikeFavoriteInfo,
        /**
         * 获赞统计
         */
        val voteInfo: ProfileLikeVoteInfo
    )

    /**
     * 单向好友
     */
    data class UnidirectionalFriendItem(
        /**
         * QQ号
         */
        val uin: Long,
        /**
         * 用户UID
         */
        val uid: String,
        /**
         * 昵称
         */
        @field:JsonProperty("nick_name")
        val nickName: String,
        /**
         * 年龄
         */
        val age: Long,
        /**
         * 来源
         */
        val source: String
    )

    /**
     * 单向好友单页结果，complete 为 false 且无游标时无法确认列表完整
     */
    data class UnidirectionalFriendPage(
        val friends: List<UnidirectionalFriendItem>,
        val nextCookie: String,
        val complete: Boolean,
        val raw: JsonNode
    )

}
