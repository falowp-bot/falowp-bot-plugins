@file:Suppress("unused")

package com.blr19c.falowp.bot.adapter.nc.message.expand

import com.blr19c.falowp.bot.system.plugin.message.MessageMatch

/**
 * 消息来源是NapCat
 */
fun MessageMatch.Build.napCat() = apply {
    this.appendCustom { receiveMessage ->
        receiveMessage.adapter.id == "NapCat"
    }
}

/**
 * 消息来源是NapCat
 */
fun MessageMatch.napCat() = this.copy(adapterId = "NapCat")