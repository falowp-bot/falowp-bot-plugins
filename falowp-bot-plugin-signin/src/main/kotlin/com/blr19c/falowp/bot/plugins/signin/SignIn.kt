package com.blr19c.falowp.bot.plugins.signin

import com.blr19c.falowp.bot.plugins.user.currentUser
import com.blr19c.falowp.bot.plugins.user.incrementCoins
import com.blr19c.falowp.bot.plugins.user.incrementImpression
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.expand.encodeToBase64String
import com.blr19c.falowp.bot.system.expand.toImageUrl
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.message.message
import com.blr19c.falowp.bot.system.readPluginResource
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.htmlToImageBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.math.RoundingMode
import java.sql.SQLException
import java.time.LocalDate
import kotlin.random.Random

/**
 * 签到
 */
@Plugin(name = "签到", desc = "每日一签,能增加金币&好感度")
class SignIn {

    private val signIn = message(Regex("签到")) {
        val currentUser = this.currentUser()
        val addCoins = Random.nextDouble(10.0, 50.0).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        val addImpression = Random.nextDouble(0.5, 5.0).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        try {
            //签到&增加金币和好感度
            currentUser.signIn()
            currentUser.incrementCoins(addCoins)
            currentUser.incrementImpression(addImpression)

            val backgroundImg = withContext(Dispatchers.IO) {
                readPluginResource("background.png").encodeToBase64String()
            }

            val html = readPluginResource("signIn.html") { inputStream ->
                inputStream.bufferedReader().use { Jsoup.parse(it.readText()) }
            }

            val monthSignIn = currentUser.queryCurrentMonthSignIn().map { it.signInDate }

            html.select("#app").backgroundUrl(backgroundImg)
            html.select("#nickname").html(currentUser.nickname)
            html.select("#uid").html(currentUser.userId)
            html.select("#qqAvatar").attr("src", "data:image/png;base64,${currentUser.avatar.toHtmlBase64()}")
            html.select("#signin-day").html(currentUser.queryCumulativeSignIn().toString())
            html.select("#signin-impression").html(addImpression.toString())
            html.select("#signin-coins").html(addCoins.toString())
            html.select("#impression").html((currentUser.impression + addImpression).toString())
            html.select("#coins").html((currentUser.coins + addCoins).toPlainString())
            html.select("#by").html(systemConfigProperty("nickname") + LocalDate.now())
            html.select("#selectDate").`val`(Json.toJsonString(monthSignIn))

            val replyImgBase64 = htmlToImageBase64(html.html(), "#app")

            this.sendReply(SendMessage.builder().image(replyImgBase64).build())
        } catch (_: SQLException) {
            this.sendReply("你今天已经签到过了", reference = true)
        }
    }

    private suspend fun Elements.backgroundUrl(url: String) {
        var style = this.attr("style")
        if (style.isNotBlank() && !style.endsWith(";")) {
            style += ";"
        }
        style += """
            background-image: url("data:image/png;base64,${url.trim().toImageUrl().toBase64()}");
            background-size: cover;
            """.trimIndent()
        this.attr("style", style)
    }

    init {
        signIn.register()
    }

}