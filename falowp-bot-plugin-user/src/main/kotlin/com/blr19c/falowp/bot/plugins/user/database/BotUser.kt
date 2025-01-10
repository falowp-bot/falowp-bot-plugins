package com.blr19c.falowp.bot.plugins.user.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table

/**
 * 机器人用户
 */
object BotUser : Table("bot_user") {
    val id = integer("id").autoIncrement()

    /**
     * 用户id
     */
    val userId = varchar("user_id", 64).index()

    /**
     * 昵称
     */
    val nickname = varchar("nickname", 128)

    /**
     * 头像url
     */
    val avatar = varchar("avatar", 256)

    /**
     * 权限
     */
    val auth = varchar("auth", 32)

    /**
     * 好感度
     */
    val impression = decimal("impression", 10, 2)

    /**
     * 金币 签到记录
     */
    val coins = decimal("coins", 10, 2)

    /**
     * 来源id
     */
    val sourceId = varchar("source_id", 128).index()

    /**
     * 来源类型
     */
    val sourceType = varchar("source_type", 16)

    override val primaryKey = PrimaryKey(id, name = "pk_bot_user_id")

    init {
        multiTransaction {
            uniqueIndex(userId, sourceId)
            SchemaUtils.create(BotUser)
        }
    }
}
