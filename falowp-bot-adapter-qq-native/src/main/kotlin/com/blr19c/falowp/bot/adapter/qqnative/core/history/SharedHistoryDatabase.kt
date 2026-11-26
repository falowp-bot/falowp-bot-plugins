package com.blr19c.falowp.bot.adapter.qqnative.core.history

import com.blr19c.falowp.bot.plugins.db.databaseMap
import com.blr19c.falowp.bot.plugins.db.primaryDatabase
import org.jetbrains.exposed.v1.jdbc.Database
import com.blr19c.falowp.bot.plugins.db.Database as DatabasePlugin

/**
 * 仅在引入数据库模块时加载，复用其 Exposed 数据源
 */
internal object SharedHistoryDatabase {
    fun resolve(name: String): Database = DatabasePlugin.run {
        if (name.isBlank()) primaryDatabase()
        else checkNotNull(databaseMap()[name]) { "找不到 QQNative 消息记录数据源：$name" }
    }
}
