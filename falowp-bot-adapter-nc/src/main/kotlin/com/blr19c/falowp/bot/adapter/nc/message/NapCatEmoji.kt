package com.blr19c.falowp.bot.adapter.nc.message

import com.blr19c.falowp.bot.system.api.ReceiveMessage

/**
 * NapCat 表情实现
 */
data class NapCatEmoji(
    override val id: String,
    override val type: String,
    override val display: String
) : ReceiveMessage.Emoji