package com.blr19c.falowp.bot.adapter.tg.api

import com.blr19c.falowp.bot.adapter.tg.TGApplication
import com.blr19c.falowp.bot.system.api.BotSelf
import com.blr19c.falowp.bot.system.expand.ImageUrl
import org.telegram.telegrambots.meta.api.objects.User

/**
 * TG自身信息
 */
class TGSelf(val user: User) : BotSelf {

    override val id: String get() = user.id.toString()
    override val nickname: String
        get() = listOfNotNull(user.firstName, user.lastName).joinToString("")
            .ifBlank { user.userName?.takeIf { it.isNotBlank() } ?: id }

    override val avatar: ImageUrl get() = TGApplication.telegramLongPollingBot().getAvatar(user.id) ?: ImageUrl.empty()

}