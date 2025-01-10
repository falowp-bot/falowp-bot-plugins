package com.blr19c.falowp.bot.plugins.wordcloud.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.plugins.wordcloud.vo.WordcloudTextInfoVo
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate

/**
 * 词云消息记录
 */
object WordcloudTextInfo : Table("word_cloud_text_info") {
    val id = integer("id").autoIncrement()

    /**
     * 发送的消息
     */
    val text = text("text")

    /**
     * 用户id
     */
    val userId = varchar("user_id", 64)

    /**
     * 来源id
     */
    val sourceId = varchar("source_id", 128).index()

    /**
     * 来源类型
     */
    val sourceType = varchar("source_type", 16)

    /**
     * 创建时间
     */
    val createDate = date("create_date").index()

    override val primaryKey = PrimaryKey(id, name = "pk_bot_user_id")

    init {
        multiTransaction {
            SchemaUtils.create(WordcloudTextInfo)
        }
    }

    fun queryAllSourceId(createDate: LocalDate): List<String> = multiTransaction {
        WordcloudTextInfo.select(sourceId)
            .where { WordcloudTextInfo.createDate eq createDate }
            .groupBy(sourceId)
            .distinctBy { it[sourceId] }
            .map { it[sourceId] }
            .toList()
    }

    fun queryBySourceId(sourceId: String, createDate: LocalDate): List<WordcloudTextInfoVo> = multiTransaction {
        WordcloudTextInfo.selectAll()
            .where { (WordcloudTextInfo.sourceId eq sourceId).and(WordcloudTextInfo.createDate eq createDate) }
            .map {
                WordcloudTextInfoVo(
                    it[WordcloudTextInfo.id],
                    it[text],
                    it[userId],
                    it[WordcloudTextInfo.sourceId],
                    it[sourceType]
                )
            }.toList()
    }
}
