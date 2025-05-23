package com.blr19c.falowp.bot.plugins.bili

import com.blr19c.falowp.bot.plugins.bili.BLiveUtils.extractBvFromBiliUrl
import com.blr19c.falowp.bot.plugins.bili.BLiveUtils.videoSummarize
import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.api.*
import com.blr19c.falowp.bot.plugins.bili.database.BiliDynamic
import com.blr19c.falowp.bot.plugins.bili.database.BiliSubscription
import com.blr19c.falowp.bot.plugins.bili.database.BiliUpInfo
import com.blr19c.falowp.bot.plugins.bili.message.biliMessage
import com.blr19c.falowp.bot.plugins.bili.vo.BiliSubscriptionVo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.expand.encodeToBase64String
import com.blr19c.falowp.bot.system.plugin.MessagePluginRegisterMatch
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Message.message
import com.blr19c.falowp.bot.system.plugin.Plugin.Task.periodicScheduling
import com.blr19c.falowp.bot.system.web.urlToRedirectUrl
import com.blr19c.falowp.bot.system.web.webclient
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Plugin(
    name = "b站订阅",
    desc = """
            <p>B站直播,UP动态等提醒(0-6点不会推送)</p>
            <p>指令:</p>
            <p>登录 [bB]站登录</p>
            <p>添加订阅 [bB]站订阅 uid</p>
            <p>删除订阅 删除[bB]站订阅 uid</p>
            <p>查看订阅 查看[bB]站订阅</p>
            <p>示例: B站订阅 123 b站订阅123</p>
            <p>示例: 删除B站订阅 123 删除b站订阅123</p>
            <p>示例: 查看b站订阅</p>
            <p>被动1: 当接收到视频分享后会自动查询视频的AI总结信息</p>
            <p>被动2: 当UP开播后会推送开播信息</p>
            <p>被动3: 当UP发送动态后会推送动态信息</p>
    """
)
class Subscription : Log {

    private val client by lazy { BiliClient() }

    private suspend fun BotApi.send(
        subscriptionList: List<BiliSubscriptionVo>,
        sendMessageChain: SendMessageChain
    ) {
        for (biliSubscriptionVo in subscriptionList) {
            if (biliSubscriptionVo.sourceType == SourceTypeEnum.PRIVATE.name) {
                this.sendPrivate(sendMessageChain, sourceId = biliSubscriptionVo.sourceId)
            }
            if (biliSubscriptionVo.sourceType == SourceTypeEnum.GROUP.name) {
                this.sendGroup(sendMessageChain, sourceId = biliSubscriptionVo.sourceId)
            }
        }
    }

    /**
     * 定时查询动态/直播
     */
    private val dynamicTask = periodicScheduling(30.seconds) {
        log().info("定时查询动态/直播")
        for (biliUpInfoVo in BiliUpInfo.queryAll()) {
            //订阅列表
            val subscriptionList = BiliSubscription.queryByMid(biliUpInfoVo.mid)
            if (subscriptionList.isEmpty()) continue
            //动态列表
            val dynamicList = client.spaceDynamicInfo(biliUpInfoVo.mid.toLong()).items
            //将已经发送过的去除
            val alreadyPushDynamicList = BiliDynamic.queryByMid(biliUpInfoVo.mid)
            val prePushDynamicList = dynamicList.filter { !alreadyPushDynamicList.contains(it.id) }
            for (prePushDynamic in prePushDynamicList) {
                //直播
                if (prePushDynamic.type.startsWith("DYNAMIC_TYPE_LIVE") && !biliUpInfoVo.liveStatus) {
                    val roomId = biliUpInfoVo.roomId
                    val liveInfo = client.getLiveInfo(roomId.toLong())
                    val liveCard = BLiveUtils.liveCard(liveInfo, biliUpInfoVo)
                    val message = SendMessage.builder()
                        .text("${biliUpInfoVo.name}猪开播啦!")
                        .image(liveCard)
                        .biliMessage(biliUpInfoVo, "https://live.bilibili.com/$roomId", roomId)
                        .build()
                    log().info("定时查询动态/直播-${biliUpInfoVo.name}猪开播啦!")
                    send(subscriptionList, message)
                    multiTransaction {
                        BiliUpInfo.updateLiveStatus(biliUpInfoVo.mid, true)
                        BiliDynamic.insert(biliUpInfoVo.mid, prePushDynamic.id)
                    }
                    continue
                }
                //动态
                val dynamicScreenshot = BLiveUtils.dynamicScreenshot(prePushDynamic.id) ?: continue
                val url = "https://www.bilibili.com/opus/${prePushDynamic.id}"
                val message = SendMessage.builder()
                    .text("${biliUpInfoVo.name}猪有新动态!")
                    .image(dynamicScreenshot)
                    .biliMessage(biliUpInfoVo, url, prePushDynamic.id)
                    .build()
                log().info("定时查询动态/直播-${biliUpInfoVo.name}猪有新动态!")
                send(subscriptionList, message)
                BiliDynamic.insert(biliUpInfoVo.mid, prePushDynamic.id)
            }
        }
    }

    /**
     * 定时查询已开播的up的开播状态
     */
    private val liveTask = periodicScheduling(1.minutes) {
        log().info("定时查询开播状态")
        for (biliUpInfoVo in BiliUpInfo.queryByLiveStatus(true)) {
            val roomId = biliUpInfoVo.roomId
            if (!client.getLiveInfo(roomId.toLong()).roomInfo.liveStatus) {
                BiliUpInfo.updateLiveStatus(biliUpInfoVo.mid, false)
                val message = SendMessage.builder()
                    .text("${biliUpInfoVo.name}猪直播结束了,下次再看吧～")
                    .biliMessage(biliUpInfoVo, "https://live.bilibili.com/$roomId", roomId)
                    .build()
                send(BiliSubscription.queryByMid(biliUpInfoVo.mid), message)
            }
        }
    }

