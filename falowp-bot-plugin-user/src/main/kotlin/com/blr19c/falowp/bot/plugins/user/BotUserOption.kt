@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.plugins.user

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.plugins.user.database.BotUser
import com.blr19c.falowp.bot.plugins.user.vo.BotUserVo
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.expand.ImageUrl
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
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
    return multiTransaction {
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
    return multiTransaction {
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
    multiTransaction {
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
    multiTransaction {
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