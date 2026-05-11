package com.blr19c.falowp.bot.adapter.nc.message

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.expand.NapCatSystemApiExpand
import com.blr19c.falowp.bot.system.api.BotSelf
import com.blr19c.falowp.bot.system.expand.ImageUrl

data class NapCatSelf(val loginInfo: NapCatSystemApiExpand.LoginInfo) : BotSelf {

    override val id: String get() = loginInfo.userId
    override val nickname: String get() = loginInfo.nickname
    override val avatar: ImageUrl get() = NapCatBotApiSupport.avatar(loginInfo.userId)
}