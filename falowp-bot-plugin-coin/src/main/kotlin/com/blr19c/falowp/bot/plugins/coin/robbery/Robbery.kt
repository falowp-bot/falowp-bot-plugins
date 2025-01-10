package com.blr19c.falowp.bot.plugins.coin.robbery

import com.blr19c.falowp.bot.plugins.coin.robbery.database.RobberyInfo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.plugins.user.currentUser
import com.blr19c.falowp.bot.plugins.user.decrementCoins
import com.blr19c.falowp.bot.plugins.user.incrementCoins
import com.blr19c.falowp.bot.plugins.user.queryByUserId
import com.blr19c.falowp.bot.plugins.user.vo.BotUserVo
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Message.message
import org.jetbrains.exposed.sql.insert
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.time.LocalDate
import kotlin.random.Random

/**
 * 打劫
 */
@Plugin(
    name = "打劫",
    desc = """
        <p>@某人 打劫</p>
        <p>好感度会影响打劫成功率</p>
    """,
    tag = "小游戏"
)
class Robbery {

    private val robbery = message(Regex("打劫")) {
        val currentUser = this.currentUser()
        val atList = this.receiveMessage.content.at
        if (atList.size > 1 && successRate(currentUser, 9)) {
            val medicalExpenses = Random.nextDouble(100.0, 500.0)
                .toBigDecimal().setScale(2, RoundingMode.HALF_UP)
            currentUser.decrementCoins(medicalExpenses)
            this.sendReply(
                "对方人多,你被打了一顿,损失了${medicalExpenses.toPlainString()}金币的医药费",
                reference = true
            )
            return@message
        }

        try {
            var sum = BigDecimal.ZERO
            multiTransaction {
                for (user in atList) {
                    if (successRate(currentUser)) {
                        val toUser = queryByUserId(user.id, receiveMessage.source.id)!!
                        val coins = Random.nextDouble(10.0, 300.0)
                            .toBigDecimal()
                            .setScale(2, RoundingMode.HALF_UP)
                            .min(toUser.coins)
                        toUser.decrementCoins(coins)
                        currentUser.incrementCoins(coins)
                        sum = sum.add(coins)
                    }
                    RobberyInfo.insert {
                        it[userId] = currentUser.userId
                        it[robbedUserId] = user.id
                        it[createDate] = LocalDate.now()
                    }
                }
            }
            this.sendReply("你抢到了${sum.toPlainString()}个金币")
        } catch (e: SQLException) {
            this.sendReply("这些人你今天已经抢过了")
        }
    }

    private fun successRate(botUserVo: BotUserVo, cardinality: Int = 6): Boolean {
        val impressionRate = botUserVo.impression.divide(BigDecimal(100)).max(BigDecimal(3)).toInt()
        return Random.nextInt(1, 10) > cardinality - impressionRate
    }


    init {
        robbery.register()
    }
}