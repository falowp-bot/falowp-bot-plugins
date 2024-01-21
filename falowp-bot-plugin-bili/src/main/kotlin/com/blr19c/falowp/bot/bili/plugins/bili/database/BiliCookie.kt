package com.blr19c.falowp.bot.bili.plugins.bili.database

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * b站登录的cookie
 */
object BiliCookie : Table("bili_cookie") {
    val id = integer("id").autoIncrement()

    /**
     * cookie
     */
    val cookie = text("cookie")
    override val primaryKey = PrimaryKey(id, name = "pk_bili_cookie_id")

    init {
        transaction {
            SchemaUtils.create(BiliCookie)
        }
    }
}
