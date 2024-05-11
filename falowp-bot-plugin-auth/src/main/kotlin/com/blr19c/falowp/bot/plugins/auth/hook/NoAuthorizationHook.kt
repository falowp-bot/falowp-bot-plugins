package com.blr19c.falowp.bot.plugins.auth.hook

import com.blr19c.falowp.bot.system.listener.hooks.MessagePluginExecutionHook
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 无权限
 */
data class NoAuthorizationHook(val messagePluginExecutionHook: MessagePluginExecutionHook) : Plugin.Listener.Hook