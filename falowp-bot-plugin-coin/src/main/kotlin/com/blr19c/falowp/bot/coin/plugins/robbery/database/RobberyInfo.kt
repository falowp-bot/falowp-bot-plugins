package com.blr19c.falowp.bot.coin.plugins.robbery.database

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 打劫记录
 */
object RobberyInfo : Table("robbery_info") {
    val id = integer("id").autoIncrement()

    /**
     * 用户id
     */
    val userId = varchar("user_id", 64).index()

    /**
     * 被打劫的用户id
     */
    val robbedUserId = varchar("robbed_user_id", 64).index()


    /**
     * 打劫日期
     */
    val createDate = date("event_date_time")


    override val primaryKey = PrimaryKey(id, name = "pk_robbery_info_id")

    init {
        transaction {
            uniqueIndex(userId, robbedUserId, createDate)
            SchemaUtils.create(RobberyInfo)
        }
    }
}