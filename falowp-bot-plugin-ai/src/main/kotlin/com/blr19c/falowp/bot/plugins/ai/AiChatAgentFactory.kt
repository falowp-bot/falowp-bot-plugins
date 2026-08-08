package com.blr19c.falowp.bot.plugins.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.serialization.typeToken
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.plugin.message.MessagePluginInfo
import com.blr19c.falowp.bot.system.pluginConfigProperty
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

internal object AiChatAgentFactory {
    fun build(
        botApi: BotApi,
        session: AiChatSession,
        prompt: Prompt,
        selfClass: KClass<*>,
    ): AIAgent<String, String> {
        return AIAgent(
            promptExecutor = promptExecutor(),
            agentConfig = agentConfig(prompt),
            toolRegistry = toolRegistry(botApi, session, selfClass),
        )
    }

    private fun agentConfig(prompt: Prompt): AIAgentConfig {
        return AIAgentConfig(
            prompt = prompt,
            model = llmModel(),
            maxAgentIterations = pluginConfigProperty("maxIterations") { "" }.toIntOrNull() ?: 10,
        )
    }

    private fun promptExecutor(): PromptExecutor {
        val baseUrl = pluginConfigProperty("baseUrl") { "https://api.openai.com" }
        val settings = OpenAIClientSettings(baseUrl = baseUrl.trimEnd('/'))
        return MultiLLMPromptExecutor(
            OpenAILLMClient(
                apiKey = pluginConfigProperty("apiKey"),
                settings = settings,
            )
        )
    }

    private fun llmModel(): LLModel {
        val model = pluginConfigProperty("model")
        return OpenAIModels.models.find { it.id == model } ?: throw IllegalArgumentException("无法识别模型:${model}")
    }

    private fun toolRegistry(botApi: BotApi, session: AiChatSession, selfClass: KClass<*>): ToolRegistry {
        val infos = PluginManagement.messagePluginInfos()
            .filter { it.pluginEnable }
            .filterNot { it.originalClass == selfClass }
        val aiTools = AiToolUtils.tools(botApi)

        return ToolRegistry {
            tools(aiTools)
            tools(infos.mapIndexed { index, info ->
                MessagePluginTool(
                    info,
                    toolName(index, info),
                    botApi,
                    session
                )
            })
        }
    }

    private fun toolName(index: Int, info: MessagePluginInfo): String {
        val rawName = listOf(info.pluginTag, info.pluginName, info.pluginId.take(8))
            .joinToString("_")
            .lowercase()
        val name = rawName.replace(Regex("[^a-z0-9_]"), "_").trim('_').ifBlank { "plugin" }
        return "falowp_${index}_$name".take(64)
    }

    @Serializable
    private data class InvokePluginArgs(
        val args: List<String> = emptyList()
    )

    private class MessagePluginTool(
        private val info: MessagePluginInfo,
        name: String,
        private val botApi: BotApi,
        private val session: AiChatSession,
    ) : SimpleTool<InvokePluginArgs>(
        argsType = typeToken<InvokePluginArgs>(),
        name = name,
        description = buildString {
            append("触发 Falowp Bot 消息插件。")
            append("插件名: ${info.pluginName}; ")
            append("标签: ${info.pluginTag}; ")
            if (info.pluginDesc.isNotBlank()) append("说明: ${info.pluginDesc}; ")
            append("匹配规则: ${info.match.regex?.pattern ?: "无固定正则"}; ")
            append("权限: ${info.match.auth}; ")
            append("这是副作用工具，调用后插件会自行回复或进入多轮交互。")
            append("只在用户明确需要该插件能力时调用；参数 args 对应正则捕获组。")
        }
    ) {
        override suspend fun execute(args: InvokePluginArgs): String {
            session.markPluginInvoked(info.pluginName)
            botApi.sendReply("识别到可能需要使用工具，接下来的对话已转接到${info.pluginName}插件", reference = true)
            with(PluginManagement) {
                botApi.invokeMessagePlugin(info.pluginId, args.args.toTypedArray())
            }
            return "插件${info.pluginName}已触发，插件会自行处理后续回复。"
        }
    }
}
