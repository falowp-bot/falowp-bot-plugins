package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model

import com.blr19c.falowp.bot.system.api.ReceiveMessage

/**
 * 本次通知涉及的表情及数量，不表示消息的全部回应
 */
data class QQNativeReaction(val emoji: ReceiveMessage.Emoji, val count: Int)
