@file:Suppress("unused")

package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand

import com.blr19c.falowp.bot.adapter.qqnative.adapter.QQNativeBotApi
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativeOnlineStatus
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativePrivateApiExpand

/**
 * 获取 Cookies
 *
 * 获取指定域名的 Cookies
 *
 * @param domain 域名
 */
suspend fun QQNativeBotApi.getCookies(domain: String): QQNativePrivateApiExpand.Cookies {
    return expandRequest("get_cookies", mapOf("domain" to domain))
}

/**
 * 获取好友列表
 *
 * @param noCache 是否禁用缓存
 */
suspend fun QQNativeBotApi.getFriendList(noCache: Boolean = true): List<QQNativePrivateApiExpand.FriendUser> {
    return expandRequest("get_friend_list", mapOf("no_cache" to noCache))
}

/**
 * 获取最近会话
 *
 * @param count 获取数量
 */
suspend fun QQNativeBotApi.getRecentContact(count: Long): List<QQNativePrivateApiExpand.RecentContactItemItem> {
    return expandRequest("get_recent_contact", mapOf("count" to count))
}

/**
 * 点赞
 *
 * @param userId 用户ID
 * @param times 点赞次数
 */
suspend fun QQNativeBotApi.sendLike(userId: String = this.receiveMessage.sender.id, times: Long) {
    expandUnit("send_like", mapOf("user_id" to userId, "times" to times))
}

/**
 * 处理加好友请求
 *
 * @param flag 请求标识
 * @param approve 是否同意
 * @param remark 备注
 */
suspend fun QQNativeBotApi.setFriendAddRequest(flag: String, approve: Boolean, remark: String? = null) {
    expandUnit("set_friend_add_request", mapOf("flag" to flag, "approve" to approve, "remark" to remark))
}

/**
 * 设置好友备注
 *
 * @param userId 用户ID
 * @param remark 备注
 */
suspend fun QQNativeBotApi.setFriendRemark(userId: String = this.receiveMessage.sender.id, remark: String) {
    expandUnit("set_friend_remark", mapOf("user_id" to userId, "remark" to remark))
}

/**
 * 获取带分组的好友列表
 */
suspend fun QQNativeBotApi.getFriendsWithCategory(): List<QQNativePrivateApiExpand.FriendsWithCategoryItemItem> {
    return expandRequest("get_friends_with_category")
}

/**
 * 获取资料点赞
 *
 * @param userId 用户ID
 * @param start 起始位置
 * @param count 获取数量
 */
suspend fun QQNativeBotApi.getProfileLike(
    userId: String? = null,
    start: Long,
    count: Long
): QQNativePrivateApiExpand.ProfileLike {
    return expandRequest("get_profile_like", mapOf("user_id" to userId, "start" to start, "count" to count))
}

/**
 * 获取单向好友列表
 */
suspend fun QQNativeBotApi.getUnidirectionalFriendList(): List<QQNativePrivateApiExpand.UnidirectionalFriendItem> {
    return expandRequest("get_unidirectional_friend_list")
}

/**
 * 获取单向好友的一页，nextCookie 原样传入下一次调用
 */
suspend fun QQNativeBotApi.getUnidirectionalFriendPage(
    cookie: String = "", count: Int = 99
): QQNativePrivateApiExpand.UnidirectionalFriendPage =
    expandRequest("get_unidirectional_friend_page", mapOf("cookie" to cookie, "count" to count))

/**
 * 设置在线状态
 *
 * @param onlineStatus 在线状态
 * @param battery 电池电量
 */
suspend fun QQNativeBotApi.setOnlineStatus(onlineStatus: QQNativeOnlineStatus, battery: Int = 0) {
    expandUnit(
        "set_online_status",
        mapOf(
            "status" to onlineStatus.status,
            "ext_status" to onlineStatus.extStatus,
            "battery_status" to battery
        )
    )
}

/**
 * 设置自定义在线状态
 *
 * @param faceId 表情 ID
 * @param faceType 表情类型
 * @param wording 状态文案
 */
suspend fun QQNativeBotApi.setDiyOnlineStatus(faceId: String, faceType: String = "1", wording: String) {
    expandUnit(
        "set_diy_online_status",
        mapOf("face_id" to faceId, "face_type" to faceType, "wording" to wording)
    )
}

/**
 * 设置QQ头像
 *
 * @param file 头像文件
 */
suspend fun QQNativeBotApi.setQQAvatar(file: String) {
    expandUnit("set_qq_avatar", mapOf("file" to file))
}

/**
 * 设置个性签名
 *
 * @param longNick 个性签名内容
 */
suspend fun QQNativeBotApi.setSelfLongNick(longNick: String) {
    expandUnit("set_self_longnick", mapOf("longNick" to longNick))
}
