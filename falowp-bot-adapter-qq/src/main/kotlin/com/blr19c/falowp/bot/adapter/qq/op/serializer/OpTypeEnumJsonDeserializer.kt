package com.blr19c.falowp.bot.adapter.qq.op.serializer

import com.blr19c.falowp.bot.adapter.qq.op.OpTypeEnum
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * OpTypeEnum反序列化时改为使用name
 */
class OpTypeEnumJsonDeserializer : JsonDeserializer<OpTypeEnum>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OpTypeEnum {
        return OpTypeEnum.valueOf(p.text)
    }

}
