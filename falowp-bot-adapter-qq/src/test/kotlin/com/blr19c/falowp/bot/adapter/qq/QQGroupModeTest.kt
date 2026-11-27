package com.blr19c.falowp.bot.adapter.qq

import com.blr19c.falowp.bot.adapter.qq.core.config.QQGroupMessageMode

import com.blr19c.falowp.bot.adapter.qq.core.message.QQMessageConverter
import com.blr19c.falowp.bot.adapter.qq.api.QQBotApiSupport
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.Json
import kotlin.test.*

class QQGroupModeTest {
    private val self = BotSelf.Default("app")
    private fun event(
        type: String,
        content: String = "普通消息",
        mentions: List<Map<String, Any>> = emptyList(),
        seq: Int = 1
    ) =
        Json.readJsonNode(
            Json.toJsonString(
                mapOf(
                    "t" to type, "d" to mapOf(
                        "id" to "group-mode-message", "group_openid" to "mode-group", "msg_seq" to seq,
                        "author" to mapOf("member_openid" to "sender", "user_openid" to "sender"),
                        "content" to content, "mentions" to mentions
                    )
                )
            )
        )

    @Test
    fun `all accepts both group events without fabricating mentions`() {
        val plain =
            assertNotNull(QQMessageConverter.convert(event("GROUP_MESSAGE_CREATE"), self, QQGroupMessageMode.ALL))
        assertFalse(plain.atMe())
        val at =
            assertNotNull(QQMessageConverter.convert(event("GROUP_AT_MESSAGE_CREATE"), self, QQGroupMessageMode.ALL))
        assertTrue(at.atMe())
    }

    @Test
    fun `at filters ordinary group messages but accepts at events and private messages`() {
        assertNull(QQMessageConverter.convert(event("GROUP_MESSAGE_CREATE"), self, QQGroupMessageMode.AT))
        assertNotNull(QQMessageConverter.convert(event("GROUP_AT_MESSAGE_CREATE"), self, QQGroupMessageMode.AT))
        assertNotNull(QQMessageConverter.convert(event("C2C_MESSAGE_CREATE"), self, QQGroupMessageMode.AT))
        assertNull(
            QQMessageConverter.convert(
                event(
                    "GROUP_MESSAGE_CREATE",
                    mentions = listOf(mapOf("member_openid" to "other"))
                ), self, QQGroupMessageMode.AT
            )
        )
    }

    @Test
    fun `full event self mention normalizes command and preserves other mentions and URL`() {
        val event = event(
            "GROUP_MESSAGE_CREATE", " <@bot-openid> /签到 <@other> https://example.com/a/b ",
            listOf(mapOf("is_you" to true, "member_openid" to "bot-openid"), mapOf("member_openid" to "other"))
        )
        val message = assertNotNull(QQMessageConverter.convert(event, self, QQGroupMessageMode.AT))
        assertTrue(message.atMe())
        assertEquals("签到 <@other> https://example.com/a/b", message.content.message)
        assertEquals(setOf("app", "other"), message.content.at.map { it.id }.toSet())
    }

    @Test
    fun `mention by self id is accepted and duplicate mentions are collapsed`() {
        val mentions = listOf(mapOf<String, Any>("id" to "app"))
        val full = assertNotNull(
            QQMessageConverter.convert(
                event("GROUP_MESSAGE_CREATE", "<@app> /签到", mentions),
                self,
                QQGroupMessageMode.AT
            )
        )
        assertEquals("签到", full.content.message)
        val at = assertNotNull(QQMessageConverter.convert(event("GROUP_AT_MESSAGE_CREATE", mentions = mentions), self))
        assertEquals(1, at.content.at.size)
    }

    @Test
    fun `both event forms and retries share deduplication while sequence distinguishes messages`() {
        val received = mutableListOf<ReceiveMessage>()
        QQBotApiSupport.dispatchMessage(event("GROUP_AT_MESSAGE_CREATE"), received::add)
        QQBotApiSupport.dispatchMessage(event("GROUP_MESSAGE_CREATE"), received::add)
        QQBotApiSupport.dispatchMessage(event("GROUP_AT_MESSAGE_CREATE"), received::add)
        QQBotApiSupport.dispatchMessage(event("GROUP_MESSAGE_CREATE", seq = 2), received::add)
        assertEquals(2, received.size)
    }
}
