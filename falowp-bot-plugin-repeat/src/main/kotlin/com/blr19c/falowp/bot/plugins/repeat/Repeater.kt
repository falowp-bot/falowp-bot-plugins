package com.blr19c.falowp.bot.plugins.repeat

import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.listener.events.NudgeEvent
import com.blr19c.falowp.bot.system.listener.events.SendMessageEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.event.eventListener
import com.blr19c.falowp.bot.system.plugin.message.MessageMatch
import com.blr19c.falowp.bot.system.plugin.message.queueMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 复读机
 */
@Plugin(
    name = "复读机",
    tag = "聊天",
    desc = "和大家一起复读"
)
class Repeater {

    private val mutex: Mutex = Mutex()
    private val historyMessage = LinkedHashMap<String, RepeaterData>()

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
        addMessage(this.receiveMessage.source, this.receiveMessage.content)
    }

    private val nudgeEventListener = eventListener<NudgeEvent> { event ->
        this.addMessage(event.source, NudgeSendMessage(event.target.id))
    }

    private val sendMessageEventListener = eventListener<SendMessageEvent> {
        clearMessage(this.receiveMessage.source.id)
    }

    private suspend fun BotApi.addMessage(source: ReceiveMessage.Source, content: Any) {
        val message = mutex.withLock {
            val old = historyMessage[source.id]
            val new = when {
                old == null || !equalsContent(old.content, content) -> RepeaterData(content)
                else -> old.copy(count = old.count + 1)
            }
            historyMessage[source.id] = new
            return@withLock new
        }
        if (message.count >= 3) {
            clearMessage(source.id)
            sendRepeater(source, message)
        }
    }

    private suspend fun BotApi.sendRepeater(source: ReceiveMessage.Source, new: RepeaterData) {
        val message = when (new.content) {
            is ReceiveMessage.Content -> SendMessage.builder()
                .text(new.content.message)
                .at(new.content.at.map { it.id })
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

    private suspend fun clearMessage(sourceId: String) = mutex.withLock {
        historyMessage.remove(sourceId)
    }

    private fun equalsContent(a: Any, b: Any) = when (a) {
        is ReceiveMessage.Content if b is ReceiveMessage.Content ->
            a.message == b.message && a.at == b.at && a.image == b.image && a.emoji == b.emoji

        is NudgeSendMessage if b is NudgeSendMessage -> a.id == b.id
        else -> false
    }

    init {
        repeater.register()
        nudgeEventListener.register()
        sendMessageEventListener.register()
    }

    private data class RepeaterData(val content: Any, val count: Int = 1)
}