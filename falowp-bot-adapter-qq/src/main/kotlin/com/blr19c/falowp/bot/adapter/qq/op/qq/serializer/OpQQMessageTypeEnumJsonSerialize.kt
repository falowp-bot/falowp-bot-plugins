package com.blr19c.falowp.bot.adapter.qq.op.qq.serializer

import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQMessageTypeEnum
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * OpQQMessageTypeEnum序列化时改为使用code
 */
class OpQQMessageTypeEnumJsonSerialize : JsonSerializer<OpQQMessageTypeEnum>() {

    override fun serialize(value: OpQQMessageTypeEnum, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.code)
    }

}
