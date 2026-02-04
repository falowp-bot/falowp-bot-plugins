package com.blr19c.falowp.bot.plugins.wordcloud.event

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin
import java.time.LocalDate

/**
 * 发送词云事件
 *
 * @param date 日期
 */
data class WordCloudEvent(val date: LocalDate = LocalDate.now()) : Plugin.Listener.Event {
    override val source = ReceiveMessage.Source.system()
    override val actor = ReceiveMessage.User.empty()
}
