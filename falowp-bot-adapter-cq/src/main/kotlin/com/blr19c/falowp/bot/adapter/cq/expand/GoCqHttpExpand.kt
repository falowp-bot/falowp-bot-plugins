package com.blr19c.falowp.bot.adapter.cq.expand

import com.blr19c.falowp.bot.adapter.cq.event.RequestJoinGroupEvent
import com.blr19c.falowp.bot.system.adapterConfigProperty
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.request.*
import kotlin.time.Duration

/**
 * 设置群名称
 * @param groupName 群名称
 * @param groupId 群id
 */
suspend fun BotApi.setGroupName(groupName: String, groupId: String = this.receiveMessage.source.id) {
    val url = adapterConfigProperty("cq.apiUrl")
    webclient().post("$url/set_group_name") {
        setBody(
            mapOf(
                "group_id" to groupId,
                "group_name" to groupName,
            )
        )
    }
}

/**
 * 设置群成员备注
 * @param card 备注
 * @param userId 用户id
 * @param groupId 群id
 */
suspend fun BotApi.setGroupCard(
    card: String,
    userId: String = this.receiveMessage.sender.id,
    groupId: String = this.receiveMessage.source.id
) {
    val url = adapterConfigProperty("cq.apiUrl")
    webclient().post("$url/set_group_card") {
        setBody(
            mapOf(
                "group_id" to groupId,
                "user_id" to userId,
                "card" to card,
            )
        )
    }
}

/**
 * 设置全员群禁言
 * @param enable 启用或者关闭
 * @param groupId 群id
 */
suspend fun BotApi.setGroupWholeBan(
    enable: Boolean = true,
    groupId: String = this.receiveMessage.source.id,
) {
    val url = adapterConfigProperty("cq.apiUrl")
    webclient().post("$url/set_group_whole_ban") {
        setBody(
            mapOf(
                "group_id" to groupId,
                "enable" to enable,
            )
        )
    }
}

/**
 * 设置群禁言
 * @param duration 禁言时间
 * @param userId 用户id
 * @param groupId 群id
 */
suspend fun BotApi.setGroupBan(
    duration: Duration,
    userId: String = this.receiveMessage.sender.id,
    groupId: String = this.receiveMessage.source.id
) {
    val url = adapterConfigProperty("cq.apiUrl")
    webclient().post("$url/set_group_ban") {
        setBody(
            mapOf(
                "group_id" to groupId,
                "user_id" to userId,
                "duration" to duration.inWholeSeconds,
            )
        )
    }
}

/**
 * 群组踢人
 * @param userId 用户id
 * @param groupId 群组id
 * @param reject 是否以后拒绝申请
 */
suspend fun BotApi.setGroupKick(
    userId: String = this.receiveMessage.sender.id,
    groupId: String = this.receiveMessage.source.id,
    reject: Boolean = false
) {
    val url = adapterConfigProperty("cq.apiUrl")
    webclient().post("$url/set_group_kick") {
        setBody(
            mapOf(
                "group_id" to groupId,
                "user_id" to userId,
                "reject_add_request" to reject,
            )
        )
    }
}

/**
 * 处理加群请求/邀请
 * @param event 加群事件
 * @param approve 是否同意
 * @param reason 如果拒绝,拒绝原因
 */
suspend fun BotApi.setGroupAddRequest(event: RequestJoinGroupEvent, approve: Boolean, reason: String = "") {
    val url = adapterConfigProperty("cq.apiUrl")
    webclient().post("$url/set_group_add_request") {
        setBody(
            mapOf(
                "flag" to event.flag,
                "sub_type" to event.subType,
                "approve" to approve,
                "reason" to reason
            )
        )
    }
}