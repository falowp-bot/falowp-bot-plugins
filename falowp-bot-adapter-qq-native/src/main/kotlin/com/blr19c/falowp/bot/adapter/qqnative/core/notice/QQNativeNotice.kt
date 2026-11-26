package com.blr19c.falowp.bot.adapter.qqnative.core.notice

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessage

/**
 * 统一的 QQ 通知，包含会话、操作者、成员资料和事件内容
 */
data class QQNativeNotice(
    val conversation: QQNativeConversation, val actor: String, val detail: QQNativeNoticeDetail,
    val actorInfo: QQNativeNoticeUser? = null, val memberInfo: QQNativeNoticeUser? = null
)

/**
 * 通知关联用户的账号、昵称和权限信息
 */
data class QQNativeNoticeUser(val id: String, val nickname: String, val role: Int)

/**
 * 戳一戳、成员变动、申请及撤回等通知的具体内容
 */
sealed interface QQNativeNoticeDetail {
    data class Nudge(val member: String) : QQNativeNoticeDetail
    data class Join(val member: String, val action: String) : QQNativeNoticeDetail
    data class Leave(val member: String, val action: String) : QQNativeNoticeDetail
    data class FriendRequest(val comment: String, val flag: String) : QQNativeNoticeDetail
    data class GroupRequest(val comment: String, val flag: String, val action: String) : QQNativeNoticeDetail
    data class Withdraw(val original: QQNativeMessage) : QQNativeNoticeDetail

    /**
     * 群管理员变更
     */
    data class GroupAdmin(val member: String, val enabled: Boolean) : QQNativeNoticeDetail

    /**
     * 群禁言变更，成员为 0 时表示全员，duration 为秒，-1 表示全员禁言
     */
    data class GroupMute(val member: String, val duration: Long) : QQNativeNoticeDetail

    /**
     * 群名片变更
     */
    data class GroupCard(val member: String, val oldCard: String, val newCard: String) : QQNativeNoticeDetail

    /**
     * 群成员头衔变更
     */
    data class GroupTitle(val member: String, val title: String) : QQNativeNoticeDetail

    /**
     * 群精华消息变更，原文不可查时仍保留消息序列号
     */
    data class GroupEssence(
        val messageId: String,
        val sequence: String,
        val member: String,
        val added: Boolean,
        val message: QQNativeMessage? = null
    ) :
        QQNativeNoticeDetail

    /**
     * 群文件上传，fileId 是可用于群文件管理的原生标识
     */
    data class GroupFileUpload(
        val fileId: String,
        val name: String,
        val size: Long,
        val busId: Long,
        val messageId: String,
        val url: String? = null
    ) : QQNativeNoticeDetail

    /**
     * 群消息表情回应，原文不可查时仍保留消息序列号
     */
    data class MessageReaction(
        val messageId: String,
        val sequence: String,
        val reactions: List<QQNativeReaction>,
        val added: Boolean,
        val message: QQNativeMessage? = null
    ) : QQNativeNoticeDetail

    /**
     * 私聊输入状态
     */
    data class InputStatus(val status: Int, val text: String) : QQNativeNoticeDetail

    /**
     * 资料点赞，time 为服务端秒级时间戳
     */
    data class ProfileLike(val count: Int, val time: Long) : QQNativeNoticeDetail
}

/**
 * 单个表情回应的数量
 */
data class QQNativeReaction(val emojiId: String, val count: Int)