    /**
     * 更新up信息
     */
    private val updateUserInfoTask = periodicScheduling(1.days) {
        log().info("更新up信息")
        for (biliUpInfo in BiliUpInfo.queryAll()) {
            val userInfo = client.getUserInfo(biliUpInfo.mid.toLong())
            if (biliUpInfo.name != userInfo.name) multiTransaction {
                BiliUpInfo.update({ BiliUpInfo.mid eq biliUpInfo.mid }) {
                    it[name] = userInfo.name
                }
            }
        }
    }

    /**
     * 订阅
     */
    private val subscription = message(Regex("[bB]站订阅\\s?(\\d+)"), auth = ApiAuth.MANAGER) { (subscriptionMid) ->
        try {
            if (BiliSubscription.queryByMid(subscriptionMid).any { it.sourceId == this.receiveMessage.source.id }) {
                return@message this.sendReply("此up已被订阅")
            }
            val userInfo = client.getUserInfo(subscriptionMid.toLong())
            val dynamicList = client.spaceDynamicInfo(subscriptionMid.toLong())
                .items
                .filter { !it.type.startsWith("DYNAMIC_TYPE_LIVE") }
                .map { it.id }
            val roomId = userInfo.liveRoom?.roomId ?: ""
            val midString = userInfo.mid
            multiTransaction {
                dynamicList.forEach { BiliDynamic.insert(midString, it) }
                BiliSubscription.insert(
                    midString,
                    this@message.receiveMessage.source.id,
                    this@message.receiveMessage.source.type.name
                )
                if (BiliUpInfo.queryByMid(midString) == null) {
                    BiliUpInfo.insert(midString, roomId, userInfo.name)
                }
            }
            this.sendReply("订阅:$subscriptionMid(${userInfo.name})完成")
        } catch (e: Exception) {
            this.sendReply("订阅失败:${e.message}")
        }
    }

    /**
     * 删除订阅
     */
    private val delSubscription = message(Regex("删除[bB]站订阅\\s?(\\d+)"), auth = ApiAuth.MANAGER) { (deleteMid) ->
        val subscriptionList = BiliSubscription.queryByMid(deleteMid)
        val subscription = subscriptionList.firstOrNull { it.sourceId == this.receiveMessage.source.id }
        if (subscription == null) {
            return@message this.sendReply("此订阅不存在")
        }
        val upInfo = BiliUpInfo.queryByMid(subscription.mid)
        multiTransaction {
            BiliSubscription.deleteWhere { id eq subscription.id }
            BiliUpInfo.queryByMid(subscription.mid)
            if (subscriptionList.size == 1) {
                BiliUpInfo.deleteWhere { mid eq subscription.mid }
                BiliDynamic.deleteWhere { mid eq subscription.mid }
            }
        }
        this.sendReply("已删除(${upInfo?.name})的订阅")
    }

    /**
     * 登录
     */
    private val login = message(Regex("[bB]站登录"), auth = ApiAuth.ADMINISTRATOR) {
        try {
            client.login { url ->
                val loginQrcode = with(QRCodeWriter()) {
                    val matrix = encode(url, BarcodeFormat.QR_CODE, 250, 250)
                    val image = MatrixToImageWriter.toBufferedImage(matrix)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    ImageIO.write(image, "PNG", byteArrayOutputStream)
                    byteArrayOutputStream.toByteArray().encodeToBase64String()
                }
                val message = SendMessage.builder().image(loginQrcode).build()
                this@message.sendReply(message)
            }
            this@message.sendReply("登录成功")
        } catch (e: Exception) {
            this@message.sendReply(e.message ?: "登录失败")
        }
    }

    /**
     * 视频ai描述
     */
    private val videoAi = message(
        MessagePluginRegisterMatch(
            messageType = MessageTypeEnum.SHARE,
            customBlock = { receiveMessage ->
                receiveMessage.content.share.any { it.appName == "哔哩哔哩" }
            }
        )
    ) {
        val webclient = webclient()
        val sourceUrlList = this.receiveMessage.content.share
            .filter { it.appName == "哔哩哔哩" }
            .map { it.sourceUrl }
            .toList()
        val replyMessages = sourceUrlList
            .mapNotNull { webclient.urlToRedirectUrl(it) }
            .mapNotNull { extractBvFromBiliUrl(it) }
            .mapNotNull { videoSummarize(client.getVideoAiSummary(it)) }
            .map { SendMessage.builder().image(it).build() }
            .toTypedArray()
        if (replyMessages.isEmpty()) {
            this.sendReply("看不懂啊")
        }
        this.sendReply(*replyMessages)
    }

    /**
     * 查看b站订阅
     */
    private val viewSubscription = message(Regex("查看[bB]站订阅")) {
        val subscriptionList = BiliSubscription.queryBySourceId(this.receiveMessage.source.id)
            .map { BiliUpInfo.queryByMid(it.mid) }
            .map { "${it?.name}(${it?.mid})" }
            .toList()
        if (subscriptionList.isEmpty()) {
            return@message this.sendReply("当前没有任何订阅")
        }
        this.sendReply(subscriptionList.joinToString("\n"))
    }

    init {
        subscription.register()
        delSubscription.register()
        login.register()
        dynamicTask.register()
        liveTask.register()
        videoAi.register()
        viewSubscription.register()
        updateUserInfoTask.register()
    }
}