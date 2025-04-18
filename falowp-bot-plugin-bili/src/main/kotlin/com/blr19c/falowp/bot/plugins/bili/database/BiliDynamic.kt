package com.blr19c.falowp.bot.plugins.bili.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

/**
 * b站动态
 */
object BiliDynamic : Table("bili_dynamic") {
    val id = integer("id").autoIncrement()

    /**
     * b站id
     */
    val mid = varchar("mid", 32).index()

    /**
     * 动态id
     */
    val dynamic = varchar("dynamic", 64)

    override val primaryKey = PrimaryKey(id, name = "pk_bili_dynamic_id")

    init {
        multiTransaction {
            SchemaUtils.create(BiliDynamic)
        }
    }


    fun queryByMid(mid: String): List<String> {
        return multiTransaction {
            BiliDynamic.selectAll().where(BiliDynamic.mid eq mid).map { it[dynamic] }.toList()
        }
    }

    fun insert(mid: String, dynamic: String) {
        multiTransaction {
            BiliDynamic.insert {
                it[BiliDynamic.mid] = mid
                it[BiliDynamic.dynamic] = dynamic
            }
        }
    }
}