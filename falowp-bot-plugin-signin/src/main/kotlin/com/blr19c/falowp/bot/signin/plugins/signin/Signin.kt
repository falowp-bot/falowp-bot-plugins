package com.blr19c.falowp.bot.signin.plugins.signin

import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.image.ImageUrl
import com.blr19c.falowp.bot.system.image.encodeToBase64String
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Message.message
import com.blr19c.falowp.bot.system.readPluginResource
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.htmlToImageBase64
import com.blr19c.falowp.bot.user.plugins.user.currentUser
import com.blr19c.falowp.bot.user.plugins.user.incrementCoins
import com.blr19c.falowp.bot.user.plugins.user.incrementImpression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.sql.SQLException
import java.time.LocalDate
import kotlin.random.Random

/**
 * 签到
 */
@Plugin(name = "签到", desc = "每日一签,能增加金币&好感度")
class Signin {

    private val signin = message(Regex("签到")) {
        val currentUser = this.currentUser()
        val addCoins = Random.nextDouble(10.0, 50.0).toBigDecimal().setScale(2)
        val addImpression = Random.nextDouble(0.5, 5.0).toBigDecimal().setScale(2)
        try {
            //签到&增加金币和好感度
            currentUser.signin()
            currentUser.incrementCoins(addCoins)
            currentUser.incrementImpression(addImpression)

            val backgroundImg = withContext(Dispatchers.IO) {
                readPluginResource("background.png").encodeToBase64String()
            }

            val html = readPluginResource("signin.html") { inputStream ->
                inputStream.bufferedReader().use { Jsoup.parse(it.readText()) }
            }

            val monthSignin = currentUser.queryCurrentMonthSignin().map { it.signinDate }

            html.select("#app").backgroundUrl(backgroundImg)
            html.select("#nickname").html(currentUser.nickname)
            html.select("#uid").html(currentUser.userId)
            html.select("#qqAvatar").attr("src", "data:image/png;base64,${currentUser.avatar.toHtmlBase64()}")
            html.select("#signin-day").html(currentUser.queryCumulativeSignin().toString())
            html.select("#signin-impression").html(addImpression.toString())
            html.select("#signin-coins").html(addCoins.toString())
            html.select("#impression").html((currentUser.impression + addImpression).toString())
            html.select("#coins").html((currentUser.coins + addCoins).toPlainString())
            html.select("#by").html(systemConfigProperty("nickname") + LocalDate.now())
            html.select("#selectDate").`val`(Json.toJsonString(monthSignin))

            val replyImgBase64 = htmlToImageBase64(html.html(), "#app")

            this.sendReply(SendMessage.builder().images(replyImgBase64).build())
        } catch (e: SQLException) {
            this.sendReply("你今天已经签到过了", reference = true)
        }
    }

    private suspend fun Elements.backgroundUrl(url: String) {
        var style = this.attr("style")
        if (style.isNotBlank() && !style.endsWith(";")) {
            style += ";"
        }
        style += """
            background-image: url("data:image/png;base64,${ImageUrl(url.trim()).toBase64()}");
            background-size: cover;
            """.trimIndent()
        this.attr("style", style)
    }

    init {
        signin.register()
    }

}