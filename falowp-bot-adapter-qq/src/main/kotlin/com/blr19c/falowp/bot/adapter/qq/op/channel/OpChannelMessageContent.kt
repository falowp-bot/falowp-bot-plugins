package com.blr19c.falowp.bot.adapter.qq.op.channel

import com.blr19c.falowp.bot.adapter.qq.op.channel.serializer.OpChannelMessageContentJsonDeserializer
import com.blr19c.falowp.bot.adapter.qq.op.channel.serializer.OpChannelMessageContentJsonSerializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * 频道消息内容
 */
@JsonSerialize(using = OpChannelMessageContentJsonSerializer::class)
@JsonDeserialize(using = OpChannelMessageContentJsonDeserializer::class)
data class OpChannelMessageContent(
    val message: String,
    val at: List<String>,
    val channel: List<String>
) {

    fun fullMessage(): String {
        return channel.joinToString { "<#$it>" }
            .plus(" ")
            .plus(at.joinToString { "<@!$it>" })
            .plus(" $message")
    }

    companion object {
        fun of(data: String): OpChannelMessageContent {
            val atRegex = """<@!?(\d+)>""".toRegex()
            val channelRegex = """<#(\d+)>""".toRegex()
            val atMatches = atRegex.findAll(data)
            val channelMatches = channelRegex.findAll(data)
            val atList = atMatches.map { it.groupValues[1] }.toList()
            val channelList = channelMatches.map { it.groupValues[1] }.toList()
            val message = data.replace(atRegex, "").replace(channelRegex, "")
            return OpChannelMessageContent(message, atList, channelList)
        }
    }
}