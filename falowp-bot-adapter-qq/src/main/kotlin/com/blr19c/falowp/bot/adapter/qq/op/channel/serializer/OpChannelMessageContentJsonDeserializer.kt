package com.blr19c.falowp.bot.adapter.qq.op.channel.serializer

import com.blr19c.falowp.bot.adapter.qq.op.channel.OpChannelMessageContent
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * 频道消息内容反序列化
 */
class OpChannelMessageContentJsonDeserializer : JsonDeserializer<OpChannelMessageContent>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OpChannelMessageContent {
        return OpChannelMessageContent.of(p.text)
    }
}