package com.blr19c.falowp.bot.repeat.plugins.repeat

import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.listener.events.SendMessageEvent
import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Event.Companion.eventListener
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Hook.Companion.afterFinallyHook
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

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
    private val executor = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val historyMessage = LinkedHashMap<String, RepeaterData>()

    private fun equalsContent(content1: ReceiveMessage.Content, content2: ReceiveMessage.Content) = runBlocking {
        val image1 = content1.image.map { it.toSummaryBytes() }.toList()
        val image2 = content2.image.map { it.toSummaryBytes() }.toList()
        content1.at.map { it.id }.toList() == content2.at.map { it.id }.toList()
                && content1.message == content2.message
                && image1 == image2
    }

    private fun buildReplyMessage(content: ReceiveMessage.Content): SendMessageChain {
        if (Random.nextDouble(0.0, 1.0) < 0.8) {
            val message = if (content.message.contains("打断施法"))
                "打断!".plus(content.message) else content.message
            return SendMessage.builder()
                .at(content.at.map { it.id })
                .image(content.image.map { it.info })
                .text(message)
                .build()
        }
        val replyMessage = "打断施法"
        val interruptMessage = if (content.message.contains(replyMessage))
            "打断!".plus(content.message) else replyMessage.plus("!")
        return SendMessage.builder(interruptMessage).build()
    }

    private suspend fun addMessage(botApi: BotApi, originalMessage: RepeaterData, sourceId: String) = mutex.withLock {
        val repeaterData = historyMessage.compute(sourceId) { _, oldRepeaterData ->
            var newRepeaterData = originalMessage
            oldRepeaterData ?: return@compute newRepeaterData
            if (!equalsContent(newRepeaterData.content, oldRepeaterData.content)) {
                return@compute newRepeaterData
            }
            newRepeaterData = oldRepeaterData.copy(count = oldRepeaterData.count + 1)
            if (newRepeaterData.count < 0) {
                newRepeaterData = oldRepeaterData.copy(repeat = true)
            }
            if (newRepeaterData.count >= 3) {
                newRepeaterData = newRepeaterData.copy(count = Int.MIN_VALUE)
            }
            newRepeaterData
        } ?: return
        if (repeaterData.count < 0 && !repeaterData.repeat) {
            botApi.sendReply(buildReplyMessage(repeaterData.content))
        }
    }

    private val repeater = afterFinallyHook<ReceiveMessageHook>(order = Int.MIN_VALUE) { (receiveMessage) ->
        val botApi = this.botApi()
        executor.launch {
            addMessage(botApi, RepeaterData(receiveMessage.content), receiveMessage.source.id)
        }
    }

    private val sendMessageEvent = eventListener<SendMessageEvent> { (sendMessageList, _, forward) ->
        if (forward || this.receiveMessage.source.id.isBlank()) return@eventListener
        for (sendMessageChain in sendMessageList) {
            val atList = sendMessageChain.messageList.filterIsInstance<AtSendMessage>()
                .map { ReceiveMessage.User.empty().copy(id = it.at) }
                .toList()
            val imageList = sendMessageChain.messageList.filterIsInstance<ImageSendMessage>()
                .map { it.image }
                .toList()
            val text = sendMessageChain.messageList.filterIsInstance<TextSendMessage>().joinToString { it.content }
            val content = ReceiveMessage.Content(text, atList, imageList, emptyList()) { null }
            addMessage(this, RepeaterData(content, repeat = true), this.receiveMessage.source.id)
        }
    }

    init {
        repeater.register()
        sendMessageEvent.register()
    }

    data class RepeaterData(val content: ReceiveMessage.Content, val count: Int = 1, val repeat: Boolean = false)
}