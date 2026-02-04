package com.blr19c.falowp.bot.adapter.qq.op.serializer

import com.blr19c.falowp.bot.adapter.qq.op.OpCodeEnum
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

/**
 * OpCodeEnum序列化时改为使用code
 */
class OpCodeEnumJsonSerialize : ValueSerializer<OpCodeEnum>() {

    override fun serialize(value: OpCodeEnum, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeNumber(value.code)
    }

}
