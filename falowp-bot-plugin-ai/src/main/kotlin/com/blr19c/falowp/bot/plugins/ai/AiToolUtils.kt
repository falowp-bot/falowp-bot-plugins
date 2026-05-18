package com.blr19c.falowp.bot.plugins.ai

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.Tool
import ai.koog.serialization.typeToken
import com.blr19c.falowp.bot.system.api.BotApi
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * AI工具注册入口。
 *
 * 其它插件可以在自己的 @PluginUtils object init 中调用这里的方法注册工具，
 * 无需再把能力包装成消息插件。
 */
@Suppress("Unused")
object AiToolUtils {
    private val factories = ConcurrentHashMap<String, AiToolFactory>()

    /**
     * 注册一个固定工具。
     */
    fun registerTool(tool: Tool<*, *>, replace: Boolean = false) {
        registerToolFactory(tool.name, replace) { tool }
    }

    /**
     * 注册一个随当前会话创建的工具。
     */
    fun registerToolFactory(name: String, replace: Boolean = false, factory: AiToolFactory) {
        require(name.matches(TOOL_NAME_REGEX)) {
            "AI工具名称只能包含字母、数字、_、-，且长度必须在1到64之间:$name"
        }
        if (replace) {
            factories[name] = factory
            return
        }
        require(factories.putIfAbsent(name, factory) == null) { "AI工具已注册:$name" }
    }

    /**
     * 注册一个只有 input 文本参数的简单工具。
     */
    fun registerSimpleTool(
        name: String,
        description: String,
        replace: Boolean = false,
        block: suspend AiToolContext.(AiToolTextArgs) -> String
    ) {
        registerToolFactory(name, replace) { context ->
            object : SimpleTool<AiToolTextArgs>(
                argsType = typeToken<AiToolTextArgs>(),
                name = name,
                description = description,
            ) {
                override suspend fun execute(args: AiToolTextArgs): String = context.block(args)
            }
        }
    }

    /**
     * 取消注册AI工具。
     */
    fun unregisterTool(name: String) {
        factories.remove(name)
    }

    /**
     * 当前已注册的AI工具名称。
     */
    fun toolNames(): List<String> {
        return factories.keys().asSequence().sorted().toList()
    }

    internal fun tools(botApi: BotApi): List<Tool<*, *>> {
        val context = AiToolContext(botApi)
        return factories.toSortedMap()
            .map { (name, factory) ->
                factory.create(context).also {
                    require(it.name == name) { "AI工具工厂注册名与实际工具名不一致:$name != ${it.name}" }
                }
            }
    }

    private val TOOL_NAME_REGEX = Regex("[A-Za-z0-9_-]{1,64}")
}

fun interface AiToolFactory {
    fun create(context: AiToolContext): Tool<*, *>
}

@Suppress("Unused")
class AiToolContext internal constructor(
    val botApi: BotApi,
)

@Serializable
data class AiToolTextArgs(
    val input: String = "",
)
