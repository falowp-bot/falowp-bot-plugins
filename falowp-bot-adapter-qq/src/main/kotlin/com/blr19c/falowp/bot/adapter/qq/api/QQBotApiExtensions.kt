package com.blr19c.falowp.bot.adapter.qq.api

import com.blr19c.falowp.bot.system.api.BotApi

/**
 * 使用当前消息上下文创建 QQ API
 * @return 保留当前上下文的 QQ API，已是 QQBotApi 时返回自身
 */
fun BotApi.qq(): QQBotApi = this as? QQBotApi ?: QQBotApi(receiveMessage, originalClass)

/**
 * 属性形式：botApi.qq.sendMarkdown(...)
 */
val BotApi.qq: QQBotApi get() = qq()
