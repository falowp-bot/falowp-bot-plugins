package com.blr19c.falowp.bot.plugins.ai

import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams
import com.blr19c.falowp.bot.plugins.ai.database.AiChatHistory
import com.blr19c.falowp.bot.system.pluginConfigProperty

internal class AiChatSession(
    private val sourceId: String,
    private val userId: String,
) {
    val id: String = "$sourceId:$userId"
    var pluginInvoked: Boolean = false
        private set
    private var pluginName: String? = null

    fun prompt(): Prompt {
        val history = AiChatHistory.query(sourceId, userId, contextMaxMessages())
        return prompt(
            id = "chat",
            params = LLMParams(
                temperature = pluginConfigProperty("temperature") { "" }.toDoubleOrNull(),
                numberOfChoices = 1,
            )
        ) {
            system(pluginConfigProperty("systemPrompt"))
            history.forEach {
                when (it.role) {
                    ROLE_USER -> user(it.content)
                    ROLE_ASSISTANT -> assistant(it.content)
                }
            }
        }
    }

    fun appendAnswer(question: String, answer: String) {
        append(question, answer)
    }

    fun markPluginInvoked(pluginName: String) {
        pluginInvoked = true
        this.pluginName = pluginName
    }

    fun appendPluginTransfer(question: String) {
        append(question, "已转接到${pluginName ?: "消息"}插件。")
    }

    private fun append(question: String, answer: String) {
        val maxChars = contextMaxMessageChars()
        AiChatHistory.append(sourceId, userId, ROLE_USER, question.take(maxChars))
        AiChatHistory.append(sourceId, userId, ROLE_ASSISTANT, answer.take(maxChars))
        AiChatHistory.trim(sourceId, userId, contextMaxMessages())
    }

    private fun contextMaxMessages(): Int {
        return contextMaxTurns() * 2
    }

    private fun contextMaxTurns(): Int {
        return pluginConfigProperty("contextMaxTurns") { "" }.toIntOrNull()?.coerceAtLeast(0) ?: 10
    }

    private fun contextMaxMessageChars(): Int {
        return pluginConfigProperty("contextMaxMessageChars") { "" }.toIntOrNull()?.coerceAtLeast(100) ?: 800
    }

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
