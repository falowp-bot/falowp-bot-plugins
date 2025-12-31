package com.blr19c.falowp.bot.plugins.wordcloud

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.plugins.wordcloud.database.WordCloudTextInfo
import com.blr19c.falowp.bot.plugins.wordcloud.event.WordCloudEvent
import com.blr19c.falowp.bot.plugins.wordcloud.hook.WordCloudSegmentHook
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.listener.events.GreetingEvent
import com.blr19c.falowp.bot.system.listener.events.SendMessageEvent
import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Event.Companion.eventListener
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Hook.Companion.beforeHook
import com.blr19c.falowp.bot.system.plugin.hook.withPluginHook
import com.blr19c.falowp.bot.system.pluginConfigListProperty
import com.blr19c.falowp.bot.system.readPluginResource
import com.blr19c.falowp.bot.system.web.htmlToImageBase64
import com.hankcs.hanlp.HanLP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.insert
import org.jsoup.Jsoup
import java.time.LocalDate

/**
 * 词云
 */
@Plugin(name = "词云", desc = "在晚安事件时生成今日词云")
class WordCloud {

    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val greetingEvent = eventListener<GreetingEvent> { (_, goodNight) ->
        if (!goodNight) return@eventListener
        this.publishEvent(WordCloudEvent())
    }

    private val wordCloud = eventListener<WordCloudEvent> {
        this.generateWordCloud(it.date)
    }


    private val receiveMessage = beforeHook<ReceiveMessageHook> { (receiveMessage) ->
        if (receiveMessage.content.message.isBlank()) {
            return@beforeHook
        }
        executor.launch {
            addMessage(
                receiveMessage.content.message,
                receiveMessage.sender.id,
                receiveMessage.source.id,
                receiveMessage.source.type
            )
        }
    }

    private val sendMessage = eventListener<SendMessageEvent> { (sendMessage) ->
        for (textSendMessage in sendMessage.flatMap { it.messageList }.filterIsInstance<TextSendMessage>()) {
            if (this@eventListener.receiveMessage.sender.id.isBlank()) continue
            if (this@eventListener.receiveMessage.source.id.isBlank()) continue
            if (textSendMessage.content.isBlank()) continue
            addMessage(
                textSendMessage.content,
                this@eventListener.receiveMessage.sender.id,
                this@eventListener.receiveMessage.source.id,
                this@eventListener.receiveMessage.source.type
            )
        }
    }

    private suspend fun BotApi.generateWordCloud(date: LocalDate) {
        val html = readPluginResource("wordCloud.html") { inputStream ->
            inputStream.bufferedReader().use { Jsoup.parse(it.readText()) }
        }
        for (sourceId in WordCloudTextInfo.queryAllSourceId(date)) {

            val segmentCountMap = mutableMapOf<String, Int>()
            lateinit var sourceType: String

            for (wordCloudTextInfoVo in WordCloudTextInfo.queryBySourceId(sourceId, date)) {
                sourceType = wordCloudTextInfoVo.sourceType
                HanLP.segment(wordCloudTextInfoVo.text)
                    .map { it.word.replace(Regex("[\\pP\\p{Punct}]"), "") }
                    .filter { it.isNotBlank() }
                    .forEach { segmentCountMap.compute(it) { _, v -> v?.plus(1) ?: 1 } }
            }

            if (segmentCountMap.isEmpty()) continue

            withPluginHook(this, WordCloudSegmentHook(segmentCountMap)) {
                val blockWords = pluginConfigListProperty("blockWords")
                blockWords.forEach { segmentCountMap.remove(it) }
            }

            val wordCloudData = Json.toJsonString(segmentCountMap.toList().map { listOf(it.first, it.second) }.toList())
            html.select("#wordCloudData").`val`(wordCloudData)
            val image = htmlToImageBase64(html.html(), "#canvas")
            this.send(sourceId, sourceType, SendMessage.builder("今日词云").image(image).build())
        }
    }


    private fun addMessage(text: String, userId: String, sourceId: String, sourceType: SourceTypeEnum) {
        multiTransaction {
            WordCloudTextInfo.insert {
                it[WordCloudTextInfo.text] = text
                it[WordCloudTextInfo.userId] = userId
                it[WordCloudTextInfo.sourceId] = sourceId
                it[WordCloudTextInfo.sourceType] = sourceType.name
                it[createDate] = LocalDate.now()
            }
        }
    }

    private suspend fun BotApi.send(
        sourceId: String,
        sourceType: String,
        sendMessageChain: SendMessageChain
    ) {
        if (sourceType == SourceTypeEnum.PRIVATE.name) {
            this.sendPrivate(sendMessageChain, sourceId = sourceId)
        }
        if (sourceType == SourceTypeEnum.GROUP.name) {
            this.sendGroup(sendMessageChain, sourceId = sourceId)
        }
    }

    init {
        greetingEvent.register()
        wordCloud.register()
        receiveMessage.register()
        sendMessage.register()
    }

}