package com.blr19c.falowp.bot.signin.database

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
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
    val signinDate = date("signin_date")

    override val primaryKey = PrimaryKey(id, name = "pk_signin_record_id")

    init {
        transaction {
            uniqueIndex(userId, signinDate)
            SchemaUtils.create(SigninRecord)
        }
    }
}