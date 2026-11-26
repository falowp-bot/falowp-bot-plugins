package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.QQNativeNoticeDetail
import tools.jackson.databind.json.JsonMapper
import kotlin.test.*

class MessageCodecTest {
    private val json = JsonMapper.builder().build()
    private val incoming = json.readTree(
        """
        {"source":"group","target":"42","peerUid":"42","id":"101","sender":"11","seq":"3","time":"99",
         "parts":[{"kind":"text","value":"hello"},{"kind":"reply","id":"100","seq":"2","clientSeq":"1","time":"98"}]}
    """.trimIndent()
    )

    @Test
    fun decodedMessagesPreserveQuoteLookupKeysAndWithdrawOriginal() {
        val message = incoming.message()
        assertEquals(QQNativeConversation.Group("42"), message.conversation)
        assertEquals(
            listOf(QQNativeReceivedPart.Text("hello"), QQNativeReceivedPart.Reply("100", "2", "1", "98")),
            message.parts
        )
        val event =
            json.readTree("""{"kind":"withdraw","actor":"11","source":"group","target":"42","original":$incoming}""")
        assertEquals(QQNativeNoticeDetail.Withdraw(message), event.notice()?.detail)
    }
}
