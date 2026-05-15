package com.blr19c.falowp.bot.plugins.ai.database

import com.blr19c.falowp.bot.plugins.ai.vo.AiChatHistoryVo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * AI聊天上下文
 */
object AiChatHistory : Table("ai_chat_history") {
    val id = integer("id").autoIncrement()

    /**
     * 来源id
     */
    val sourceId = varchar("source_id", 128).index()

    /**
     * 用户id
     */
    val userId = varchar("user_id", 64).index()

    /**
     * 角色
     */
    val role = varchar("role", 16)

    /**
     * 消息内容
     */
    val content = text("content")

    /**
     * 创建时间
     */
    val createTime = long("create_time").index()

    override val primaryKey = PrimaryKey(id, name = "pk_ai_chat_history_id")

    init {
        multiTransaction {
            SchemaUtils.create(AiChatHistory)
        }
    }

    fun query(sourceId: String, userId: String, limit: Int): List<AiChatHistoryVo> = multiTransaction {
        if (limit <= 0) return@multiTransaction emptyList()
        AiChatHistory.selectAll()
            .where { (AiChatHistory.sourceId eq sourceId).and(AiChatHistory.userId eq userId) }
            .orderBy(AiChatHistory.id, SortOrder.DESC)
            .take(limit)
            .map {
                AiChatHistoryVo(
                    id = it[AiChatHistory.id],
                    sourceId = it[AiChatHistory.sourceId],
                    userId = it[AiChatHistory.userId],
                    role = it[role],
                    content = it[content],
                    createTime = it[createTime],
                )
            }
            .reversed()
    }

    fun append(sourceId: String, userId: String, role: String, content: String) = multiTransaction {
        AiChatHistory.insert {
            it[AiChatHistory.sourceId] = sourceId
            it[AiChatHistory.userId] = userId
            it[AiChatHistory.role] = role
            it[AiChatHistory.content] = content
            it[AiChatHistory.createTime] = System.currentTimeMillis()
        }
    }

    fun trim(sourceId: String, userId: String, maxRows: Int) = multiTransaction {
        if (maxRows < 0) return@multiTransaction
        val expiredIds = AiChatHistory.select(AiChatHistory.id)
            .where { (AiChatHistory.sourceId eq sourceId).and(AiChatHistory.userId eq userId) }
            .orderBy(AiChatHistory.id, SortOrder.DESC)
            .drop(maxRows)
            .map { it[AiChatHistory.id] }

        expiredIds.forEach { expiredId ->
            AiChatHistory.deleteWhere { AiChatHistory.id eq expiredId }
        }
    }
}
