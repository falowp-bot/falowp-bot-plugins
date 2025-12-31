package com.blr19c.falowp.bot.plugins.signin.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * 签到记录
 */
object SignInRecord : Table("sign_in_record") {
    val id = integer("id").autoIncrement()

    /**
     * 用户id
     */
    val userId = varchar("user_id", 64).index()

    /**
     * 签到时间
     */
    val signInDate = date("sign_in_date")

    override val primaryKey = PrimaryKey(id, name = "pk_sign_in_record_id")

    init {
        multiTransaction {
            uniqueIndex(userId, signInDate)
            SchemaUtils.create(SignInRecord)
        }
    }
}