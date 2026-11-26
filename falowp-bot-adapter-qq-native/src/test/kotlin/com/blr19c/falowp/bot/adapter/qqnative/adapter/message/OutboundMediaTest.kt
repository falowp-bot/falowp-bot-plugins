package com.blr19c.falowp.bot.adapter.qqnative.adapter.message

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.system.api.ImageSendMessage
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.expand.toImageUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

class OutboundMediaTest {
    @Test
    fun nativePartitionSeparatesMessages(): Unit = runBlocking(Dispatchers.IO) {
        val chain = SendMessage.builder("签到汇总").partition().text("角色甲").build()
        OutboundMedia().use { media ->
            assertEquals(
                listOf(
                    QQNativeOutgoingPart.Text("签到汇总"),
                    QQNativeOutgoingPart.Partition,
                    QQNativeOutgoingPart.Text("角色甲")
                ),
                chain.messageList.map { media.encode(it) })
        }
    }

    @Test
    fun localImagesFromBothFalowpFileAndPathUrisKeepTheirOriginalFile(): Unit = runBlocking(Dispatchers.IO) {
        val file = Files.createTempFile("QQ 图片 ", ".png")
        try {
            ImageIO.write(BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB), "png", file.toFile())
            for (url in listOf(file.toFile().toImageUrl(), file.toUri().toImageUrl())) {
                OutboundMedia().use { media ->
                    val image = assertIs<QQNativeOutgoingPart.Image>(media.encode(ImageSendMessage(url)))
                    assertEquals(file, image.path)
                    assertEquals(2, image.width)
                    assertEquals(3, image.height)
                }
                assertTrue(Files.exists(file))
            }
        } finally {
            Files.deleteIfExists(file)
        }

    }
}
