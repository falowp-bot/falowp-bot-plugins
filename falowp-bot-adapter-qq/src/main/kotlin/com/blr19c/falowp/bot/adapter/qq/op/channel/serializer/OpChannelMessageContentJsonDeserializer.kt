package com.blr19c.falowp.bot.adapter.qq.op.channel.serializer

import com.blr19c.falowp.bot.adapter.qq.op.channel.OpChannelMessageContent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

/**
 * 频道消息内容反序列化
 */
class OpChannelMessageContentJsonDeserializer : ValueDeserializer<OpChannelMessageContent>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OpChannelMessageContent {
        return OpChannelMessageContent.of(p.string)
    }
}