package com.blr19c.falowp.bot.adapter.nc.message.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 事件类型
 */
enum class NapCatPostType(@get:JsonValue val value: String) {

    /**
     * meta_event-元事件
     */
    META_EVENT("meta_event"),

    /**
     * request-请求事件
     */
    REQUEST("request"),

    /**
     * notice-通知事件
     */
    NOTICE("notice"),

    /**
     * message-消息事件
     */
    MESSAGE("message"),

    /**
     * message_sent-自身消息
     */
    MESSAGE_SENT("message_sent"),

    /**
     * unknown-未知事件类型
     */
    UNKNOWN("unknown");

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): NapCatPostType {
            return entries.firstOrNull { it.value == value } ?: UNKNOWN
        }

    }

    fun negligible(): Boolean {
        return this == UNKNOWN || this == META_EVENT
    }
}
