package com.blr19c.falowp.bot.adapter.qqnative.ui

import com.blr19c.falowp.bot.adapter.qqnative.core.session.QQNativeStatus
import com.blr19c.falowp.bot.system.web.WebServer
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun registerSessionRoutes(status: () -> QQNativeStatus) {
    WebServer.registerRoute {
        authenticate {
            get("/qq-native") { call.respondRedirect("qq-native/") }
            get("/qq-native/status") {
                call.response.header(HttpHeaders.CacheControl, "no-store")
                call.respond(status())
            }
            staticResources("/qq-native/", "qq-native/ui") {
                modify { _, call ->
                    call.response.header("X-Frame-Options", "DENY")
                    call.response.header(HttpHeaders.CacheControl, "no-cache")
                }
            }
        }
    }
}
