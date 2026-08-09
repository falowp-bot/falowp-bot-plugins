package com.blr19c.falowp.bot.plugins.bili.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * 已经推送过的 B 站动态
 */
object BiliDynamic : Table("bili_dynamic") {
    val id = integer("id").autoIncrement()

    /**
     * UP 主 UID
     */
    val mid = varchar("mid", 32).index()

    /**
     * 动态 ID
     */
    val dynamic = varchar("dynamic", 64)

    override val primaryKey = PrimaryKey(id, name = "pk_bili_dynamic_id")

    init {
        multiTransaction {
            SchemaUtils.create(BiliDynamic)
        }
    }


    /**
     * 查询一个 UP 主已经推送过的动态
     */
    fun queryByMid(mid: String): List<String> {
        return multiTransaction {
            BiliDynamic.selectAll().where(BiliDynamic.mid eq mid).map { it[dynamic] }.toList()
        }
    }

    /**
     * 记下一条已经推送的动态
     */
    fun insert(mid: String, dynamic: String) {
        multiTransaction {
            BiliDynamic.insert {
                it[BiliDynamic.mid] = mid
                it[BiliDynamic.dynamic] = dynamic
            }
        }
    }
}
