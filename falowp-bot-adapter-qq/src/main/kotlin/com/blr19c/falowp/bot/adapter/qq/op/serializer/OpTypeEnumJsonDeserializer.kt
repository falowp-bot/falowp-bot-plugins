package com.blr19c.falowp.bot.adapter.qq.op.serializer

import com.blr19c.falowp.bot.adapter.qq.op.OpTypeEnum
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

/**
 * OpTypeEnum反序列化时改为使用name
 */
class OpTypeEnumJsonDeserializer : ValueDeserializer<OpTypeEnum>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OpTypeEnum {
        return OpTypeEnum.valueOf(p.string)
    }

}
