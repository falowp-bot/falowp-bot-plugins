package com.blr19c.falowp.bot.adapter.qqnative.core.transport

import com.blr19c.falowp.bot.adapter.qqnative.core.QQNativeAccount
import com.blr19c.falowp.bot.adapter.qqnative.core.history.*
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessage
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.*
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.QQNativeNotice
import com.blr19c.falowp.bot.adapter.qqnative.core.session.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.io.BufferedWriter
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds

internal typealias WorkerCall = suspend (String, Map<String, Any>) -> JsonNode

/**
 * 管理 Electron 子进程及请求关联，处理回调、退出和重启
 */
internal class WorkerProcess(
    private val onMessage: (QQNativeMessage, QQNativeAccount) -> Unit,
    private val onNotice: (QQNativeNotice, QQNativeAccount) -> Unit
) {
    private val json = JsonMapper.builder().build()
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    var status = QQNativeStatus(QQNativeSessionPhase.PREPARING, "正在准备 Linux NTQQ")
        private set

    @Volatile
    private var account: String? = null
    private var input: BufferedWriter? = null

    @Volatile
    private var runtime: NativeRuntime? = null

    @Volatile
    private var session: NativeSession? = null
    private var stopWorker: (() -> Unit)? = null
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JsonNode>>()
    private lateinit var history: MessageHistory
    private lateinit var eventRoot: Path
    private var persistEvents = false

    fun start() {
        thread(isDaemon = true, name = "qq-native") {
            runCatching { supervise() }.onFailure {
                status = QQNativeStatus(QQNativeSessionPhase.ERROR, "启动失败：${it.message}")
                log.error("QQNative 启动失败", it)
            }
        }
    }

    private fun supervise() {
        check(System.getProperty("os.name") == "Linux") { "需要 Linux NTQQ 运行环境" }
        val root = (System.getenv("FALOWP_QQ_DATA_DIR")?.takeIf { it.isNotBlank() }?.let { Path.of(it) }
            ?: Path.of("data", "qq-native")).toAbsolutePath().normalize()
        Files.createDirectories(
            root,
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))
        )
        FileChannel.open(root.resolve("runtime.lock"), CREATE, WRITE).use { channel ->
            checkNotNull(channel.tryLock()) { "此目录已被另一个 QQNative 实例使用" }.use {
                val settings = HistorySettings.configured()
                history = runBlocking(Dispatchers.IO) { HistoryStores.open(settings, root) }
                eventRoot = root.resolve("event-inbox")
                persistEvents = settings.enabled && settings.backend != "memory"
                val scripts = Files.createTempDirectory(root, ".scripts.")
                try {
                    for (name in listOf(
                        "install-linux.sh",
                        "start-linux.sh",
                        "worker.cjs",
                        "native-rpc.cjs",
                        "packet-bridge.cjs",
                        "native/packet-linux-arm64.node",
                        "native/packet-linux-x64.node",
                        "media-tools.sh"
                    )) {
                        checkNotNull(javaClass.getResourceAsStream("/qq-native/core/$name")).use { stream ->
                            Files.createDirectories(scripts.resolve(name).parent)
                            Files.copy(stream, scripts.resolve(name))
                        }
                    }
                    while (true) {
                        runCatching { runWorker(root, scripts) }.onFailure { log.warn("QQNative 子进程退出", it) }
                        status =
                            QQNativeStatus(QQNativeSessionPhase.RETRYING, "NTQQ 已退出，30 秒后重试。请查看容器日志。")
                        Thread.sleep(30_000)
                    }
                } finally {
                    scripts.toFile().deleteRecursively()
                }
            }
        }
    }

    private fun runWorker(root: Path, scripts: Path) {
        status = QQNativeStatus(QQNativeSessionPhase.PREPARING, "正在准备 NTQQ 并连接登录服务")
        val process =
            ProcessBuilder("sh", scripts.resolve("start-linux.sh").toString(), root.toString(), scripts.toString())
                .redirectError(ProcessBuilder.Redirect.INHERIT).start()
        val stopProcess = Runnable {
            process.descendants().toList().asReversed().forEach { it.destroy() }
            process.destroy()
        }
        val shutdown = Thread(stopProcess)
        Runtime.getRuntime().addShutdownHook(shutdown)
        try {
            synchronized(this) {
                input = process.outputWriter()
                stopWorker = { process.destroy() }
            }
            process.inputReader().forEachLine { line ->
                if (line.startsWith("@falowp ")) dispatch(json.readTree(line.substring(8)))
            }
            check(process.waitFor() == 0) { "退出码 ${process.exitValue()}" }
        } finally {
            stopProcess.run()
            runCatching { Runtime.getRuntime().removeShutdownHook(shutdown) }
            synchronized(this) {
                session?.close()
                session = null
                runtime?.close()
                runtime = null
                stopWorker = null
                account = null
                input = null
                pending.values.forEach { it.completeExceptionally(IllegalStateException("NTQQ 会话已关闭")) }
                pending.clear()
            }
        }
    }

    private fun dispatch(event: JsonNode) {
        fun field(key: String) = event.path(key).asString("")
        when (field("type")) {
            "hostReady" -> {
                val raw: WorkerCall = { op, args -> request(op, null, args) }
                session = NativeSession(NativeBridge(raw), event, { updated ->
                    status = updated
                    if (updated.phase == QQNativeSessionPhase.ONLINE) runtime?.start()
                }, ::openAccount) { stopWorker?.invoke() }.also { it.start() }
            }

            "nativeEvent" -> {
                session?.event(event)
                runtime?.event(event)
            }

            "failed" -> status = QQNativeStatus(QQNativeSessionPhase.ERROR, "NTQQ 初始化失败")
            "result" -> pending.remove(field("request"))?.let { reply ->
                if (event.path("ok").asBoolean(false)) reply.complete(event.path("data"))
                else reply.completeExceptionally(IllegalStateException(field("error")))
            }

        }
    }

    private fun openAccount(event: JsonNode) {
        val id = event.path("id").asString()
        lateinit var active: NativeRuntime
        val raw: WorkerCall = { op, args ->
            check(runtime === active) { "QQ 会话已离线或账号已变更" }
            request(op, id, args)
        }
        account = id
        val inbox = NativeEventInbox(if (persistEvents) eventRoot.resolve(HistoryKey(id, 0, "", "").hash) else null)
        active = NativeRuntime(
            raw, event,
            { message -> onMessage(message, boundAccount(id, active)) },
            { notice -> onNotice(notice, boundAccount(id, active)) },
            history, inbox, startImmediately = false
        )
        runtime?.close()
        runtime = active
    }

    private fun boundAccount(id: String, active: NativeRuntime): QQNativeAccount {
        check(account == id && runtime === active) { "QQ 会话已离线或账号已变更" }
        val nickname = status.takeIf { it.accountId == id }?.nickname.orEmpty().ifBlank { id }
        return active.account(nickname) {
            check(account == id && runtime === active) { "QQ 会话已离线或账号已变更" }
        }
    }

    fun account(id: String): QQNativeAccount = boundAccount(id, checkNotNull(runtime) { "QQ 会话尚未就绪" })

    private suspend fun request(operation: String, selfId: String?, args: Map<String, Any>): JsonNode =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val reply = CompletableDeferred<JsonNode>()
            try {
                synchronized(this) {
                    check(selfId == null || account == selfId) { "QQ 会话已离线或账号已变更" }
                    val pipe = checkNotNull(input)
                    pending[id] = reply
                    val command = mapOf("type" to operation, "request" to id, "args" to args)
                    pipe.write(json.writeValueAsString(command))
                    pipe.newLine()
                    pipe.flush()
                }
                return@withContext withTimeout(120_000.milliseconds) { reply.await() }
            } finally {
                pending.remove(id)
            }
        }
}
