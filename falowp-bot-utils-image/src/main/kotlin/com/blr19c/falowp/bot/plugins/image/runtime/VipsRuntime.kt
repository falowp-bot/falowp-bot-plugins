package com.blr19c.falowp.bot.plugins.image.runtime

import java.io.OutputStream
import java.net.URI
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.nio.file.StandardOpenOption.*
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Properties
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream

/**
 * libvips运行库
 */
internal object VipsRuntime {

    private fun platform(): String {
        val arch = when (val name = System.getProperty("os.arch")) {
            "aarch64", "arm64" -> "arm64"
            "x86_64", "amd64" -> "x64"
            else -> error("不支持的CPU架构: $name")
        }
        val os = System.getProperty("os.name").lowercase()
        val system = when {
            os.startsWith("windows") -> "win32"
            os.startsWith("mac") -> "darwin"
            os == "linux" -> {
                //从当前进程判断，避免系统同时装有glibc和musl时选错
                val musl = Files.lines(Path.of("/proc/self/maps")).use { lines ->
                    lines.anyMatch { it.contains("/ld-musl-") || it.contains("/libc.musl-") }
                }
                if (musl) "linuxmusl" else "linux"
            }
            else -> error("不支持的操作系统: $os")
        }
        return "$system-$arch"
    }

    fun prepareLibrary(): Path {
        val platform = platform()
        val manifest = Properties().apply {
            VipsRuntime::class.java.getResourceAsStream("/image/libvips.properties")!!.use { load(it) }
        }
        val (url, archiveHash, entryName, libraryHash) = manifest.getProperty(platform).split(' ')
        val cache = System.getProperty("falowp.image.cacheDir")
            ?: Path.of(System.getProperty("user.home"), ".cache", "falowp-bot", "image").toString()
        val dir = Files.createDirectories(Path.of(cache, platform, archiveHash).toAbsolutePath())
        val library = dir.resolve(entryName.substringAfterLast('/'))
        FileChannel.open(dir.resolve("install.lock"), CREATE, WRITE).use { channel ->
            channel.lock().use {
                if (Files.isRegularFile(library) && sha256(library) == libraryHash) return library
                install(dir, library, url, archiveHash, entryName, libraryHash)
            }
        }
        return library
    }

    private fun install(dir: Path, library: Path, url: String, archiveHash: String, entryName: String, libraryHash: String) {
        val archive = Files.createTempFile(dir, "download-", ".tgz")
        try {
            val connection = URI.create(url).toURL().openConnection().apply {
                connectTimeout = 15000
                readTimeout = 60000
            }
            connection.getInputStream().use { Files.copy(it, archive, REPLACE_EXISTING) }
            check(sha256(archive) == archiveHash) { "libvips下载包校验失败: $entryName" }
            val staged = Files.createTempFile(dir, "libvips-", ".tmp")
            try {
                TarArchiveInputStream(GZIPInputStream(Files.newInputStream(archive).buffered())).use { tar ->
                    val entry = generateSequence { tar.nextEntry }.first { it.name == entryName }
                    check(entry.isFile)
                    Files.copy(tar, staged, REPLACE_EXISTING)
                }
                check(sha256(staged) == libraryHash) { "libvips运行库校验失败: $entryName" }
                Files.move(staged, library, ATOMIC_MOVE, REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(staged)
            }
        } finally {
            Files.deleteIfExists(archive)
        }
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestOutputStream(OutputStream.nullOutputStream(), digest).use { Files.copy(path, it) }
        return HexFormat.of().formatHex(digest.digest())
    }
}
