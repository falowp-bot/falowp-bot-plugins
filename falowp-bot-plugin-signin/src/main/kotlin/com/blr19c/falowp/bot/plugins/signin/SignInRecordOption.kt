package com.blr19c.falowp.bot.plugins.signin

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.plugins.signin.database.SignInRecord
import com.blr19c.falowp.bot.plugins.signin.vo.SignInRecordVo
import com.blr19c.falowp.bot.plugins.user.vo.BotUserVo
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate
import java.time.YearMonth

/**
 * 签到
 */
fun BotUserVo.signIn() {
    return multiTransaction {
        SignInRecord.insert {
            it[userId] = this@signIn.userId
            it[signInDate] = LocalDate.now()
        }
    }
}


/**
 * 获取累计签到次数
 */
fun BotUserVo.queryCumulativeSignIn(): Long {
    return multiTransaction {
        val userId = this@queryCumulativeSignIn.userId
        SignInRecord.selectAll().where { SignInRecord.userId eq userId }.count()
    }
}


/**
 * 获取本月签到信息
 */
fun BotUserVo.queryCurrentMonthSignIn(): List<SignInRecordVo> {
    val start = LocalDate.now().withDayOfMonth(1)
    val end = YearMonth.now().atEndOfMonth()

    return multiTransaction {
        SignInRecord.selectAll()
            .where {
                val userEq = SignInRecord.userId eq this@queryCurrentMonthSignIn.userId
                SignInRecord.signInDate.between(start, end).and(userEq)
            }.map { convertVo(it) }
    }
}

private fun convertVo(resultRow: ResultRow): SignInRecordVo {
    return SignInRecordVo(
        resultRow[SignInRecord.id],
        resultRow[SignInRecord.userId],
        resultRow[SignInRecord.signInDate],
    )
}