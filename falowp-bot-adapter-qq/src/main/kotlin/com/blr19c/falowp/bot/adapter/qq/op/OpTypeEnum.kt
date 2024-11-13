package com.blr19c.falowp.bot.adapter.qq.op

import com.blr19c.falowp.bot.adapter.qq.op.serializer.OpTypeEnumJsonDeserializer
import com.blr19c.falowp.bot.adapter.qq.op.serializer.OpTypeEnumJsonSerialize
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * 消息事件类型
 */
@JsonSerialize(using = OpTypeEnumJsonSerialize::class)
@JsonDeserialize(using = OpTypeEnumJsonDeserializer::class)
enum class OpTypeEnum {
    /**
     * QQ-用户单聊发消息给机器人时候
     */
    C2C_MESSAGE_CREATE,

    /**
     * QQ-用户添加使用机器人
     */
    FRIEND_ADD,

    /**
     * QQ-用户在群里@机器人时收到的消息
     */
    GROUP_AT_MESSAGE_CREATE,

    /**
     * QQ-机器人被添加到群聊
     */
    GROUP_ADD_ROBOT,


    /**
     * 频道-当收到@机器人的消息时
     */
    AT_MESSAGE_CREATE,

    /**
     * 频道-当频道的消息被删除时
     */
    PUBLIC_MESSAGE_DELETE,

    /**
     * 频道-当收到用户发给机器人的私信消息时
     */
    DIRECT_MESSAGE_CREATE,

    /**
     * 频道-删除（撤回）消息事件
     */
    DIRECT_MESSAGE_DELETE;

    /**
     * 来源为频道
     */
    fun isChannel(): Boolean {
        return when (this) {
            AT_MESSAGE_CREATE, PUBLIC_MESSAGE_DELETE, DIRECT_MESSAGE_CREATE, DIRECT_MESSAGE_DELETE -> true
            else -> false
        }
    }

    /**
     * 来源为QQ
     */
    fun isQQ(): Boolean {
        return when (this) {
            C2C_MESSAGE_CREATE, FRIEND_ADD, GROUP_AT_MESSAGE_CREATE, GROUP_ADD_ROBOT -> true
            else -> false
        }
    }

    /**
     * 是否为私聊
     */
    fun isDirect(): Boolean {
        return when (this) {
            DIRECT_MESSAGE_CREATE, C2C_MESSAGE_CREATE -> true
            else -> false
        }
    }

    /**
     * 是否为群聊
     */
    fun isGroup(): Boolean {
        return when (this) {
            AT_MESSAGE_CREATE, GROUP_AT_MESSAGE_CREATE -> true
            else -> false
        }
    }

    companion object {

        fun valueOfOption(type: String): OpTypeEnum? {
            return OpTypeEnum.entries.firstOrNull { it.name == type }
        }
    }
}