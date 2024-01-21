package com.blr19c.falowp.bot.repeat.plugins.repeat

import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.image.ImageUrl
import com.blr19c.falowp.bot.system.image.encodeToBase64String
import com.blr19c.falowp.bot.system.listener.events.SendMessageEvent
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Event.Companion.eventListener
import com.blr19c.falowp.bot.system.plugin.Plugin.Message.message
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
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
    private val historyMessage = ConcurrentHashMap<String, LinkedBlockingQueue<ReceiveMessage>>()

    private suspend fun convertMessage(originalMessage: ReceiveMessage): ReceiveMessage {
        val base64ImageList = originalMessage.content.image
            .map { ImageUrl(it.toSummaryBytes().encodeToBase64String()) }
            .toList()
        return originalMessage.copy(content = originalMessage.content.copy(image = base64ImageList))
    }


    private suspend fun addMessage(originalMessage: ReceiveMessage) {
        val message = convertMessage(originalMessage)
        historyMessage.compute(message.source.id) { _, queue ->
            val newQueue = queue ?: LinkedBlockingQueue<ReceiveMessage>(7)
            while (newQueue.size >= 7) newQueue.poll()
            newQueue.offer(message)
            newQueue
        }
    }

    private suspend fun duplicateMessage(originalMessage: ReceiveMessage, duplicateCount: Int = 3): ReceiveMessage? {
        val message = convertMessage(originalMessage)
        val messages = historyMessage[message.source.id] ?: return null
        val iterator = messages.iterator()
        if (!iterator.hasNext()) return null
        var consecutiveCount = 1
        var current = iterator.next()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (current.sender.id != current.self.id
                && next.sender.id != next.self.id
                && current.content == next.content
                && message.content == next.content
                && messages.none { it.content == current.content && it.sender.id == current.self.id }
            ) {
                consecutiveCount++
            } else {
                consecutiveCount = 1
            }
            if (consecutiveCount >= duplicateCount) {
                return next
            }
            current = next
        }
        return null
    }

    private fun buildReplyMessage(duplicateMessage: ReceiveMessage): SendMessage {
        val content = duplicateMessage.content
        if (Random.nextDouble(0.0, 1.0) < 0.8) {
            val message = if (content.message.contains("打断施法"))
                "打断!".plus(content.message) else content.message
            return SendMessage.builder()
                .at(content.at.map { it.id })
                .images(content.image.map { it.info })
                .content(message)
                .build()
        }
        val replyMessage = "打断施法"
        val interruptMessage = if (content.message.contains(replyMessage))
            "打断!".plus(content.message) else replyMessage.plus("!")
        return SendMessage.builder(interruptMessage).build()
    }

    private val repeater = message(order = Int.MIN_VALUE, terminateEvent = false) {
        addMessage(this.receiveMessage)
        val duplicateMessage = duplicateMessage(this.receiveMessage) ?: return@message
        this.sendReply(buildReplyMessage(duplicateMessage))
    }

    private val sendMessageEvent = eventListener<SendMessageEvent> {
        if (it.forward) return@eventListener
        for (sendMessage in it.sendMessage) {
            val sender = this.receiveMessage.sender.copy(id = this.receiveMessage.self.id)
            val at = sendMessage.at.map { at -> ReceiveMessage.User.empty().copy(id = at) }
            val content = ReceiveMessage.Content(sendMessage.content, at, sendMessage.images, emptyList())
            val receiveMessage = this.receiveMessage.copy(sender = sender, content = content)
            addMessage(receiveMessage)
        }
    }

    init {
        repeater.register()
        sendMessageEvent.register()
    }
}