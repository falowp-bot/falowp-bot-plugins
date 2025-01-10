package com.blr19c.falowp.bot.plugins.auth.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table

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
        multiTransaction {
            uniqueIndex(userId, sourceId)
            SchemaUtils.create(BanInfo)
        }
    }
}