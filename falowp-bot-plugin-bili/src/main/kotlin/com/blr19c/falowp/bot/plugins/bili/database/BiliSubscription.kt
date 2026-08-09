package com.blr19c.falowp.bot.plugins.bili.database

import com.blr19c.falowp.bot.plugins.bili.vo.BiliSubscriptionVo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * B 站订阅记录
 */
object BiliSubscription : Table("bili_subscription") {
    val id = integer("id").autoIncrement()

    /**
     * UP 主 UID
     */
    val mid = varchar("mid", 32).index()

    /**
     * 接收推送的好友或群 ID
     */
    val sourceId = varchar("source_id", 128).index()

    /**
     * 接收推送的会话类型
     */
    val sourceType = varchar("source_type", 16)

    /**
     * 表主键
     */
    override val primaryKey = PrimaryKey(id, name = "pk_bili_subscription_id")

    init {
        multiTransaction {
            uniqueIndex(mid, sourceId)
            SchemaUtils.create(BiliSubscription)
        }
    }

    /**
     * 添加一条订阅
     */
    fun insert(mid: String, sourceId: String, sourceType: String) {
        multiTransaction {
            BiliSubscription.insert {
                it[BiliSubscription.mid] = mid
                it[BiliSubscription.sourceId] = sourceId
                it[BiliSubscription.sourceType] = sourceType
            }
        }
    }

    /**
     * 查询一个 UP 主的全部订阅
     */
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

    /**
     * 查询一个会话里的全部订阅
     */
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
