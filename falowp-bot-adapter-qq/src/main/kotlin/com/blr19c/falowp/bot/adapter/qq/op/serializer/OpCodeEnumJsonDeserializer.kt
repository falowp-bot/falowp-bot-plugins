package com.blr19c.falowp.bot.adapter.qq.op.serializer

import com.blr19c.falowp.bot.adapter.qq.op.OpCodeEnum
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

/**
 * OpCodeEnum反序列化时改为使用code
 */
class OpCodeEnumJsonDeserializer : ValueDeserializer<OpCodeEnum>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): OpCodeEnum {
        return OpCodeEnum.valueOfCode(p.intValue)
    }

}
