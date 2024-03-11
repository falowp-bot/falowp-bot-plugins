package com.blr19c.falowp.bot.wordcloud.plugins.wordcloud.event

import com.blr19c.falowp.bot.system.plugin.Plugin
import java.time.LocalDate

/**
 * 发送词云事件
 *
 * @param date 日期
 */
data class WordcloudEvent(val date: LocalDate = LocalDate.now()) : Plugin.Listener.Event
