package com.blr19c.falowp.bot.plugins.coin.work

import com.blr19c.falowp.bot.plugins.user.currentUser
import com.blr19c.falowp.bot.plugins.user.incrementCoins
import com.blr19c.falowp.bot.system.plugin.MessagePluginRegisterMatch
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Message.message
import com.blr19c.falowp.bot.system.plugin.hook.awaitReply
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Plugin(
    name = "打工",
    desc = """
        <p>打工</p>
        <p>打工赚取金币</p>
    """,
    tag = "小游戏"
)
class Work {

    private val work = message(Regex("打工")) {
        val int1 = listOf(Random.nextInt(1, 100), Random.nextInt(1, 100))
        val int2 = listOf(Random.nextInt(1, 100), Random.nextInt(1, 100))
        val int3 = listOf(Random.nextInt(1, 100), Random.nextInt(1, 100))
        val int4 = listOf(Random.nextInt(1, 100), Random.nextInt(1, 100))
        val int5 = listOf(Random.nextInt(1, 100), Random.nextInt(1, 100))

        val replyQuestion = """
            请在120秒内完成以下5道题目,输入'提交 X X X'进行提交:
            ${int1[0]}+${int1[1]}=?
            ${int2[0]}+${int2[1]}=?
            ${int3[0]}+${int3[1]}=?
            ${int4[0]}+${int4[1]}=?
            ${int5[0]}+${int5[1]}=?
          """.trimIndent()
        this.sendReply(replyQuestion)

        val inputAnswerList = withTimeoutOrNull(120.seconds) {
            val answer = awaitReply(MessagePluginRegisterMatch(regex = Regex("提交([\\d ]+)"))) { (answer) -> answer }
            Regex("\\b\\d+\\b").findAll(answer).map { it.value.toInt() }.toList()
        } ?: return@message this.sendReply("你做的太慢了,没得到任何奖励", reference = true)

        val answerList = listOf(int1.sum(), int2.sum(), int3.sum(), int4.sum(), int5.sum())
        if (inputAnswerList != answerList) {
            return@message this.sendReply("回答错误,你的奖励消失了")
        }

        val award = Random.nextInt(100, 500)
        currentUser().incrementCoins(award.toBigDecimal())
        this.sendReply("你获得了${award}金币的奖励")
    }

    init {
        work.register()
    }
}