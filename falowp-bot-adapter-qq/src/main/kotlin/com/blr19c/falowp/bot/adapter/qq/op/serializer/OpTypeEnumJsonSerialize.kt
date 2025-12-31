package com.blr19c.falowp.bot.adapter.qq.op.serializer

import com.blr19c.falowp.bot.adapter.qq.op.OpTypeEnum
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

/**
 * OpTypeEnum序列化时改为使用name
 */
class OpTypeEnumJsonSerialize : ValueSerializer<OpTypeEnum>() {

    override fun serialize(value: OpTypeEnum, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeString(value.name)
    }

}
