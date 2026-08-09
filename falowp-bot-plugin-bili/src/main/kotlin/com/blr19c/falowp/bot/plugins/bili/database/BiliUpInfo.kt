package com.blr19c.falowp.bot.plugins.bili.database

import com.blr19c.falowp.bot.plugins.bili.vo.BiliUpInfoVo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/**
 * 订阅中的 UP 主信息
 */
object BiliUpInfo : Table("bili_up_info") {
    val id = integer("id").autoIncrement()

    /**
     * UP 主 UID
     */
    val mid = varchar("mid", 32).uniqueIndex()

    /**
     * 直播间 ID
     */
    val roomId = varchar("room_id", 32)

    /**
     * UP 主昵称
     */
    val name = varchar("name", 128)

    /**
     * 上次检查时是否开播
     */
    val liveStatus = bool("live_status")

    override val primaryKey = PrimaryKey(id, name = "pk_bili_up_info_id")

    init {
        multiTransaction {
            SchemaUtils.create(BiliUpInfo)
        }
    }

    /**
     * 查询全部 UP 主
     */
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

    /**
     * 按 UID 查询 UP 主
     */
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

    /**
     * 按直播状态查询 UP 主
     */
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

    /**
     * 更新 UP 主的直播状态
     */
    fun updateLiveStatus(mid: String, liveStatus: Boolean) {
        multiTransaction {
            BiliUpInfo.update({ BiliUpInfo.mid eq mid }) {
                it[BiliUpInfo.liveStatus] = liveStatus
            }
        }
    }

    /**
     * 保存一个新的 UP 主
     */
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
