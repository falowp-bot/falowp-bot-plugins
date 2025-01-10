package com.blr19c.falowp.bot.plugins.bili.database

import com.blr19c.falowp.bot.plugins.bili.vo.BiliUpInfoVo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.sql.*

/**
 * b站up主信息
 */
object BiliUpInfo : Table("bili_up_info") {
    val id = integer("id").autoIncrement()

    /**
     * b站id
     */
    val mid = varchar("mid", 32).uniqueIndex()

    /**
     * 直播id
     */
    val roomId = varchar("room_id", 32)

    /**
     * up名称
     */
    val name = varchar("name", 128)

    /**
     * 直播状态
     */
    val liveStatus = bool("live_status")

    override val primaryKey = PrimaryKey(id, name = "pk_bili_up_info_id")

    init {
        multiTransaction {
            SchemaUtils.create(BiliUpInfo)
        }
    }

    fun queryAll(): List<BiliUpInfoVo> {
        return multiTransaction {
            BiliUpInfo.selectAll().map {
                BiliUpInfoVo(
                    it[BiliUpInfo.id],
                    it[mid],
                    it[roomId],
                    it[name],
                    it[liveStatus]
                )
            }.toList()
        }
    }

    fun queryByMid(mid: String): BiliUpInfoVo? {
        return multiTransaction {
            BiliUpInfo.selectAll().where { BiliUpInfo.mid eq mid }.map {
                BiliUpInfoVo(
                    it[BiliUpInfo.id],
                    it[BiliUpInfo.mid],
                    it[roomId],
                    it[name],
                    it[liveStatus]
                )
            }.firstOrNull()
        }
    }

    fun queryByLiveStatus(liveStatus: Boolean): List<BiliUpInfoVo> {
        return multiTransaction {
            BiliUpInfo.selectAll().where { BiliUpInfo.liveStatus eq liveStatus }.map {
                BiliUpInfoVo(
                    it[BiliUpInfo.id],
                    it[mid],
                    it[roomId],
                    it[name],
                    it[BiliUpInfo.liveStatus]
                )
            }.toList()
        }
    }

    fun updateLiveStatus(mid: String, liveStatus: Boolean) {
        multiTransaction {
            BiliUpInfo.update({ BiliUpInfo.mid eq mid }) {
                it[BiliUpInfo.liveStatus] = liveStatus
            }
        }
    }

    fun insert(mid: String, roomId: String, name: String) {
        multiTransaction {
            BiliUpInfo.insert {
                it[BiliUpInfo.mid] = mid
                it[BiliUpInfo.roomId] = roomId
                it[BiliUpInfo.name] = name
                it[liveStatus] = false
            }
        }
    }
}