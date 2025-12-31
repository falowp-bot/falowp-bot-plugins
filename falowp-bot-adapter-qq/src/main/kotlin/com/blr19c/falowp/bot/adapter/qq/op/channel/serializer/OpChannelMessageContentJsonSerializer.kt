package com.blr19c.falowp.bot.adapter.qq.op.channel.serializer

import com.blr19c.falowp.bot.adapter.qq.op.channel.OpChannelMessageContent
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

/**
 * 频道消息内容序列化
 */
class OpChannelMessageContentJsonSerializer : ValueSerializer<OpChannelMessageContent>() {

    override fun serialize(value: OpChannelMessageContent, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeString(value.fullMessage())
    }
}