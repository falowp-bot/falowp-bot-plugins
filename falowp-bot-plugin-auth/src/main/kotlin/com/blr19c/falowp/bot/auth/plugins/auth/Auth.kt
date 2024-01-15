package com.blr19c.falowp.bot.auth.plugins.auth

import com.blr19c.falowp.bot.auth.plugins.hook.NoAuthorizationHook
import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.listener.hooks.MessagePluginExecutionHook
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Hook.Companion.beforeHook
import com.blr19c.falowp.bot.system.plugin.hook.withPluginHook

/**
 * 权限
 */
@Plugin(
    name = "权限",
    tag = "权限",
    desc = "权限",
    hidden = true
)
class Auth {

    private val auth = beforeHook<MessagePluginExecutionHook> { (receiveMessage, register) ->
        val botApi = this.botApi()
        if ((register.match.auth?.code ?: Int.MIN_VALUE) > receiveMessage.sender.auth.code) {
            withPluginHook(botApi, NoAuthorizationHook(MessagePluginExecutionHook(receiveMessage, register))) {
                botApi.sendReply(SendMessage.builder("你还没有权限操作此功能").build())
            }
            return@beforeHook this.terminate()
        }
    }

    init {
        auth.register()
    }
}