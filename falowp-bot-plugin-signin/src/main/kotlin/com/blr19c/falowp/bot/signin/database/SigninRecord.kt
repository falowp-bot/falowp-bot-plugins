package com.blr19c.falowp.bot.signin.database

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 签到记录
 */
object SigninRecord : Table("signin_record") {
    val id = integer("id").autoIncrement()

    /**
     * 用户id
     */
    val userId = varchar("user_id", 64).index()

    /**
     * 签到时间
     */
    val signinDate = varchar("signin_date", 10)

    /**
     * 创建时间
     */
    val createTime = datetime("event_date_time")

    override val primaryKey = PrimaryKey(id, name = "pk_signin_record_id")

    init {
        transaction {
            uniqueIndex(userId, signinDate)
            SchemaUtils.create(SigninRecord)
        }
    }
}