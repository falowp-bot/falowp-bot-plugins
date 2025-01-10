package com.blr19c.falowp.bot.plugins.db

import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.plugin.PluginUtils
import com.blr19c.falowp.bot.system.pluginConfigList
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

@PluginUtils
object Database : Log {

    init {
        log().info("初始化Database")
        pluginConfigList("").flatMap { it.toMap().entries }.forEach { (name, dbConfig) ->
            dbConfig as Map<*, *>
            val db = org.jetbrains.exposed.sql.Database.connect(
                url = dbConfig["url"].toString(),
                user = dbConfig["username"].toString(),
                password = dbConfig["password"].toString(),
                driver = dbConfig["driver"].toString()
            )
            factory[name] = db
            if (dbConfig["primary"].toString().toBoolean()) {
                primary = db
            }
        }
        if (!::primary.isInitialized && factory.size == 1) {
            factory.firstNotNullOf { primary = it.value }
        }
        if (!::primary.isInitialized) {
            throw IllegalStateException("需要指定一个数据源或者主要数据源")
        }
        log().info("已加载的数据源:${factory.keys},主要数据源:${factory.filter { it.value == primary }.keys}")
        log().info("初始化Database完成")
    }
}

private lateinit var primary: org.jetbrains.exposed.sql.Database

private val factory: MutableMap<String, org.jetbrains.exposed.sql.Database> = mutableMapOf()

/**
 * 多数据源
 */
fun <T> multiTransaction(name: String? = null, statement: Transaction.() -> T): T {
    return transaction(name?.let { databaseMap()[it] } ?: primaryDatabase()) { statement() }
}

/**
 * 主要数据源
 */
fun primaryDatabase(): org.jetbrains.exposed.sql.Database {
    return primary
}

/**
 * 数据源map
 */
fun databaseMap(): Map<String, org.jetbrains.exposed.sql.Database> {
    return factory
}