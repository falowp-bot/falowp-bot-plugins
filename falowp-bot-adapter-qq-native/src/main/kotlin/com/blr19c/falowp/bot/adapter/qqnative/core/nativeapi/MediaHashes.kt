package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * 文件大小、摘要及 Highway 分块上传所需的 SHA-1 状态
 */
internal class FileHashes(
    val path: Path,
    val size: Long,
    val md5: ByteArray,
    val sha1: ByteArray,
    val states: List<ByteArray>
)

internal fun digest(kind: String, data: ByteArray) = MessageDigest.getInstance(kind).digest(data)

/**
 * 计算文件摘要；Highway 检查点使用每 MiB 处未填充的 SHA-1 小端状态，末尾追加标准摘要
 */
internal fun hashes(path: Path, checkpoints: Boolean = false): FileHashes {
    val md5 = MessageDigest.getInstance("MD5")
    val sha1 = MessageDigest.getInstance("SHA-1")
    val states = mutableListOf<ByteArray>()
    val h = intArrayOf(0x67452301, 0xefcdab89.toInt(), 0x98badcfe.toInt(), 0x10325476, 0xc3d2e1f0.toInt())
    val w = IntArray(80)
    var size = 0L
    var processed = 0L
    Files.newInputStream(path).use { input ->
        while (true) {
            val data = input.readNBytes(1024 * 1024)
            if (data.isEmpty()) break
            md5.update(data)
            sha1.update(data)
            size += data.size
            if (!checkpoints) continue
            for (offset in 0 until data.size - 63 step 64) {
                val buffer = ByteBuffer.wrap(data, offset, 64)
                for (i in 0..15) w[i] = buffer.int
                for (i in 16..79) w[i] = Integer.rotateLeft(w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16], 1)
                var a = h[0]
                var b = h[1]
                var c = h[2]
                var d = h[3]
                var e = h[4]
                for (i in 0..79) {
                    val f = when {
                        i < 20 -> (b and c) or (b.inv() and d)
                        i < 40 -> b xor c xor d
                        i < 60 -> (b and c) or (b and d) or (c and d)
                        else -> b xor c xor d
                    }
                    val k = when {
                        i < 20 -> 0x5a827999
                        i < 40 -> 0x6ed9eba1
                        i < 60 -> 0x8f1bbcdc.toInt()
                        else -> 0xca62c1d6.toInt()
                    }
                    val next = Integer.rotateLeft(a, 5) + f + e + k + w[i]
                    e = d
                    d = c
                    c = Integer.rotateLeft(b, 30)
                    b = a
                    a = next
                }
                intArrayOf(a, b, c, d, e).forEachIndexed { i, v -> h[i] += v }
                processed += 64
                if (processed % (1024 * 1024) == 0L) states.add(
                    ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN).apply { h.forEach { putInt(it) } }.array()
                )
            }
        }
    }
    val final = sha1.digest()
    states.add(final)
    return FileHashes(path, size, md5.digest(), final, states)
}
