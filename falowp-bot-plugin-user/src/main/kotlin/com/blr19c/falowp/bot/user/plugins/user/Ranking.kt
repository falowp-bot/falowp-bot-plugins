package com.blr19c.falowp.bot.user.plugins.user

import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Message.message
import com.blr19c.falowp.bot.system.readPluginResource
import com.blr19c.falowp.bot.system.web.htmlToImageBase64
import com.blr19c.falowp.bot.user.database.BotUser
import com.blr19c.falowp.bot.user.vo.BotUserVo
import org.jetbrains.exposed.sql.SortOrder
import org.jsoup.Jsoup

/**
 * 排行榜
 */
@Plugin(
    name = "金币/好感度排行",
    desc = """
        <p>金币排行</p>
        <p>好感度排行</p>
    """
)
class Ranking {

    private val coinsRanking = message(Regex("金币排行")) {
        val list = queryBySourceId(this.receiveMessage.source.id) {
            it.orderBy(BotUser.coins, order = SortOrder.DESC).take(7)
        }
        val replyImgBase64 = build(list, "coinsRanking.html", "#coinsRankingDiv") {
            it.coins.toPlainString()
        }
        this.sendReply(SendMessage.builder().image(replyImgBase64).build())
    }

    private val impressionRanking = message(Regex("好感度排行")) {
        val list = queryBySourceId(this.receiveMessage.source.id) {
            it.orderBy(BotUser.impression, order = SortOrder.DESC).take(7)
        }
        val replyImgBase64 = build(list, "impressionRanking.html", "#impressionRankingDiv") {
            it.impression.toPlainString()
        }
        this.sendReply(SendMessage.builder().image(replyImgBase64).build())
    }

    private suspend fun build(
        list: List<BotUserVo>,
        htmlFile: String,
        querySelector: String,
        block: (BotUserVo) -> String
    ): String {
        val impressionList = list.map { block.invoke(it) }
        val nameList = list.map { it.nickname }
        val html = readPluginResource(htmlFile) { inputStream ->
            inputStream.bufferedReader().use { Jsoup.parse(it.readText()) }
        }
        html.select("#labels").`val`(Json.toJsonString(nameList))
        html.select("#data").`val`(Json.toJsonString(impressionList))

        return htmlToImageBase64(html.html(), querySelector)
    }


    init {
        coinsRanking.register()
        impressionRanking.register()
    }

}