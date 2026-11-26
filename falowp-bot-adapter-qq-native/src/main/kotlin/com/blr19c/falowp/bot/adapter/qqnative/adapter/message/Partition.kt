package com.blr19c.falowp.bot.adapter.qqnative.adapter.message

import com.blr19c.falowp.bot.system.api.SendMessage

internal data object MessagePartition : SendMessage

/**
 * 在同一条消息链中开始下一条记录
 */
fun SendMessage.Builder.partition() = apply { messageList.add(MessagePartition) }
