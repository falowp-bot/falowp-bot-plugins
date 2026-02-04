package com.blr19c.falowp.bot.adapter.qq.op.qq.serializer

import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQMessageTypeEnum
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

/**
 * OpQQMessageTypeEnum反序列化时改为使用code
 */
class OpQQMessageTypeEnumJsonDeserializer : ValueDeserializer<OpQQMessageTypeEnum>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OpQQMessageTypeEnum {
        return OpQQMessageTypeEnum.valueOfCode(p.intValue)
    }

}
