package com.blr19c.falowp.bot.plugins.wordcloud.database

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.plugins.wordcloud.vo.WordCloudTextInfoVo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate

/**
 * 词云消息记录
 */
object WordCloudTextInfo : Table("word_cloud_text_info") {
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
            SchemaUtils.create(WordCloudTextInfo)
        }
    }

    fun queryAllSourceId(createDate: LocalDate): List<String> = multiTransaction {
        WordCloudTextInfo.select(sourceId)
            .where { WordCloudTextInfo.createDate eq createDate }
            .groupBy(sourceId)
            .distinctBy { it[sourceId] }
            .map { it[sourceId] }
            .toList()
    }

    fun queryBySourceId(sourceId: String, createDate: LocalDate): List<WordCloudTextInfoVo> = multiTransaction {
        WordCloudTextInfo.selectAll()
            .where { (WordCloudTextInfo.sourceId eq sourceId).and(WordCloudTextInfo.createDate eq createDate) }
            .map {
                WordCloudTextInfoVo(
                    it[WordCloudTextInfo.id],
                    it[text],
                    it[userId],
                    it[WordCloudTextInfo.sourceId],
                    it[sourceType]
                )
            }.toList()
    }
}
