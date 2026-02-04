package com.blr19c.falowp.bot.adapter.tg.api

import com.blr19c.falowp.bot.system.api.BotSelf
import org.telegram.telegrambots.meta.api.objects.User

/**
 * TG自身信息
 */
class TGSelf(val user: User) : BotSelf {

    override val id: String get() = user.id.toString()

}