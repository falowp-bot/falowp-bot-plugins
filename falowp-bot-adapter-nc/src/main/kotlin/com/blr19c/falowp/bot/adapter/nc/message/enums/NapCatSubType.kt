package com.blr19c.falowp.bot.adapter.nc.message.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 消息子类型
 */
enum class NapCatSubType(@get:JsonValue val value: String) {
    /**
     * 戳一戳
     */
    POKE("poke"),

    /**
     * 朋友
     */
    FRIEND("friend"),

    /**
     * 群聊
     */
    GROUP("group"),

    /**
     * 未知
     */
    UNKNOWN("normal");

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): NapCatSubType {
           return entries.firstOrNull { it.value == value } ?: UNKNOWN
        }

    }
}
