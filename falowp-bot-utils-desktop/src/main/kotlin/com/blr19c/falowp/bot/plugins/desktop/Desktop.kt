package com.blr19c.falowp.bot.plugins.desktop

import com.blr19c.falowp.bot.plugins.desktop.log.LogListener
import com.blr19c.falowp.bot.system.plugin.PluginUtils
import com.blr19c.falowp.bot.system.web.WebServer
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

@PluginUtils
object Desktop {

    init {
        LogListener.registerListener()
        WebServer.registerRoute {
            authenticate {
                webSocket("/desktop/ws") {
                    call.respond("")
                }
                post("/desktop") {
                    post("/login") {
                        call.respond("ax")
                    }
                }
            }
        }
    }
}