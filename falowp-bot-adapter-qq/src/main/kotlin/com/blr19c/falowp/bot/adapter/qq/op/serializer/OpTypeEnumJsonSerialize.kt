package com.blr19c.falowp.bot.adapter.qq.op.serializer

import com.blr19c.falowp.bot.adapter.qq.op.OpTypeEnum
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * OpTypeEnum序列化时改为使用name
 */
class OpTypeEnumJsonSerialize : JsonSerializer<OpTypeEnum>() {

    override fun serialize(value: OpTypeEnum, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.name)
    }

}
