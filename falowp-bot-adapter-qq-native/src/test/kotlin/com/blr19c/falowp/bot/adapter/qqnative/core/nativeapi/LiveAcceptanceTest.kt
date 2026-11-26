package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeClient
import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import com.blr19c.falowp.bot.adapter.qqnative.core.session.QQNativeSessionPhase
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Tag
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Opt-in, real sends to exactly the configured test group. Never included in normal builds.
 */
@Tag("live")
class LiveAcceptanceTest {
    @Test
    fun newForwardBodiesMediaAndQuotesAreReadable(): Unit = runBlocking(Dispatchers.IO) {
        fun env(key: String) = requireNotNull(System.getenv(key)?.takeIf { it.isNotBlank() }) { "缺少 $key" }
        check(System.getProperty("os.name") == "Linux") { "真实验证需要 Linux NTQQ" }
        val group = env("FALOWP_QQ_LIVE_GROUP")
        val self = env("FALOWP_QQ_LIVE_ACCOUNT")
        require(listOf(group, self).all { Regex("[1-9]\\d*").matches(it) })
        val image = Path.of(env("FALOWP_QQ_LIVE_IMAGE"))
        val voice = Path.of(env("FALOWP_QQ_LIVE_VOICE"))
        val video = Path.of(env("FALOWP_QQ_LIVE_VIDEO"))
        listOf(image, voice, video).forEach { require(Files.isRegularFile(it)) }
        val dimensions = checkNotNull(ImageIO.read(image.toFile())) { "图片不可读" }
        val client = QQNativeClient({ _, _ -> }, { _, _ -> })
        client.start()
        withTimeout(120_000.milliseconds) { while (client.status.phase != QQNativeSessionPhase.ONLINE) delay(250.milliseconds) }
        assertEquals(self, client.status.accountId, "账号不匹配，不发送任何消息")
        val account = client.account(self)
        val peer = QQNativeConversation.Group(group)
        assertTrue(peer in account.contacts.groups())
        val messages = account.messages
        val report = messages.send(
            peer,
            QQNativeOutgoingChain(
                QQNativeOutgoingPart.Text("Kotlin 验收汇总"),
                QQNativeOutgoingPart.Partition,
                QQNativeOutgoingPart.Text("明细甲"),
                QQNativeOutgoingPart.Partition,
                QQNativeOutgoingPart.Text("明细乙")
            ),
            forward = true
        ).single()

        fun texts(rows: List<QQNativeMessage>) =
            rows.flatMap { it.parts.filterIsInstance<QQNativeReceivedPart.Text>() }.map { it.value }
        assertEquals(listOf("Kotlin 验收汇总", "明细甲", "明细乙"), texts(messages.readForward(peer, report.id)))
        val nested = messages.send(
            peer,
            QQNativeOutgoingChain(QQNativeOutgoingPart.Text("链甲")),
            QQNativeOutgoingChain(QQNativeOutgoingPart.Text("链乙")),
            forward = true
        ).single()
        val children = messages.readForward(peer, nested.id)
        assertEquals(2, children.size)
        assertEquals(
            listOf("链甲", "链乙"),
            texts(children.flatMap { messages.readForward(peer, nested.id, it.receipt.id) })
        )
        val media = messages.send(
            peer,
            QQNativeOutgoingChain(
                QQNativeOutgoingPart.Image(image, ".png", 1001, dimensions.width, dimensions.height),
                QQNativeOutgoingPart.Voice(voice),
                QQNativeOutgoingPart.Video(video)
            ),
            forward = true
        ).single()
        val attachments =
            messages.readForward(peer, media.id)
                .flatMap { it.parts.filterIsInstance<QQNativeReceivedPart.Attachment>() }
        assertEquals(
            setOf(QQNativeMediaType.IMAGE, QQNativeMediaType.VOICE, QQNativeMediaType.VIDEO),
            attachments.map { it.type }.toSet()
        )
        for (part in attachments) {
            val connection = part.uri.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            try {
                assertEquals(200, connection.responseCode)
                assertTrue(connection.inputStream.use { it.read() != -1 })
            } finally {
                connection.disconnect()
            }
        }
        val original = messages.readForward(peer, report.id).first()
        // 引用真实会话消息
        val context = original.copy(receipt = report)
        val quoted =
            messages.send(peer, QQNativeOutgoingChain(QQNativeOutgoingPart.Text("Kotlin 引用验收")), replyTo = context)
                .single()
        val result = assertNotNull(messages.resolve(context, QQNativeReceivedPart.Reply(quoted.id, "", "", "")))
        val reply = result.parts.filterIsInstance<QQNativeReceivedPart.Reply>().single()
        assertEquals(report.id, messages.resolve(result, reply)?.receipt?.id)
    }
}
