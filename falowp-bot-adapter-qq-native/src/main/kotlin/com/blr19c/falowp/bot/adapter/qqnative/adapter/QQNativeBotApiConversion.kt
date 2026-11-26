package com.blr19c.falowp.bot.adapter.qqnative.adapter

import com.blr19c.falowp.bot.system.api.BotApi

/**
 * 转为 QQNativeBotApi，沿用当前消息及 SchedulingBotApiSupport 提供的账号上下文
 */
fun BotApi.qqNative(): QQNativeBotApi =
    this as? QQNativeBotApi ?: QQNativeBotApi(receiveMessage, originalClass)
