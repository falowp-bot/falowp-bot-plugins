package com.blr19c.falowp.bot.plugins.image

import com.blr19c.falowp.bot.system.plugin.PluginUtils
import com.blr19c.falowp.bot.system.pluginConfigProperty

/**
 * 图像处理配置
 */
@PluginUtils
internal object ImageConfig {

    val requireNative = pluginConfigProperty("requireNative").toBooleanStrict()
}
