package com.blr19c.falowp.bot.adapter.qq.core.webhook

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.EdECPrivateKey
import java.security.spec.NamedParameterSpec
import java.util.HexFormat

/**
 * 使用 JDK Ed25519 处理 QQ 回调签名
 */
internal class QQSignature(secret: String) {
    private val keyPair = createKeyPair(secret)

    /**
     * 签名内容为时间戳和原始请求体，不重新序列化 JSON
     */
    fun sign(timestamp: String, body: ByteArray): String {
        val signer = Signature.getInstance("Ed25519", "SunEC")
        signer.initSign(keyPair.private)
        signer.update(timestamp.toByteArray(Charsets.UTF_8))
        signer.update(body)
        return HexFormat.of().formatHex(signer.sign())
    }

    /**
     * 缺失、格式错误或被篡改的签名均返回 false
     */
    fun verify(timestamp: String?, signature: String?, body: ByteArray): Boolean {
        if (timestamp.isNullOrBlank() || signature?.length != 128) return false
        return runCatching {
            val verifier = Signature.getInstance("Ed25519", "SunEC")
            verifier.initVerify(keyPair.public)
            verifier.update(timestamp.toByteArray(Charsets.UTF_8))
            verifier.update(body)
            verifier.verify(HexFormat.of().parseHex(signature))
        }.getOrDefault(false)
    }

    /**
     * 按 QQ 规则重复 AppSecret 的 UTF-8 字节，取前 32 字节作为 seed
     */
    private fun createKeyPair(secret: String): KeyPair {
        val bytes = secret.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty()) { "AppSecret 不能为空" }
        val seed = ByteArray(32) { bytes[it % bytes.size] }
        try {
            val generator = KeyPairGenerator.getInstance("Ed25519", "SunEC")
            generator.initialize(NamedParameterSpec.ED25519, seedSource(seed))
            return generator.generateKeyPair().also { pair ->
                val actual = (pair.private as EdECPrivateKey).bytes.orElseThrow()
                try {
                    check(MessageDigest.isEqual(seed, actual)) { "JDK Ed25519 未使用指定 seed" }
                } finally {
                    actual.fill(0)
                }
            }
        } finally {
            bytes.fill(0)
            seed.fill(0)
        }
    }

    /**
     * SunEC 从 nextBytes 读取私钥 seed，此处只供固定密钥导入使用
     */
    private fun seedSource(seed: ByteArray): SecureRandom = object : SecureRandom() {
        private var consumed = false

        override fun nextBytes(bytes: ByteArray) {
            check(!consumed && bytes.size == seed.size) { "JDK Ed25519 seed 读取方式不符合预期" }
            seed.copyInto(bytes)
            consumed = true
        }
    }
}
