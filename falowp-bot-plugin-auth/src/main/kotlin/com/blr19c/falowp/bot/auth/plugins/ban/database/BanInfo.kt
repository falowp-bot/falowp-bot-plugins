package com.blr19c.falowp.bot.auth.plugins.ban.database

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 禁用用户
 */
object BanInfo : Table("ban_info") {
    val id = integer("id").autoIncrement()

    /**
     * 用户id
     */
    val userId = varchar("user_id", 64)

    /**
     * 来源id
     */
    val sourceId = varchar("source_id", 128)

    override val primaryKey = PrimaryKey(id, name = "pk_ban_info_id")

    init {
        transaction {
            uniqueIndex(userId, sourceId)
            SchemaUtils.create(BanInfo)
        }
    }
}