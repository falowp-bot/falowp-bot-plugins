package com.blr19c.falowp.bot.plugins.bili.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * B 站登录 Cookie
 */
object BiliCookie : Table("bili_cookie") {
    val id = integer("id").autoIncrement()

    /**
     * 序列化后的 Cookie
     */
    val cookie = text("cookie")
    override val primaryKey = PrimaryKey(id, name = "pk_bili_cookie_id")

    init {
        multiTransaction {
            SchemaUtils.create(BiliCookie)
        }
    }
}
