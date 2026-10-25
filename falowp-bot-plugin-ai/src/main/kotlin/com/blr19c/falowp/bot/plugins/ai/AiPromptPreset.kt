package com.blr19c.falowp.bot.plugins.ai

import com.blr19c.falowp.bot.system.pluginConfigProperty
import com.blr19c.falowp.bot.system.readPluginResource
import com.blr19c.falowp.bot.system.cache.CacheReference
import kotlin.time.Duration

/**
 * AI 系统提示词预设加载器
 */
internal object AiPromptPreset {

    private val cachedPrompt by CacheReference(Duration.INFINITE) { loadPrompt() }

    fun systemPrompt(): String = cachedPrompt

    private suspend fun loadPrompt(): String {
        val content = loadResource() ?: pluginConfigProperty("systemPrompt")

        return content.trim().also {
            require(it.isNotBlank()) { "AI系统提示词不能为空" }
        }
    }

    private suspend fun loadResource(): String? = try {
        readPluginResource(PRESET_FILE) { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    } catch (_: IllegalStateException) {
        null
    }

    private const val PRESET_FILE = "system-prompt.txt"
}
