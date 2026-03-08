package com.blr19c.falowp.bot.adapter.nc.message.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 消息数据类型
 */
@Suppress("SpellCheckingInspection")
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
     * 商城表情
     */
    M_FACE("mface"),

    /**
     * 音乐
     */
    MUSIC("music"),

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
     * 在线文件
     */
    ONLINE_FILE("onlinefile"),

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
     * 拆分消息
     */
    SPLIT_INDEPENDENT("splitIndependent"),

    /**
     * 未知
     */
    UNKNOWN("unknown");

    /**
     * 是否独立消息
     */
    fun independent(): Boolean {
        return when (this) {
            TEXT, IMAGE, FACE, AT, REPLY -> false
            else -> true
        }
    }

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): NapCatMessageDataType {
            return entries.firstOrNull { it.value == value } ?: UNKNOWN
        }

    }
}
