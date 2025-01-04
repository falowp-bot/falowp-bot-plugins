@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.adapter.tg.expand

import com.blr19c.falowp.bot.adapter.tg.TGApplication
import com.blr19c.falowp.bot.system.api.BotApi
import org.telegram.telegrambots.bots.TelegramLongPollingBot

/**
 * 获取tg原始bot
 */
fun BotApi.tgOriginalBot(): TelegramLongPollingBot {
    return TGApplication.telegramLongPollingBot()
}