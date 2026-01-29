@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * NapCatUserApiExpand
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
suspend fun NapCatBotApi.getFriendList(noCache: Boolean? = null): List<NapCatUserApiExpand.FriendUser> {
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
suspend fun NapCatBotApi.sendLike(userId: String, times: Long) {
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
suspend fun NapCatBotApi.setFriendRemark(userId: String, remark: String) {
    apiRequestUnit("set_friend_remark", mapOf("user_id" to userId, "remark" to remark))
}
