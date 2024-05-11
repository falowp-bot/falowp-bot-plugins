package com.blr19c.falowp.bot.plugins.db

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.plugin.PluginUtils
import com.blr19c.falowp.bot.system.pluginConfigProperty

@PluginUtils
object Database : Log {

    init {
        log().info("初始化Database")
        org.jetbrains.exposed.sql.Database.connect(
            url = pluginConfigProperty("url"),
            user = pluginConfigProperty("username"),
            password = pluginConfigProperty("password"),
            driver = pluginConfigProperty("driver"),
        )
        log().info("初始化Database完成")
    }

}