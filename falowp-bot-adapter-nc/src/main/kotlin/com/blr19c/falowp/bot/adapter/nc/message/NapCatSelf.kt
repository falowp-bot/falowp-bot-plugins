package com.blr19c.falowp.bot.adapter.nc.message

import com.blr19c.falowp.bot.adapter.nc.expand.NapCatSystemApiExpand
import com.blr19c.falowp.bot.system.api.BotSelf

data class NapCatSelf(val loginInfo: NapCatSystemApiExpand.LoginInfo) : BotSelf {

    override val id: String get() = loginInfo.userId
}