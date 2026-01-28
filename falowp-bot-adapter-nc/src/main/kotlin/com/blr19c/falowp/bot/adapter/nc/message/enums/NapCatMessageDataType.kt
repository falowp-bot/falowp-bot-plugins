package com.blr19c.falowp.bot.adapter.nc.message.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 消息数据类型
 */
enum class NapCatMessageDataType(@get:JsonValue val value: String) {

    /**
     * 文本消息
     */
    TEXT("text"),

    /**
     * 图片消息
     */
    IMAGE("image"),

    /**
     * 互动表情消息
     */
    POKE("poke"),

    /**
     * 表情
     */
    FACE("face"),

    /**
     * at@ 消息
     */
    AT("at"),

    /**
     * 引用回复消息
     */
    REPLY("reply"),

    /**
     * 语音消息
     */
    RECORD("record"),

    /**
     * 视频消息
     */
    VIDEO("video"),

    /**
     * 文件消息
     */
    FILE("file"),

    /**
     * 合并转发
     */
    FORWARD("forward"),

    /**
     * json/卡片/分享消息
     */
    JSON("json"),

    /**
     * 骰子消息
     */
    DICE("dice"),

    /**
     * 猜拳消息
     */
    RPS("rps"),

    /**
     * 未知
     */
    UNKNOWN("unknown");

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): NapCatMessageDataType {
            return entries.firstOrNull { it.value == value } ?: UNKNOWN
        }

    }
}
