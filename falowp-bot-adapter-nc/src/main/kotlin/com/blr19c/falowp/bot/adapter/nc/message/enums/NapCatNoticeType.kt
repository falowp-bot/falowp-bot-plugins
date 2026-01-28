package com.blr19c.falowp.bot.adapter.nc.message.enums

import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.handlers.*
import com.blr19c.falowp.bot.system.json.safeString
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import tools.jackson.databind.JsonNode
import kotlin.reflect.KClass

/**
 * 通知类型
 */
enum class NapCatNoticeType(
    @get:JsonValue val value: String,
    val clazz: KClass<out NapCatNotice.NapCatNoticeInterface>?
) {

    /**
     * 好友添加-这个事件在 NapCatRequestType 中处理
     */
    FRIEND_ADD("friend_add", null),

    /**
     * 私聊消息撤回
     */
    FRIEND_RECALL("friend_recall", NapCatFriendRecallNotice::class),

    /**
     * 接收到离线文件-这个事件不支持处理
     */
    OFFLINE_FILE("offline_file", null),

    /**
     * 其他客户端在线状态变更-这个事件不支持处理
     */
    CLIENT_STATUS("client_status", null),

    /**
     * 群聊管理员变动
     */
    GROUP_ADMIN("group_admin", NapCatGroupAdminNotice::class),

    /**
     * 群聊禁言
     */
    GROUP_BAN("group_ban", NapCatGroupBanNotice::class),

    /**
     * 群成员名片更新
     */
    GROUP_CARD("group_card", NapCatGroupCardNotice::class),

    /**
     * 群聊成员减少
     */
    GROUP_DECREASE("group_decrease", NapCatGroupDecreaseNotice::class),

    /**
     * 群聊成员增加
     */
    GROUP_INCREASE("group_increase", NapCatGroupIncreaseNotice::class),

    /**
     * 群聊消息撤回
     */
    GROUP_RECALL("group_recall", NapCatGroupRecallNotice::class),

    /**
     * 群聊文件上传
     */
    GROUP_UPLOAD("group_upload", NapCatGroupUploadNotice::class),

    /**
     * 群聊表情回应
     */
    GROUP_MSG_EMOJI_LIKE("group_msg_emoji_like", NapCatGroupMsgEmojiLikeNotice::class),

    /**
     * 群聊设精
     */
    ESSENCE("essence", NapCatGroupEssenceNotice::class),

    /**
     * 戳一戳
     */
    NOTIFY_POKE("notify.poke", NapCatPokeNotice::class),

    /**
     * 输入状态更新
     */
    NOTIFY_INPUT_STATUS("notify.input_status", NapCatNotifyInputStatusNotice::class),

    /**
     * 群成员头衔变更
     */
    NOTIFY_TITLE("notify.title", NapCatGroupNotifyTitleNotice::class),

    /**
     * 点赞
     */
    NOTIFY_PROFILE_LIKE("notify.profile_like", NapCatNotifyProfileLikeNotice::class),

    /**
     * 未知
     */
    UNKNOWN("unknown", null);

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): NapCatNoticeType {
            return entries.firstOrNull { it.value == value } ?: UNKNOWN
        }

        fun fromValue(originalMessage: JsonNode): NapCatNoticeType {
            val noticeType = originalMessage.path("notice_type").safeString()
            val subType = originalMessage.path("sub_type").safeString()

            return if (subType.isBlank() || noticeType != "notify") fromValue(noticeType)
            else fromValue("${noticeType}.${subType}")
        }

    }
}
