package com.blr19c.falowp.bot.plugins.ai

import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.message.MessageMatch
import com.blr19c.falowp.bot.system.plugin.message.queueMessage
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration

/**
 * ai聊天
 */
@Plugin(
    name = "AI",
    tag = "聊天",
    desc = """
        <p>@机器人即可聊天</p>
    """
)
class AiChat {

    private val chat = queueMessage(match = MessageMatch(atMe = true), order = Int.MAX_VALUE, onSuccess = {}) {
        val question = this.receiveMessage.content.message
        if (question.isBlank()) return@queueMessage
        val session = AiChatSession(this.receiveMessage.source.id, this.receiveMessage.sender.id)
        val res = AiChatAgentFactory
            .build(this, session, session.prompt(), this@AiChat::class)
            .run(question, session.id)
            .trim()
        if (!session.pluginInvoked && res.isNotBlank()) {
            this.sendReply(res)
            session.appendAnswer(question, res)
        } else if (session.pluginInvoked) {
            session.appendPluginTransfer(question)
        }
    }

    init {
        KotlinLoggingConfiguration.logStartupMessage = false
        chat.register()
    }
}
