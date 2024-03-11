package com.blr19c.falowp.bot.signin.plugins.signin

import com.blr19c.falowp.bot.signin.database.SigninRecord
import com.blr19c.falowp.bot.signin.vo.SigninRecordVo
import com.blr19c.falowp.bot.user.vo.BotUserVo
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.YearMonth

/**
 * 签到
 */
fun BotUserVo.signin() {
    return transaction {
        SigninRecord.insert {
            it[userId] = this@signin.userId
            it[signinDate] = LocalDate.now()
        }
    }
}


/**
 * 获取累计签到次数
 */
fun BotUserVo.queryCumulativeSignin(): Long {
    return transaction {
        val userId = this@queryCumulativeSignin.userId
        SigninRecord.selectAll().where { SigninRecord.userId eq userId }.count()
    }
}


/**
 * 获取本月签到信息
 */
fun BotUserVo.queryCurrentMonthSignin(): List<SigninRecordVo> {
    val start = LocalDate.now().withDayOfMonth(1)
    val end = YearMonth.now().atEndOfMonth()

    return transaction {
        SigninRecord.selectAll()
            .where {
                val userEq = SigninRecord.userId eq this@queryCurrentMonthSignin.userId
                SigninRecord.signinDate.between(start, end).and(userEq)
            }.map { convertVo(it) }
    }
}

private fun convertVo(resultRow: ResultRow): SigninRecordVo {
    return SigninRecordVo(
        resultRow[SigninRecord.id],
        resultRow[SigninRecord.userId],
        resultRow[SigninRecord.signinDate],
    )
}