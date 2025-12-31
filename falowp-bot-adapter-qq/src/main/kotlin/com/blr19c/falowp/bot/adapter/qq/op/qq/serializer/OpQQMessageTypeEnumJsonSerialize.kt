package com.blr19c.falowp.bot.adapter.qq.op.qq.serializer

import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQMessageTypeEnum
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

/**
 * OpQQMessageTypeEnum序列化时改为使用code
 */
class OpQQMessageTypeEnumJsonSerialize : ValueSerializer<OpQQMessageTypeEnum>() {

    override fun serialize(value: OpQQMessageTypeEnum, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeNumber(value.code)
    }

}
