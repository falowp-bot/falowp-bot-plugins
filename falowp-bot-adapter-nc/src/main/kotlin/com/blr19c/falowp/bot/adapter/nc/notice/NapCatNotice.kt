package com.blr19c.falowp.bot.adapter.nc.notice

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatNoticeType
import com.blr19c.falowp.bot.adapter.nc.message.enums.NapCatRequestType
import com.blr19c.falowp.bot.system.Log
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.event.EventBotApi
import com.blr19c.falowp.bot.system.plugin.event.eventBotApi
import tools.jackson.databind.JsonNode

/**
 * 通知&事件管理
 */
object NapCatNotice {


    interface NapCatNoticeInterface : Log {

        suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event

    }

    /**
     * 处理事件
     */
    suspend fun processEvent(originalMessage: JsonNode) {
        val requestType = NapCatRequestType.fromValue(originalMessage.path("request_type").safeString())
        val event = requestType.clazz?.objectInstance?.toBotEvent(originalMessage)
        event?.let { eventBot(it).publishEvent(it) }
    }

    /**
     * 处理通知
     */
    suspend fun processNotice(originalMessage: JsonNode) {
        val napCatNoticeType = NapCatNoticeType.fromValue(originalMessage)
        val event = napCatNoticeType.clazz?.objectInstance?.toBotEvent(originalMessage)
        event?.let { eventBot(it).publishEvent(it) }
    }

    private suspend fun eventBot(event: Plugin.Listener.Event): EventBotApi {
        return NapCatBotApiSupport.tempBot.eventBotApi(event, ReceiveMessage.Adapter("NapCat", event))
    }

}
