package com.blr19c.falowp.bot.adapter.nc.message.enums

import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.handlers.NapCatFriendAddNotice
import com.blr19c.falowp.bot.adapter.nc.notice.handlers.NapCatGroupAddNotice
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import kotlin.reflect.KClass

/**
 * 事件请求类型
 */
enum class NapCatRequestType(
    @get:JsonValue val value: String,
    val clazz: KClass<out NapCatNotice.NapCatNoticeInterface>?
) {

    /**
     * 加好友请求
     */
    FRIEND("friend", NapCatFriendAddNotice::class),

    /**
     * 加群请求
     */
    GROUP("group", NapCatGroupAddNotice::class),

    /**
     * 邀请登录号入群
     */
    GROUP_INVITE("group.invite", null),

    /**
     * 未知
     */
    UNKNOWN("unknown", null);

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): NapCatRequestType {
            return NapCatRequestType.entries.firstOrNull { it.value == value } ?: UNKNOWN
        }
    }

}
