package com.blr19c.falowp.bot.wordcloud.plugins.wordcloud.hook

import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 词云分词钩子
 *
 * @param segmentCountMap 分词内容
 */
data class WordcloudSegmentHook(val segmentCountMap: MutableMap<String, Int>) : Plugin.Listener.Hook
