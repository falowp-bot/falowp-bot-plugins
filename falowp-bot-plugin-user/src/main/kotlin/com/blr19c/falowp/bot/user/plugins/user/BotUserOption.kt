package com.blr19c.falowp.bot.user.plugins.user

import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.expand.ImageUrl
import com.blr19c.falowp.bot.user.database.BotUser
import com.blr19c.falowp.bot.user.vo.BotUserVo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

/**
 * 当前用户
 */
fun BotApi.currentUser(): BotUserVo {
    return currentUserOrNull()!!
}

/**
 * 当前用户
 */
fun BotApi.currentUserOrNull(): BotUserVo? {
    return queryByUserId(receiveMessage.sender.id, receiveMessage.source.id)
}

/**
 * 根据userId查询用户
 */
fun queryByUserId(userId: String, sourceId: String): BotUserVo? {
    return transaction {
        BotUser.selectAll()
            .where {
                val user = BotUser.userId eq userId
                val source = BotUser.sourceId eq sourceId
                user and source
            }
            .singleOrNull()
            ?.let { convertVo(it) }
    }
}

/**
 * 根据来源id获取用户
 */
fun queryBySourceId(sourceId: String, block: (Query) -> List<ResultRow> = { it.toList() }): List<BotUserVo> {
    return transaction {
        BotUser.selectAll()
            .where { BotUser.sourceId eq sourceId }
            .let { block.invoke(it) }
            .map { convertVo(it) }
    }
}


/**
 * 增加金币
 */
fun BotUserVo.incrementCoins(coins: BigDecimal) {
    transaction {
        BotUser.update({ BotUser.id eq this@incrementCoins.id }) {
            it.update(BotUser.coins, BotUser.coins + coins)
        }
    }
}

/**
 * 减少金币
 */
fun BotUserVo.decrementCoins(coins: BigDecimal) {
    incrementCoins(-coins)
}

/**
 * 增加好感度
 */
fun BotUserVo.incrementImpression(impression: BigDecimal) {
    transaction {
        BotUser.update({ BotUser.id eq this@incrementImpression.id }) {
            it.update(BotUser.impression, BotUser.impression + impression)
        }
    }
}

/**
 * 减少好感度
 */
fun BotUserVo.decrementImpression(impression: BigDecimal) {
    incrementImpression(-impression)
}


private fun convertVo(resultRow: ResultRow): BotUserVo {
    return BotUserVo(
        resultRow[BotUser.id],
        resultRow[BotUser.userId],
        resultRow[BotUser.nickname],
        ImageUrl(resultRow[BotUser.avatar]),
        ApiAuth.valueOf(resultRow[BotUser.auth]),
        resultRow[BotUser.impression],
        resultRow[BotUser.coins],
        resultRow[BotUser.sourceId],
        resultRow[BotUser.sourceType]
    )
}