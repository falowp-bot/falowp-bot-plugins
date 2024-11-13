package com.blr19c.falowp.bot.adapter.qq.op.channel.serializer

import com.blr19c.falowp.bot.adapter.qq.op.channel.OpChannelMessageContent
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * 频道消息内容序列化
 */
class OpChannelMessageContentJsonSerializer : JsonSerializer<OpChannelMessageContent>() {
    override fun serialize(value: OpChannelMessageContent, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.fullMessage())
    }
}