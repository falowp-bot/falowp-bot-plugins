package com.blr19c.falowp.bot.adapter.tg.database

import com.blr19c.falowp.bot.adapter.tg.api.TGBotApiSupport
import com.blr19c.falowp.bot.adapter.tg.vo.TGUserInfoVo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.sql.*
import org.telegram.telegrambots.meta.api.objects.Message

/**
 * TG用户信息缓存
 */
object TGUserInfo : Table("tg_user_info") {

    private val id = integer("id").autoIncrement()

    /**
     * 用户id
     */
    private val userId = long("user_id").index()

    /**
     * 用户名
     */
    private val userName = varchar("user_name", 256).index()

    /**
     * 来源id
     */
    private val sourceId = long("source_id").index()

    /**
     * 来源类型
     */
    private val sourceType = varchar("source_type", 64)

    override val primaryKey = PrimaryKey(id, name = "pk_tg_user_info_id")

    init {
        multiTransaction {
            SchemaUtils.create(TGUserInfo)
        }
    }

    fun queryAllGroup(): List<Long> = multiTransaction {
        TGUserInfo.select(sourceId)
            .where { sourceType inList TGBotApiSupport.groupTypeList }
            .map { it[sourceId] }
            .toList()
    }

    fun queryByUserName(userName: String, message: Message? = null): TGUserInfoVo? = multiTransaction {
        val select = TGUserInfo.selectAll().where { TGUserInfo.userName eq userName }
        message?.let { select.andWhere { sourceId eq it.chatId } }
        select.map { toVo(it) }.firstOrNull()
    }

    fun queryByUserId(userId: Long, message: Message? = null): TGUserInfoVo? = multiTransaction {
        val select = TGUserInfo.selectAll().where { TGUserInfo.userId eq userId }
        message?.let { select.andWhere { sourceId eq it.chatId } }
        select.map { toVo(it) }.firstOrNull()
    }

    fun queryBySourceId(sourceId: Long): List<TGUserInfoVo> = multiTransaction {
        TGUserInfo.selectAll()
            .where { TGUserInfo.sourceId eq sourceId }
            .map { toVo(it) }
            .toList()
    }

    fun saveOrUpdate(message: Message) = multiTransaction {
        val current = queryByUserId(message.from.id, message)
        if (current == null) {
            TGUserInfo.insert {
                it[userId] = message.from.id
                it[userName] = message.from.userName
                it[sourceId] = message.chatId
                it[sourceType] = message.chat.type
            }
            return@multiTransaction
        }
        TGUserInfo.update({ TGUserInfo.id eq current.id }) {
            it[userName] = message.from.userName
        }
    }

    private fun toVo(it: ResultRow): TGUserInfoVo {
        return TGUserInfoVo(it[id], it[userId], it[userName], it[sourceId], it[sourceType])
    }
}