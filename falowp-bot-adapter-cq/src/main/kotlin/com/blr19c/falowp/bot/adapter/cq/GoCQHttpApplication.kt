package com.blr19c.falowp.bot.adapter.cq

import com.blr19c.falowp.bot.adapter.cq.web.GoCQHttpWebSocket
import com.blr19c.falowp.bot.system.adapter.BotAdapter
import com.blr19c.falowp.bot.system.adapter.BotAdapterInterface
import com.blr19c.falowp.bot.system.adapter.BotAdapterRegister

@BotAdapter(name = "CQ")
class GoCQHttpApplication : BotAdapterInterface {

    override suspend fun start(register: BotAdapterRegister) {
        GoCQHttpWebSocket { register.finish(this@GoCQHttpApplication) }.configure()
    }
}