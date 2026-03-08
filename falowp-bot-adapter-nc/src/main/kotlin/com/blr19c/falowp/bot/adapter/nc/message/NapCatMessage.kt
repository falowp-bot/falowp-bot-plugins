package com.blr19c.falowp.bot.adapter.nc.message

import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageDataType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatMessageType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatPostType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatSubType
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * NapCat 原生消息
 */
data class NapCatMessage(
    /**
     * 消息ID
     */
    @field:JsonProperty("message_id")
    val messageId: String,
    /**
     * 事件类型
     */
    @field:JsonProperty("post_type")
    val postType: NapCatPostType,
    /**
     * 消息类型
     */
    @field:JsonProperty("message_type")
    val messageType: NapCatMessageType,
    /**
     * 消息子类型
     */
    @field:JsonProperty("sub_type")
    val subType: NapCatSubType,
    /**
     * 自身QQ号
     */
    @field:JsonProperty("self_id")
    val selfId: String,
    /**
     * 发送人QQ号
     */
    @field:JsonProperty("user_id")
    val userId: String,
    /**
     * 群聊ID
     */
    @field:JsonProperty("group_id")
    val groupId: String?,
    /**
     * 时间
     */
    @field:JsonProperty("time")
    val time: Long,
    /**
     * 消息序列号同消息ID
     */
    @field:JsonProperty("message_seq")
    val messageSeq: String,
    /**
     * 同消息ID
     */
    @field:JsonProperty("real_id")
    val realId: String,
    /**
     * 未知
     */
    @field:JsonProperty("real_seq")
    val realSeq: String,
    /**
     * 发送人信息
     */
    @field:JsonProperty("sender")
    val sender: Sender,
    /**
     * 原消息内容
     */
    @field:JsonProperty("raw_message")
    val rawMessage: String,
    /**
     * 字体大小
     */
    @field:JsonProperty("font")
    val font: Int,
    /**
     * 消息内容
     */
    @field:JsonProperty("message")
    val message: List<Message>,
    /**
     * 消息格式化类型
     * array-数组
     * string-字符串
     */
    @field:JsonProperty("message_format")
    val messageFormat: String,
    /**
     * 目标人QQ号
     */
    @field:JsonProperty("target_id")
    val targetId: String?
) {

    companion object {

        fun empty(): NapCatMessage {
            return NapCatMessage(
                "0",
                NapCatPostType.UNKNOWN,
                NapCatMessageType.UNKNOWN,
                NapCatSubType.UNKNOWN,
                "0",
                "0",
                "0",
                0,
                "0",
                "0",
                "0",
                Sender("0", "", ""),
                "",
                14,
                emptyList(),
                "array",
                null
            )
        }
    }

    data class Sender(
        /**
         * 发送人QQ号
         */
        @field:JsonProperty("user_id")
        val userId: String,
        /**
         * 发送人昵称
         */
        @field:JsonProperty("nickname")
        val nickname: String,
        /**
         * 发送人备注
         */
        @field:JsonProperty("card")
        val card: String
    )

    data class Message(
        /**
         * 消息类型
         */
        @field:JsonProperty("type")
        val type: NapCatMessageDataType,
        /**
         * 消息内容
         */
        @field:JsonProperty("data")
        val data: MessageData
    )

    data class MessageData(
        /**
         * 文本内容
         */
        @field:JsonProperty("text")
        val text: String? = null,
        /**
         * 如果是表情就是表情ID
         * 如果是合并转发就是转发消息ID
         * 如果是引用/回复就是引用的消息ID
         */
        @field:JsonProperty("id")
        @field:JsonAlias("msgId")
        val id: String? = null,
        /**
         * 如果是互动表情就是互动表情类型
         */
        @field:JsonProperty("type")
        val type: String? = null,
        /**
         * 如果是at消息就是QQ
         */
        @field:JsonProperty("qq")
        val qq: String? = null,
        /**
         * 名字/文件名
         */
        @field:JsonProperty("name")
        val name: String? = null,
        /**
         * 文件
         */
        @field:JsonProperty("file")
        @field:JsonAlias("fileName")
        val file: String? = null,
        /**
         * 文件ID
         */
        @field:JsonProperty("file_id")
        @field:JsonAlias("elementId")
        val fileId: String? = null,
        /**
         * 文件大小
         */
        @field:JsonProperty("file_size")
        @field:JsonAlias("fileSize")
        val fileSize: Long? = null,
        /**
         * 文件链接
         */
        @field:JsonProperty("url")
        val url: String? = null,
        /**
         * 如果是骰子消息就是骰子的点数结果 1-6
         * 如果是猜拳就是猜拳结果 1-石头 2-剪刀 3-布
         */
        @field:JsonProperty("result")
        val result: String? = null,
        /**
         * 如果是商城表情就是表情大概描述
         */
        @field:JsonProperty("summary")
        val summary: String? = null,
        /**
         * 如果是商城表情就是表情的key
         */
        @field:JsonProperty("key")
        val key: String? = null,
        /**
         * 如果是商城表情就是表情ID
         */
        @field:JsonProperty("emoji_id")
        val emojiId: String? = null,
        /**
         * 如果是商城表情就是整个表情包的ID
         */
        @field:JsonProperty("emoji_package_id")
        val emojiPackageId: String? = null,
        /**
         * 原始数据 如果有
         * 由于 NapCat 设计问题 这里可能是一个StringNode 需要 unwrap
         */
        @field:JsonProperty("data")
        @field:JsonAlias("raw")
        val data: JsonNode? = null,
        /**
         * 在线文件时标注是否是文件夹
         */
        val isDir: Boolean? = null
    )
}