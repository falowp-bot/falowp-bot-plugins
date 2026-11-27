package com.blr19c.falowp.bot.adapter.qq.core.webhook

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.EdECPrivateKeySpec
import java.security.spec.NamedParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.HexFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class QQSignatureTest {
    @Test
    fun `signatures verify against the official public key`() {
        val signer = QQSignature("naOC0ocQE3shWLAfffVLB1rhYPG7")
        val publicBytes = byteArrayOf(
            215.toByte(), 195.toByte(), 98, 254.toByte(), 120, 174.toByte(), 248.toByte(), 31,
            242.toByte(), 50, 135.toByte(), 180.toByte(), 147.toByte(), 98, 139.toByte(), 93,
            176.toByte(), 42, 60, 79, 227.toByte(), 11, 33, 94, 77, 25, 96.toByte(), 155.toByte(),
            93, 118, 103, 58,
        )
        val encoded = HexFormat.of().parseHex("302a300506032b6570032100") + publicBytes
        val publicKey = KeyFactory.getInstance("Ed25519", "SunEC").generatePublic(X509EncodedKeySpec(encoded))
        val body = "{ \"op\": 0 }".toByteArray()
        val signature = signer.sign("1725442341", body)
        val verifier = Signature.getInstance("Ed25519", "SunEC")
        verifier.initVerify(publicKey)
        verifier.update("1725442341".toByteArray() + body)
        assertTrue(verifier.verify(HexFormat.of().parseHex(signature)))
    }

    @Test
    fun `seed derivation follows UTF8 bytes for short exact and long secrets`() {
        val secrets = listOf("a", "x".repeat(31), "x".repeat(32), "x".repeat(33), "密钥🔑")
        val body = "消息".toByteArray()
        for (secret in secrets) {
            val bytes = secret.toByteArray(Charsets.UTF_8)
            val seed = ByteArray(32) { bytes[it % bytes.size] }
            val privateKey = KeyFactory.getInstance("Ed25519", "SunEC")
                .generatePrivate(EdECPrivateKeySpec(NamedParameterSpec.ED25519, seed))
            val signer = Signature.getInstance("Ed25519", "SunEC")
            signer.initSign(privateKey)
            signer.update("12345".toByteArray() + body)
            val expected = HexFormat.of().formatHex(signer.sign())
            val actual = QQSignature(secret)
            assertEquals(expected, actual.sign("12345", body))
            assertTrue(actual.verify("12345", expected, body))
        }
    }

    @Test
    fun `rejects empty secret and signatures from another secret`() {
        assertFailsWith<IllegalArgumentException> { QQSignature("") }
        val signer = QQSignature("one")
        val body = byteArrayOf(0, 1, -1)
        assertFalse(signer.verify("1", QQSignature("two").sign("1", body), body))
        assertFalse(signer.verify("1", "00".repeat(64), body))
        assertFalse(signer.verify("1", null, body))
    }

    @Test
    fun `one key pair supports concurrent signing and verification`() = runBlocking {
        val signature = QQSignature("concurrent-secret")
        (1..32).map { index ->
            async(Dispatchers.Default) {
                val body = "message-$index".toByteArray()
                val value = signature.sign("12345", body)
                assertTrue(signature.verify("12345", value, body))
                assertEquals(value, QQSignature("concurrent-secret").sign("12345", body))
            }
        }.awaitAll()
        Unit
    }
}
