package com.blr19c.falowp.bot.adapter.nc

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.web.NapCatWebSocket
import com.blr19c.falowp.bot.system.adapter.AdapterApplication
import com.blr19c.falowp.bot.system.adapter.BotAdapter
import com.blr19c.falowp.bot.system.adapter.BotAdapterInterface
import com.blr19c.falowp.bot.system.adapter.BotAdapterRegister

/**
 * NapCat 协议适配器
 */
@BotAdapter(name = "NapCat")
class NapCatApplication : BotAdapterInterface {

    override suspend fun start(register: BotAdapterRegister) {
        AdapterApplication.botApiSupportRegister(NapCatBotApiSupport)
        NapCatWebSocket.configure {
            register.finish(this@NapCatApplication)
        }
    }
}
