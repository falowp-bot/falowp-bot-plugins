@file:Suppress("SpellCheckingInspection")

package com.blr19c.falowp.bot.plugins.bili

import com.blr19c.falowp.bot.plugins.bili.api.DatabaseCookiesStorage
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliRoomInfo
import com.blr19c.falowp.bot.plugins.bili.api.data.BiliVideoAiSummary
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.cache.CacheReference
import com.blr19c.falowp.bot.system.expand.encodeToBase64String
import com.blr19c.falowp.bot.system.expand.toImageUrl
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.readPluginResource
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.*
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.ElementHandle
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.Cookie
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.ScreenshotType
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.net.URI
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.days


/**
 * B 站页面截图和卡片工具
 */
@Suppress("SpellCheckingInspection")
object BLiveUtils : Log {

    /**
     * 网页请求使用的 bili_ticket
     */
    private val ticket by CacheReference(1.days) { genWebTicket() }

    /**
     * 获取新的 bili_ticket
     */
    private suspend fun genWebTicket(): String {
        val ts = System.currentTimeMillis() / 1000
        val hmacKey = SecretKeySpec("XgwSnGZ1p".toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        val hexSign = mac.doFinal("ts$ts".toByteArray()).joinToString("") { "%02x".format(it) }

        val formData = Parameters.build {
            append("key_id", "ec02")
            append("hexsign", hexSign)
            append("context[ts]", ts.toString())
            append("csrf", "")
        }
        return webclient().post("https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket?${formData.formUrlEncode()}") {
            for ((name, value) in DatabaseCookiesStorage.getAll()) {
                cookie(name, value)
            }
        }.bodyAsJsonNode()["data"]["ticket"].safeString()
    }


    /**
     * 创建带 B 站登录信息的浏览器环境
     */
    private suspend fun <T> browserContext(block: BrowserContext.() -> T): T {
        return withContext(Dispatchers.IO) {
            val cookies = DatabaseCookiesStorage.getAll().map {
                Cookie(it.name, it.value)
                    .setDomain(".bilibili.com")
                    .setPath(it.path)
                    .setHttpOnly(it.httpOnly)
                    .setSecure(it.secure)
            }.toMutableList()
            cookies.add(Cookie("bili_ticket", ticket).setDomain(".bilibili.com").setPath("/"))
            commonWebdriverContext {
                this.addCookies(cookies)
                block(this)
            }
        }
    }

    /**
     * 截取一条 B 站动态
     */
    suspend fun dynamicScreenshot(dynamicId: String): String? = coroutineScope {
        val url = "https://www.bilibili.com/opus/$dynamicId"
        log().info("b站动态截屏:{}", dynamicId)
        browserContext {
            this.newPage().use { page ->
                page.navigate(url)
                page.waitForLoadState(LoadState.NETWORKIDLE)
                if (page.title().contains("验证码")) {
                    log().info("b站动态截屏出现验证码,等待下次重试:{}", dynamicId)
                    return@browserContext null
                }
                if (page.title().contains("出错")) {
                    log().info("b站动态截屏出现页面错误,等待下次重试:{}", dynamicId)
                    return@browserContext null
                }
                removeDynamicUselessParts(page)
                // 旧版动态页面
                val biliDynPage = page.existsToExecute(".bili-dyn-item__main") {
                    biliDynPage(page)
                }
                // 新版图文页面
                val biliOpusPage = page.existsToExecute(".bili-opus-view") {
                    biliOpusPage(page)
                }
                // 专栏页面
                val articlePage = page.existsToExecute(".article-container") {
                    articlePage(page)
                }
                (biliDynPage + biliOpusPage + articlePage).firstOrNull()
                    ?: screenshot("body", page)
            }
        }
    }

    /**
     * 移除截图里不需要的页面元素
     */
    private fun removeDynamicUselessParts(page: Page) {
        scrollToBottom(page)
        // 顶部导航
        page.existsToExecute(".bili-header") {
            page.evaluate("""document.querySelector(".bili-header").remove();""")
        }
        // 滚动后固定在顶部的导航
        page.existsToExecute(".fixed-top-header") {
            page.evaluate("""document.querySelector(".fixed-top-header").remove();""")
        }
        // 推荐内容
        page.existsToExecute(".bili-dyn-card-link-common") {
            page.evaluate("""document.querySelector(".bili-dyn-card-link-common").remove();""")
        }
        // 回复输入框
        page.existsToExecute(".fixed-reply-box") {
            page.evaluate("""document.querySelector(".fixed-reply-box").remove();""")
        }
        // 关注推荐
        page.existsToExecute(".fixed-author-header") {
            page.evaluate("""document.querySelector(".fixed-author-header").remove();""")
        }
    }

    /**
     * 整理专栏页面
     */
    private fun articlePage(page: Page): String {
        // 底部版权信息
        page.existsToExecute(".article-footer-box") {
            page.evaluate("""document.querySelector(".article-footer-box").remove();""")
        }
        // 分享区域
        page.existsToExecute(".interaction-info") {
            page.evaluate("""document.querySelector(".interaction-info").remove();""")
        }
        // 专栏较长 截图不带评论
        page.existsToExecute(".comment-wrapper") {
            page.evaluate("""document.querySelector(".comment-wrapper").remove();""")
        }
        return screenshot(".article-detail", page)
    }

    /**
     * 整理新版图文页面
     */
    private fun biliOpusPage(page: Page): String {
        // 只保留前三条评论
        page.existsToExecute(".bili-comment-container") {
            page.evaluate(
                """const replyList = Array.from(document.querySelectorAll(".reply-item")).slice(0, 3);
                        replyList.forEach(item=> document.querySelector(".opus-module-content").appendChild(item));
                """.trimIndent()
            )
        }
        return screenshot(".bili-opus-view", page)
    }

    /**
     * 整理旧版动态页面
     */
    private fun biliDynPage(page: Page): String {
        // 只保留前三条评论
        page.existsToExecute(".bili-dyn-item") {
            page.evaluate(
                """const replyList = Array.from(document.querySelectorAll(".reply-item")).slice(0, 3);
                   replyList.forEach(item=> document.querySelector(".bili-dyn-item__main").appendChild(item));
                """.trimIndent()
            )
        }
        return screenshot(".bili-dyn-item__main", page)
    }

    /**
     * 滚动到底部
     */
    private fun scrollToBottom(page: Page) {
        val viewportHeight = page.viewportSize().height.toDouble()
        val scrollHeight = page.evaluate("document.documentElement.scrollHeight").toString().toDouble()
        val scrollStep = viewportHeight / 10.0
        var currentScroll = 0.0
        while (currentScroll < scrollHeight) {
            currentScroll += scrollStep
            page.evaluate("window.scrollTo(0, $currentScroll)")
            page.waitForTimeout(100.0)
        }
    }

    /**
     * 截图
     */
    private fun screenshot(selector: String, page: Page): String {
        val screenshot = page.querySelector(selector)
            ?.screenshot(ElementHandle.ScreenshotOptions().setQuality(100).setType(ScreenshotType.JPEG))
            ?: page.screenshot(Page.ScreenshotOptions().setQuality(100).setType(ScreenshotType.JPEG))
        return screenshot.encodeToBase64String()
    }

    /**
     * 从 B 站视频地址中取出 BV 号
     */
    fun extractBvFromBiliUrl(biliUrl: String): String? {
        val url = URI.create(biliUrl)
        val pathSegments = url.path.split("/")
        val bvIndex = pathSegments.indexOf("video") + 1
        if (bvIndex > 0 && bvIndex < pathSegments.size) {
            return pathSegments[bvIndex]
        }
        return null
    }

    /**
     * 把视频 AI 摘要做成图片
     */
    suspend fun videoSummarize(biliVideoAiSummary: BiliVideoAiSummary): String? {
        if (!biliVideoAiSummary.support()) return null
        val htmlString = readPluginResource("ai/aiSummary.html") { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
        val htmlBody = Jsoup.parse(htmlString)
        htmlBody.select("#summary").html(biliVideoAiSummary.modelResult.summary)
        val lineList = mutableListOf<String>()
        for ((title, part) in biliVideoAiSummary.modelResult.outline) {
            val titleLine = """<div class="section-title" id="title">$title</div>"""
            val partList = mutableListOf<String>()
            for ((timestamp, content) in part) {
                val time = formatTimestamp(timestamp)
                val contentLine = """<span class="content">$content</span>"""
                val timeLine = """<span class="timestamp"><span class="timestamp-inner">$time</span></span>"""
                val partLine = """<div class="bullet">$timeLine$contentLine</div>"""
                partList.add(partLine)
            }
            val line = """<div class="section">$titleLine${partList.joinToString("")}</div>"""
            lineList.add(line)
        }
        htmlBody.select("#outline").html(lineList.joinToString(""))
        return browserContext {
            this.newPage().use { page ->
                page.setContent(htmlBody.html())
                page.waitForLoadState(LoadState.NETWORKIDLE)
                page.querySelector(".ai-summary").screenshot().encodeToBase64String()
            }
        }
    }

    /**
     * 生成 B 站开播卡片
     */
    suspend fun liveCard(roomInfo: BiliRoomInfo): String {
        val liveLogoImg = withContext(Dispatchers.IO) {
            readPluginResource("live/live-logo.png").encodeToBase64String()
        }
        val htmlString = readPluginResource("live/live.html") { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
        val htmlBody = Jsoup.parse(htmlString)
        val cover = "data:image/png;base64,${roomInfo.cover.trim().toImageUrl().toBase64()}"
        htmlBody.select("#background").attr("src", cover)
        // 标题
        htmlBody.select(".title").html(roomInfo.title)
        // 主播头像
        htmlBody.select(".face").backgroundUrl(roomInfo.face)
        // 直播角标
        htmlBody.select(".live-logo").backgroundUrl(liveLogoImg)
        // 主播昵称
        htmlBody.select(".live-user").html("UP猪: ${roomInfo.name}")
        // 直播间 ID
        htmlBody.select(".room-id").html("房间号: ${roomInfo.roomId}")
        // 底栏标题
        htmlBody.select(".bottom-bar-title").html("${systemConfigProperty("nickname")}直播推送")
        return htmlToImageBase64(htmlBody.html(), "#background")
    }

    /**
     * 把图片写进元素的背景样式
     */
    private suspend fun Elements.backgroundUrl(url: String) {
        var style = this.attr("style")
        if (style.isNotBlank() && !style.endsWith(";")) {
            style += ";"
        }
        style += """background-image: url("data:image/png;base64,${url.trim().toImageUrl().toBase64()}");"""
        this.attr("style", style)
    }

    /**
     * 把视频秒数显示成分秒
     */
    private fun formatTimestamp(time: Long): String {
        val minutes = time / 60
        val seconds = time % 60
        return "%02d:%02d".format(minutes, seconds)
    }

}
