package com.blr19c.falowp.bot.plugins.bili

import com.blr19c.falowp.bot.plugins.bili.BLiveUtils.extractBvFromBiliUrl
import com.blr19c.falowp.bot.plugins.bili.BLiveUtils.videoSummarize
import com.blr19c.falowp.bot.plugins.bili.api.BiliClient
import com.blr19c.falowp.bot.plugins.bili.api.api.*
import com.blr19c.falowp.bot.plugins.bili.database.BiliDynamic
import com.blr19c.falowp.bot.plugins.bili.database.BiliSubscription
import com.blr19c.falowp.bot.plugins.bili.database.BiliUpInfo
import com.blr19c.falowp.bot.plugins.bili.event.BiliDynamicEvent
import com.blr19c.falowp.bot.plugins.bili.event.BiliLiveEndEvent
import com.blr19c.falowp.bot.plugins.bili.event.BiliLiveStartEvent
import com.blr19c.falowp.bot.plugins.bili.vo.BiliSubscriptionVo
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.expand.encodeToBase64String
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.message.MessageMatch
import com.blr19c.falowp.bot.system.plugin.message.message
import com.blr19c.falowp.bot.system.plugin.task.periodicScheduling
import com.blr19c.falowp.bot.system.web.urlToRedirectUrl
import com.blr19c.falowp.bot.system.web.webclient
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.update
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@Plugin(
    name = "b站订阅",
    desc = """
            <p>B站直播,UP动态等提醒(系统休息时段不会推送)</p>
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
        for ((_, _, sourceId, sourceType) in subscriptionList) {
            if (sourceType == SourceTypeEnum.PRIVATE.name) {
                this.sendPrivate(sendMessageChain, sourceId = sourceId)
            }
            if (sourceType == SourceTypeEnum.GROUP.name) {
                this.sendGroup(sendMessageChain, sourceId = sourceId)
            }
        }
    }

    /**
     * 定时检查新动态
     */
    private val dynamicTask = periodicScheduling(1.minutes) {
        log().info("定时查询动态/直播")
        for ((_, mid, _, name, liveStatus) in BiliUpInfo.queryAll()) {
            // 找到需要接收这个 UP 主消息的会话
            val subscriptionList = BiliSubscription.queryByMid(mid)
            if (subscriptionList.isEmpty()) continue
            // 拉取 UP 主最新一页动态
            val dynamicList = client.spaceDynamicInfo(mid.toLong()).items
            // 去掉已经推送过的动态
            val alreadyPushDynamicList = BiliDynamic.queryByMid(mid)
            val prePushDynamicList = dynamicList.filter { !alreadyPushDynamicList.contains(it.id) }
            for ((id, type) in prePushDynamicList.reversed()) {
                // 开播提醒交给直播任务处理
                if (type.startsWith("DYNAMIC_TYPE_LIVE") && !liveStatus) {
                    BiliDynamic.insert(mid, id)
                    continue
                }
                // 普通动态截图后推送
                val dynamicScreenshot = BLiveUtils.dynamicScreenshot(id) ?: continue
                val message = SendMessage.builder()
                    .text("${name}猪有新动态!")
                    .image(dynamicScreenshot)
                    .build()
                log().info("定时查询动态/直播-${name}猪有新动态!")
                send(subscriptionList, message)
                publishEvent(BiliDynamicEvent(mid, name, id, type))
                BiliDynamic.insert(mid, id)
            }
        }
    }

    /**
     * 定时检查 UP 主的开播状态
     */
    private val liveTask = periodicScheduling(1.minutes) {
        log().info("定时查询开播状态")
        val upInfoList = BiliUpInfo.queryAll()
        val roomInfoList = client.batchRoomInfo(upInfoList.map { it.mid })
        for (roomInfo in roomInfoList) {
            val upInfo = upInfoList.single { it.mid == roomInfo.uid }
            if (roomInfo.liveStatus == 1 && !upInfo.liveStatus) {
                BiliUpInfo.updateLiveStatus(roomInfo.uid, true)
                val liveCard = BLiveUtils.liveCard(roomInfo)
                val message = SendMessage.builder()
                    .text("${roomInfo.name}猪开播啦!")
                    .image(liveCard)
                    .build()
                log().info("定时查询动态/直播-${roomInfo.name}猪开播啦!")
                send(BiliSubscription.queryByMid(roomInfo.uid), message)
                publishEvent(BiliLiveStartEvent(roomInfo))
            }
            if (roomInfo.liveStatus != 1 && upInfo.liveStatus) {
                BiliUpInfo.updateLiveStatus(roomInfo.uid, false)
                val message = SendMessage.builder()
                    .text("${roomInfo.name}猪直播结束了,下次再看吧～")
                    .build()
                send(BiliSubscription.queryByMid(roomInfo.uid), message)
                publishEvent(BiliLiveEndEvent(roomInfo))
            }
        }
    }

    /**
     * 每天更新一次 UP 主昵称
     */
    private val updateUserInfoTask = periodicScheduling(1.days) {
        log().info("更新up信息")
        for ((_, mid, _, name) in BiliUpInfo.queryAll()) {
            val userInfo = client.getUserInfo(mid.toLong())
            if (name != userInfo.name) multiTransaction {
                BiliUpInfo.update({ BiliUpInfo.mid eq mid }) {
                    it[BiliUpInfo.name] = userInfo.name
                }
            }
        }
    }

    /**
     * 添加 B 站订阅
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
     * 删除 B 站订阅
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
     * 扫码登录 B 站
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
     * 收到 B 站视频分享后生成 AI 摘要卡片
     */
    private val videoAi = message(
        MessageMatch(
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
     * 查看当前会话的 B 站订阅
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
