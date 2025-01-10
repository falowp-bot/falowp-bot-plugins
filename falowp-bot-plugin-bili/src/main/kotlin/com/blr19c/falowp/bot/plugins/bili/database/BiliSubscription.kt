package com.blr19c.falowp.bot.plugins.bili.database

import com.blr19c.falowp.bot.plugins.bili.vo.BiliSubscriptionVo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

/**
 * b站订阅
 */
object BiliSubscription : Table("bili_subscription") {
    val id = integer("id").autoIncrement()

    /**
     * b站id
     */
    val mid = varchar("mid", 32).index()

    /**
     * 来源id
     */
    val sourceId = varchar("source_id", 128).index()

    /**
     * 来源类型
     */
    val sourceType = varchar("source_type", 16)

    /**
     * 动态列表
     */
    override val primaryKey = PrimaryKey(id, name = "pk_bili_subscription_id")

    init {
        multiTransaction {
            uniqueIndex(mid, sourceId)
            SchemaUtils.create(BiliSubscription)
        }
    }

    fun insert(mid: String, sourceId: String, sourceType: String) {
        multiTransaction {
            BiliSubscription.insert {
                it[BiliSubscription.mid] = mid
                it[BiliSubscription.sourceId] = sourceId
                it[BiliSubscription.sourceType] = sourceType
            }
        }
    }

    fun queryByMid(mid: String): List<BiliSubscriptionVo> {
        return multiTransaction {
            BiliSubscription.selectAll().where(BiliSubscription.mid eq mid).map {
                BiliSubscriptionVo(
                    it[BiliSubscription.id],
                    it[BiliSubscription.mid],
                    it[sourceId],
                    it[sourceType],
                )
            }.toList()
        }
    }

    fun queryBySourceId(sourceId: String): List<BiliSubscriptionVo> {
        return multiTransaction {
            BiliSubscription.selectAll().where(BiliSubscription.sourceId eq sourceId).map {
                BiliSubscriptionVo(
                    it[BiliSubscription.id],
                    it[mid],
                    it[BiliSubscription.sourceId],
                    it[sourceType],
                )
            }.toList()
        }
    }
}
