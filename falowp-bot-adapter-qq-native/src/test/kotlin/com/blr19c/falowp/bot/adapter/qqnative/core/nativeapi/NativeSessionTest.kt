package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.session.QQNativeSessionPhase
import com.blr19c.falowp.bot.adapter.qqnative.core.session.QQNativeStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import tools.jackson.databind.JsonNode
import java.nio.file.Files
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class NativeSessionTest {
    @Test
    fun loginAppIdMustMatchPinnedNativeArchitecture() {
        assertEquals("537391665", nativeAppId("3.2.34-53644", "arm64"))
        assertEquals("537391664", nativeAppId("3.2.34-53644", "x64"))
        assertFails { nativeAppId("3.2.34-53644", "other") }
        assertFails { nativeAppId("other", "arm64") }
    }

    @Test
    fun persistedDeviceIdentitySurvivesHostAndNativeGuidChanges(): Unit = runBlocking(Dispatchers.IO) {
        val home = Files.createTempDirectory("qq-device-")
        val accountPath = home.resolve(".config/QQ").toString()
        suspend fun open(hostname: String, nativeGuid: String): Pair<JsonNode, JsonNode> {
            lateinit var login: JsonNode
            val config = CompletableDeferred<JsonNode>()
            val bridge = NativeBridge { _, args ->
                val params = j(args).path("arguments")
                when (args["method"]) {
                    "getMachineGuid" -> j(nativeGuid)
                    "initConfig" -> {
                        login = params[0]
                        j(null)
                    }

                    "getLoginList" -> j(obj("result" to 0, "LocalLoginInfoList" to emptyList<Any>()))
                    "init" -> {
                        config.complete(params[0])
                        j(null)
                    }

                    else -> j(null)
                }
            }
            val session = NativeSession(
                bridge, j(
                    obj(
                        "home" to home.toString(), "hostname" to hostname, "release" to "linux-test",
                        "version" to "3.2.34-53644", "arch" to "arm64", "paths" to obj("accountPath" to accountPath)
                    )
                ), {}, {}, {})
            session.use {
                session.start().join()
                session.event(
                    j(
                        obj(
                            "name" to "onQRCodeLoginSucceed",
                            "arguments" to listOf(obj("uin" to "9", "uid" to "u_9"))
                        )
                    )
                )
                return login to withTimeout(3000.milliseconds) { config.await() }
            }
        }
        try {
            val first = open("original-host", "123456781234123412341234567890ab")
            val second = open("replacement-host", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            assertEquals("537391665", second.first.s("appid"))
            assertEquals("537391665", second.second.s("appid"))
            assertEquals(first.first.s("hostName"), second.first.s("hostName"))
            assertEquals(first.second.path("deviceInfo").s("guid"), second.second.path("deviceInfo").s("guid"))
            assertEquals("original-host", second.second.path("deviceInfo").s("devName"))
            assertEquals(accountPath, second.first.s("commonPath"))
            assertEquals(accountPath, second.second.path("desktopPathConfig").s("account_path"))
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun statusCallbacksPreserveQrAndInitializeAccountBeforeListeners(): Unit = runBlocking(Dispatchers.IO) {
        val home = Files.createTempDirectory("qq-session-callbacks-")
        val statuses = mutableListOf<QQNativeStatus>()
        val opened = CompletableDeferred<Unit>()
        val complete = CompletableDeferred<Unit>()
        var ready = false
        val listeners = mutableListOf<String>()
        val bridge = NativeBridge { _, args ->
            when (val method = args["method"]) {
                "getMachineGuid" -> j("123456781234123412341234567890ab")
                "getLoginList" -> j(
                    obj(
                        "result" to 0, "LocalLoginInfoList" to
                                if (ready) listOf(obj("uin" to "9", "nickName" to "更新昵称")) else emptyList()
                    )
                )

                "init" -> {
                    opened.complete(Unit); j(null)
                }

                "addKernelGroupListener", "addKernelBuddyListener", "addKernelMsgListener" -> {
                    assertTrue(ready, "Account must be ready before registering native listeners")
                    listeners.add(method.toString())
                    j(null)
                }

                else -> j(null)
            }
        }
        try {
            NativeSession(
                bridge,
                j(
                    obj(
                        "home" to home.toString(), "hostname" to "test", "release" to "linux-test",
                        "version" to "3.2.34-53644", "arch" to "arm64",
                        "paths" to obj("accountPath" to home.resolve("account").toString())
                    )
                ),
                { status ->
                    statuses.add(status)
                    if (status.nickname == "更新昵称") complete.complete(Unit)
                },
                { account ->
                    assertEquals("9", account.s("id"))
                    assertEquals("u_9", account.s("uid"))
                    ready = true
                },
                { complete.completeExceptionally(IllegalStateException("Unexpected session stop")) }).use { session ->
                session.start().join()
                fun event(name: String, data: Map<String, Any?>) =
                    session.event(j(obj("name" to name, "arguments" to listOf(data))))
                event("onQRCodeGetPicture", obj("pngBase64QrcodeData" to "data:image/png;base64,test-qr"))
                assertEquals(QQNativeSessionPhase.SCAN, statuses.last().phase)
                assertEquals("test-qr", statuses.last().qr)
                event("onQRCodeSessionFailed", obj())
                assertEquals(QQNativeSessionPhase.WAITING_QR, statuses.last().phase)
                event("onQRCodeLoginSucceed", obj("uin" to "9", "uid" to "u_9", "nickName" to "初始昵称"))
                withTimeout(3000.milliseconds) { opened.await() }
                assertEquals(QQNativeSessionPhase.CONNECTING, statuses.last().phase)
                event("onOpentelemetryInit", obj("is_init" to true))
                withTimeout(3000.milliseconds) { complete.await() }
                assertEquals(3, listeners.size)
                assertEquals(
                    listOf("初始昵称", "更新昵称"),
                    statuses.filter { it.phase == QQNativeSessionPhase.ONLINE }.map { it.nickname })
                assertEquals("9", statuses.last().accountId)
            }
        } finally {
            home.toFile().deleteRecursively()
        }
    }

}
