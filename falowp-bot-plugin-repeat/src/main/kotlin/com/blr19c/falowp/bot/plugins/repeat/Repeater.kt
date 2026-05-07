package com.blr19c.falowp.bot.plugins.repeat

import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.listener.events.NudgeEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.event.eventListener
import com.blr19c.falowp.bot.system.plugin.message.MessageMatch
import com.blr19c.falowp.bot.system.plugin.message.queueMessage
import java.util.concurrent.ConcurrentHashMap

/**
 * 复读机
 */
@Plugin(
    name = "复读机",
    tag = "聊天",
    desc = "和大家一起复读"
)
class Repeater {

    private val historyMessage = ConcurrentHashMap<String, RepeaterData>()

    private val repeater = queueMessage(
        match = MessageMatch.allMatch(),
        terminateEvent = false,
        onSuccess = {},
        onOverFlow = {}
    ) {
        if (this.receiveMessage.messageType != MessageTypeEnum.MESSAGE) {
            clearMessage(this.receiveMessage.source.id)
            return@queueMessage
        }
        addMessage(
            this.receiveMessage.source,
            this.receiveMessage.content,
            this.receiveMessage.content.toSummary(),
        )
    }

    private val nudgeEventListener = eventListener<NudgeEvent> { event ->
        this.addMessage(event.source, NudgeSendMessage(event.target.id), RepeatSummary.Nudge(event.target.id))
    }

    private suspend fun BotApi.addMessage(source: ReceiveMessage.Source, content: Any, summary: RepeatSummary) {
        var sendMessage: RepeaterData? = null
        historyMessage.compute(source.id) { _, old ->
            var new = when {
                old == null || (old.summary != summary) -> RepeaterData(summary, content)
                else -> old.copy(count = old.count + 1)
            }
            if (new.count >= 2 && !new.repeat) {
                sendMessage = new
                new = new.copy(repeat = true)
            }
            return@compute new
        }
        sendMessage?.let { sendRepeater(source, it) }
    }

    private suspend fun BotApi.sendRepeater(source: ReceiveMessage.Source, new: RepeaterData) {
        val message = when (new.content) {
            is ReceiveMessage.Content -> SendMessage.builder()
                .at(new.content.at.map { it.id })
                .text(new.content.message)
                .image(new.content.image.map { it.info })
                .emoji(new.content.emoji.map { EmojiSendMessage(it.id, it.type, it.display) })
                .build()

            is NudgeSendMessage -> SendMessage.builder()
                .nudge(new.content.id)
                .build()

            else -> return
        }
        if (source.type == SourceTypeEnum.GROUP) {
            this.sendGroup(message, sourceId = source.id)
        }
        if (source.type == SourceTypeEnum.PRIVATE) {
            this.sendPrivate(message, sourceId = source.id)
        }
    }

    private suspend fun ReceiveMessage.Content.toSummary(): RepeatSummary.Message {
        return RepeatSummary.Message(
            message = this.message,
            at = this.at.map { it.id },
            emoji = this.emoji.map { it.id },
            image = this.image.map { it.toSummary() }
        )
    }

    private fun clearMessage(sourceId: String) {
        historyMessage.remove(sourceId)
    }

    init {
        repeater.register()
        nudgeEventListener.register()
    }

    private sealed interface RepeatSummary {
        data class Message(
            val message: String,
            val at: List<String>,
            val emoji: List<String>,
            val image: List<String>
        ) : RepeatSummary

        data class Nudge(val id: String) : RepeatSummary
    }

    private data class RepeaterData(
        val summary: RepeatSummary,
        val content: Any,
        val count: Int = 1,
        val repeat: Boolean = false
    )
}
