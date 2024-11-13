package com.blr19c.falowp.bot.adapter.qq.op.qq.serializer

import com.blr19c.falowp.bot.adapter.qq.op.qq.OpQQMessageTypeEnum
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * OpQQMessageTypeEnum反序列化时改为使用code
 */
class OpQQMessageTypeEnumJsonDeserializer : JsonDeserializer<OpQQMessageTypeEnum>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OpQQMessageTypeEnum {
        return OpQQMessageTypeEnum.valueOfCode(p.intValue)
    }

}
