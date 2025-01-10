package com.blr19c.falowp.bot.plugins.bili.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table

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
        multiTransaction {
            SchemaUtils.create(BiliCookie)
        }
    }
}
