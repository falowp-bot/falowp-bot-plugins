package com.blr19c.falowp.bot.adapter.nc.message.enums

import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 消息类型
 */
enum class NapCatMessageType(@get:JsonValue val value: String) {

    /**
     * 私聊
     */
    PRIVATE("private"),

    /**
     * 群聊
     */
    GROUP("group"),

    /**
     * 未知
     */
    UNKNOWN("unknown");


    fun toSourceType(): SourceTypeEnum {
        return when (this) {
            PRIVATE -> SourceTypeEnum.PRIVATE
            GROUP -> SourceTypeEnum.GROUP
            else -> SourceTypeEnum.UNKNOWN
        }
    }

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): NapCatMessageType {
            return entries.firstOrNull { it.value == value } ?: UNKNOWN
        }

    }
}
