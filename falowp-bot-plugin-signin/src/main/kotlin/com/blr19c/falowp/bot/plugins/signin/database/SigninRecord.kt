package com.blr19c.falowp.bot.plugins.signin.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

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
        multiTransaction {
            uniqueIndex(userId, signinDate)
            SchemaUtils.create(SigninRecord)
        }
    }
}