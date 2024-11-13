package com.blr19c.falowp.bot.adapter.qq.op.qq

import com.blr19c.falowp.bot.adapter.qq.op.qq.serializer.OpQQMessageTypeEnumJsonDeserializer
import com.blr19c.falowp.bot.adapter.qq.op.qq.serializer.OpQQMessageTypeEnumJsonSerialize
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * QQ发送消息类型
 */
@JsonSerialize(using = OpQQMessageTypeEnumJsonSerialize::class)
@JsonDeserialize(using = OpQQMessageTypeEnumJsonDeserializer::class)
enum class OpQQMessageTypeEnum(val code: Int) {

    /**
     * 文本
     */
    TEXT(0),

    /**
     * 富媒体
     */
    MEDIA(7);

    companion object {
        fun valueOfCode(code: Int): OpQQMessageTypeEnum {
            return OpQQMessageTypeEnum.entries.first { it.code == code }
        }
    }
}