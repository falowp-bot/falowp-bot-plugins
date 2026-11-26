package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.session.QQNativeSessionPhase
import com.blr19c.falowp.bot.adapter.qqnative.core.session.QQNativeStatus
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import java.nio.file.*
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * 负责设备信息持久化、二维码刷新、登录恢复和会话生命周期
 */
internal class NativeSession(
    private val bridge: NativeBridge,
    private val host: JsonNode,
    private val onStatus: (QQNativeStatus) -> Unit,
    private val onReady: (JsonNode) -> Unit,
    private val stop: () -> Unit
) : AutoCloseable {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val version = host.s("version")
    private val appid = nativeAppId(version, host.s("arch"))
    private val accountPath = host.path("paths").s("accountPath")
    private val deviceFile = Path.of(host.s("home"), "falowp-device.json")
    private lateinit var device: JsonNode

    @Volatile
    private var account: JsonNode? = null
    private var qr: Job? = null
    private var started = false
    private fun listener(vararg names: String) = obj($$"$listener" to names.toList())
    private fun launch(block: suspend () -> Unit) = scope.launch {
        try {
            block()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            log.error("QQNative 会话失败", error)
            onStatus(QQNativeStatus(QQNativeSessionPhase.ERROR, "NTQQ 会话失败：${error.message}"))
            stop()
        }
    }

    fun start() = launch {
        withContext(Dispatchers.IO) { Files.createDirectories(Path.of(accountPath)) }
        bridge.invoke(
            "engine",
            "initWithDeskTopConfig",
            obj(
                "base_path_prefix" to "",
                "platform_type" to 5,
                "app_type" to 4,
                "app_version" to version,
                "os_version" to host.s("release"),
                "qua" to "V1_LNX_NQ_${version.replace('-', '_')}_GW_B",
                "use_xlog" to false,
                "global_path_config" to obj("desktopGlobalPath" to accountPath),
                "thumb_config" to obj("maxSide" to 324, "minSide" to 48, "longLimit" to 6, "density" to 2)
            ),
            listener()
        )
        val nativeGuid = bridge.invoke("login", "getMachineGuid").asString("")
        check(nativeGuid.isNotBlank()) { "NTQQ 未提供设备标识" }
        val existingDevice = Files.exists(deviceFile)
        device =
            if (existingDevice) withContext(Dispatchers.IO) { mapper.readTree(Files.readString(deviceFile)) }.also {
                check(
                    it.i("version") == 1 && it.s("hostname").isNotBlank() && it.s("guid").isNotBlank()
                ) { "设备记录不完整" }
            } else {
                j(
                    obj(
                        "version" to 1,
                        "hostname" to host.s("hostname"),
                        "guid" to nativeGuid,
                        "lastAccount" to null
                    )
                ).also { save(it) }
            }
        log.info(
            "QQNative 设备记录：{}，原生 GUID 与已保存 GUID 一致={}，宿主名称一致={}",
            if (existingDevice) "复用" else "首次创建",
            nativeGuid == device.s("guid"),
            host.s("hostname") == device.s("hostname")
        )
        bridge.invoke(
            "login",
            "initConfig",
            obj(
                "machineId" to "",
                "appid" to appid,
                "platVer" to host.s("release"),
                "commonPath" to accountPath,
                "clientVer" to version,
                "hostName" to device.s("hostname"),
                "externalVersion" to false
            )
        )
        bridge.invoke(
            "login",
            "addKernelLoginListener",
            listener("onQRCodeGetPicture", "onQRCodeSessionFailed", "onQRCodeLoginSucceed", "onLoginFailed")
        )
        bridge.invoke("login", "connect")
        delay(1200.milliseconds)
        restore()
    }

    private fun save(value: JsonNode) {
        val temp = Path.of("$deviceFile.${UUID.randomUUID()}")
        try {
            Files.createFile(temp, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
            Files.writeString(temp, mapper.writeValueAsString(value))
            Files.move(temp, deviceFile, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun requestQr(delayMs: Long = 0, reason: String = "等待手机确认登录") {
        qr?.cancel()
        if (account == null) qr = launch {
            delay(delayMs.milliseconds)
            if (account == null) {
                log.info("QQNative 显示登录二维码：{}", reason)
                bridge.invoke("login", "getQRCodePicture")
            }
        }
    }

    private suspend fun restore() {
        requestQr(20_000, "自动登录在 20 秒内未完成")
        try {
            val list = bridge.invoke("login", "getLoginList")
            val rows = list.list("LocalLoginInfoList").ifEmpty { list.list("localLoginInfoList") }
            val last = device.s("lastAccount")
            val row = if (last.isNotBlank()) rows.firstOrNull { it.s("uin") == last } else rows.singleOrNull()
            log.info(
                "QQNative 登录恢复：列表结果={}，账号记录数={}，目标记录存在={}，允许快速登录={}",
                list.i("result"), rows.size, row != null, row?.path("isQuickLogin")?.asBoolean(true)
            )
            if (list.i("result") == 0 && row != null && row.path("isQuickLogin")
                    .asBoolean(true) && Regex("\\d+").matches(row.s("uin")) && account == null
            ) {
                onStatus(QQNativeStatus(QQNativeSessionPhase.CONNECTING, "正在恢复上次登录的 QQ"))
                val reply = bridge.invoke("login", "quickLoginWithUin", row.s("uin"))
                if (reply.i("result") == 0 && reply.path("loginErrorInfo").s("errMsg").isBlank()) return
                log.warn(
                    "QQNative 快速登录失败：result={}，step={}，说明={}",
                    reply.s("result"),
                    reply.path("loginErrorInfo").i("step"),
                    reply.path("loginErrorInfo").s("errMsg").replace(Regex("https?://\\S+"), "[URL]").take(200)
                )
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            log.warn("QQNative 自动登录调用异常：{}", error.javaClass.simpleName)
        }
        requestQr(reason = "自动登录不可用，详见前面的登录恢复诊断")
    }

    fun event(event: JsonNode) {
        val data = event.list("arguments").firstOrNull() ?: j(null)
        when (event.s("name")) {
            "onQRCodeGetPicture" -> if (account == null) data.s("pngBase64QrcodeData")
                .removePrefix("data:image/png;base64,").takeIf { it.isNotBlank() }
                ?.let { onStatus(QQNativeStatus(QQNativeSessionPhase.SCAN, "请用手机 QQ 扫码并确认登录", it)) }

            "onQRCodeSessionFailed" -> if (account == null) {
                onStatus(QQNativeStatus(QQNativeSessionPhase.WAITING_QR, "正在刷新二维码"))
                requestQr(1200)
            }

            "onLoginFailed" -> if (account == null) {
                log.warn(
                    "QQNative 收到登录失败回调：result={}，step={}",
                    data.s("result"),
                    data.path("loginErrorInfo").i("step")
                )
                requestQr(1200, "收到 QQ 登录失败回调")
            }

            "onQRCodeLoginSucceed" -> if (account == null) {
                account = data
                qr?.cancel()
                launch { open(data) }
            }

            "onOpentelemetryInit" -> if (data.path("is_init").asBoolean(false) && !started) {
                started = true
                launch { online() }
            }

            "onKickedOffLine" -> stop()
        }
    }

    private suspend fun open(user: JsonNode) {
        onStatus(QQNativeStatus(QQNativeSessionPhase.CONNECTING, "认证成功，正在建立消息会话"))
        val guid = device.s("guid").replace(Regex("^(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})$"), "$1-$2-$3-$4-$5")
        val config = obj(
            "selfUin" to user.s("uin"),
            "selfUid" to user.s("uid"),
            "appid" to appid,
            "clientVer" to version,
            "platform" to 5,
            "platVer" to host.s("release"),
            "machineId" to "",
            "a2" to "",
            "d2" to "",
            "d2Key" to "",
            "desktopPathConfig" to obj("account_path" to accountPath),
            "defaultFileDownloadPath" to System.getProperty("java.io.tmpdir"),
            "deviceInfo" to obj(
                "guid" to guid,
                "buildVer" to version,
                "localId" to 2052,
                "devName" to device.s("hostname"),
                "devType" to "Linux",
                "vendorName" to "",
                "osVer" to host.s("release"),
                "vendorOsName" to "Linux",
                "setMute" to false,
                "vendorType" to 0
            ),
            "deviceConfig" to "{}",
            "rdeliveryConfig" to obj(
                "appKey" to "",
                "systemId" to 0,
                "appId" to "",
                "logicEnvironment" to "",
                "platform" to 5,
                "language" to "",
                "sdkVersion" to "",
                "userId" to "",
                "appVersion" to "",
                "osVersion" to "",
                "bundleId" to "",
                "serverUrl" to "",
                "fixedAfterHitKeys" to emptyList<String>()
            )
        )
        bridge.invoke(
            "session",
            "init",
            config,
            listener(),
            listener(),
            listener("onOpentelemetryInit", "onKickedOffLine")
        )
        bridge.invoke("startup", "start")
    }

    private suspend fun online() {
        val user = checkNotNull(account)
        // 先初始化 Kotlin 会话状态，再注册监听器，避免丢失提前到达的原生事件
        onReady(
            j(
                obj(
                    "id" to user.s("uin"),
                    "uid" to user.s("uid"),
                    "nickname" to user.s("nickName").ifEmpty { user.s("nick") },
                    "paths" to host.path("paths")
                )
            )
        )
        bridge.invoke(
            "group",
            "addKernelGroupListener",
            listener(
                "onMemberInfoChange",
                "onGroupListUpdate",
                "onGroupNotifiesUpdated",
                "onGroupDetailInfoChange",
                "onShutUpMemberListChanged",
                "onGroupSingleScreenNotifies"
            )
        )
        bridge.invoke("buddy", "addKernelBuddyListener", listener("onBuddyReqChange"))
        bridge.msg(
            "addKernelMsgListener",
            listener(
                "onRecvMsg",
                "onRecvSysMsg",
                "onRichMediaDownloadComplete",
                "onMsgInfoListUpdate",
                "onInputStatusPush"
            )
        )
        device = j(mapper.convertValue(device, Map::class.java) + obj("lastAccount" to user.s("uin")))
        save(device)
        val online = QQNativeStatus(
            QQNativeSessionPhase.ONLINE, "QQ ${user.s("uin")} 已上线",
            accountId = user.s("uin"), nickname = user.s("nickName").ifEmpty { user.s("nick") })
        onStatus(online)
        runCatching {
            bridge.invoke("login", "getLoginList").list("LocalLoginInfoList")
                .firstOrNull { it.s("uin") == user.s("uin") }
        }.onFailure { if (it is CancellationException) throw it }
            .getOrNull()?.takeIf { it.s("nickName").isNotBlank() }
            ?.let { onStatus(online.copy(nickname = it.s("nickName"))) }
    }

    override fun close() {
        scope.cancel()
    }
}

/**
 * 两个固定版本的 Linux QQ 使用不同的登录认证 AppID
 */
internal fun nativeAppId(version: String, arch: String): String {
    require(version == "3.2.34-53644") { "未经验证的 NTQQ 版本：$version" }
    return when (arch) {
        "arm64" -> "537391665"
        "x64" -> "537391664"
        else -> error("未经验证的 NTQQ 架构：$arch")
    }
}
