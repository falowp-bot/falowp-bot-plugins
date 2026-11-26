package com.blr19c.falowp.bot.adapter.qqnative.adapter.notice

import com.blr19c.falowp.bot.adapter.qqnative.adapter.QQNativeBotApi
import com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event.*
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model.QQNativeReaction
import com.blr19c.falowp.bot.adapter.qqnative.adapter.message.*
import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeAccount
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.*
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.listener.events.*
import com.blr19c.falowp.bot.system.plugin.PluginBotApi
import kotlinx.coroutines.launch

internal fun nativeNotice(notice: QQNativeNotice, account: QQNativeAccount) {
    val source = NativeMessages.source(notice.conversation)
    fun user(id: String, info: QQNativeNoticeUser?) = NativeMessages.user(id, info?.nickname ?: id, info?.role ?: 0)
    val actor = user(notice.actor, notice.actorInfo)
    val event = when (val detail = notice.detail) {
        is QQNativeNoticeDetail.GroupAdmin -> QQNativeGroupAdminEvent(
            source,
            actor,
            user(detail.member, notice.memberInfo),
            detail.enabled
        )

        is QQNativeNoticeDetail.GroupMute -> QQNativeGroupMuteEvent(
            source,
            actor,
            user(detail.member, notice.memberInfo),
            detail.duration
        )

        is QQNativeNoticeDetail.GroupCard -> QQNativeGroupCardEvent(
            source,
            actor,
            user(detail.member, notice.memberInfo),
            detail.oldCard,
            detail.newCard
        )

        is QQNativeNoticeDetail.GroupTitle -> QQNativeGroupTitleEvent(
            source,
            actor,
            user(detail.member, notice.memberInfo),
            detail.title
        )

        is QQNativeNoticeDetail.GroupEssence -> QQNativeGroupEssenceEvent(
            source,
            actor,
            detail.messageId,
            detail.sequence,
            user(detail.member, notice.memberInfo),
            detail.added,
            detail.message?.let { NativeMessages.decode(it, account) }
        )

        is QQNativeNoticeDetail.GroupFileUpload -> QQNativeGroupFileUploadEvent(
            source,
            actor,
            detail.fileId,
            detail.name,
            detail.size,
            detail.busId,
            detail.messageId,
            detail.url
        )

        is QQNativeNoticeDetail.MessageReaction -> QQNativeMessageReactionEvent(
            source,
            actor,
            detail.messageId,
            detail.sequence,
            detail.reactions.map { QQNativeReaction(NativeMessages.emoji(it.emojiId), it.count) },
            detail.added,
            detail.message?.let { NativeMessages.decode(it, account) }
        )

        is QQNativeNoticeDetail.InputStatus -> QQNativeInputStatusEvent(source, actor, detail.status, detail.text)
        is QQNativeNoticeDetail.ProfileLike -> QQNativeProfileLikeEvent(source, actor, detail.count, detail.time)
        is QQNativeNoticeDetail.Nudge -> NudgeEvent(source, actor, user(detail.member, notice.memberInfo))
        is QQNativeNoticeDetail.Join -> GroupIncreaseEvent(
            source,
            actor,
            user(detail.member, notice.memberInfo),
            detail.action
        )

        is QQNativeNoticeDetail.Leave -> GroupDecreaseEvent(
            source,
            actor,
            user(detail.member, notice.memberInfo),
            detail.action
        )

        is QQNativeNoticeDetail.FriendRequest -> RequestAddFriendEvent(source, actor, detail.comment, detail.flag)
        is QQNativeNoticeDetail.GroupRequest -> RequestJoinGroupEvent(
            source,
            actor,
            detail.comment,
            detail.flag,
            detail.action
        )

        is QQNativeNoticeDetail.Withdraw -> WithdrawMessageEvent(
            source,
            actor,
            NativeMessages.decode(detail.original, account)
        )
    }
    val api = QQNativeBotApi(
        ReceiveMessage.empty().copy(
            source = source, sender = actor, self = NativeMessages.self(account),
            adapter = ReceiveMessage.Adapter("QQNative", notice)
        ), QQNativeBotApi::class, account
    )
    api.launch { PluginBotApi(api).publishEvent(event) }
}
