package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.b
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.u
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

class PacketTest {
    @Test
    fun binaryLengthsAndUnsignedIdentifiersSurviveWireEncoding() {
        for (size in listOf(0, 22, 23, 126, 127, 128, 255, 1024)) {
            val bytes = ByteArray(size) { it.toByte() }
            assertContentEquals(bytes, b(m(4, bytes), 4))
        }
        assertEquals(ULong.MAX_VALUE, u(cat(Wire.varint(8u), Wire.varint(ULong.MAX_VALUE)), 1))
        assertFails { Wire.fields(byteArrayOf(8, 128.toByte())) }
        assertFails { Wire.fields(byteArrayOf(10, 127, 0)) }
        assertFails { Wire.fields(byteArrayOf(0)) }
    }

    @Test
    fun packetBridgePreservesBinaryAndRejectsStringResponses(): Unit = runBlocking(Dispatchers.IO) {
        val bytes = byteArrayOf(0, 127, 128.toByte(), 255.toByte())
        val bridge = NativeBridge { op, args ->
            assertEquals("native", op)
            val payload = j(args)
            assertEquals("sendSsoCmdReqByContend", payload.s("method"))
            assertContentEquals(bytes, payload.path("arguments")[1].binary())
            j(obj("result" to 0, "rspbuffer" to binary(bytes)))
        }
        assertContentEquals(bytes, bridge.packet("test", bytes))
        val broken = NativeBridge { _, _ -> j(obj("result" to 0, "rspbuffer" to "corrupted")) }
        assertFailsWith<IllegalStateException> { broken.packet("test", bytes) }
        val rejected = NativeBridge { _, _ -> j(obj("result" to 9, "rspbuffer" to binary(bytes))) }
        assertFailsWith<IllegalStateException> { rejected.packet("test", bytes) }
    }

    @Test
    fun highwayFramingValidatesBothErrorLayersAndLength() {
        val good = Highway.frame(cat(n(3, 0), m(2, n(5, 0))), byteArrayOf())
        Highway.response(good)
        assertFails { Highway.response(good.copyOf(good.size - 1)) }
        assertFails { Highway.response(Highway.frame(n(3, 7), byteArrayOf())) }
        assertFails { Highway.response(Highway.frame(m(2, n(5, 7)), byteArrayOf())) }
        assertEquals("1.2.3.4", Highway.ip(0x04030201u))
    }

    @Test
    fun mediaHashesUseLittleEndianUnpaddedCheckpoints() {
        val file = Files.createTempFile("qq-hash-", ".bin")
        try {
            Files.write(file, ByteArray(1024 * 1024 + 79) { 0xa5.toByte() })
            val hash = hashes(file, true)
            assertEquals(2, hash.states.size)
            // 使用独立的 SHA-1 状态测试向量
            assertEquals("311550c1a56061a18adc50457472c34022e50e19", hash.states[0].hex())
            assertContentEquals(digest("SHA-1", Files.readAllBytes(file)), hash.sha1)
            assertContentEquals(hash.sha1, hash.states.last())
            assertContentEquals(digest("MD5", Files.readAllBytes(file)), hash.md5)
        } finally {
            Files.delete(file)
        }
    }

    @Test
    fun decompressionEnforcesOutputLimit() {
        assertFails { unpack(gzip(ByteArray(4096)), true, 100) }
        assertContentEquals(byteArrayOf(1, 2), unpack(deflate(byteArrayOf(1, 2))))
    }

    @Test
    fun qqNormalizedNestedXmlUsesOuterSectionEvenWithoutResid() {
        val xml = """<msg serviceID="35" m_fileName="child-1" m_resid=""/>"""
        val rich = m(12, m(1, cat(byteArrayOf(1), deflate(xml.toByteArray()))))
        val row = cat(m(1, n(1, 42)), m(2, n(5, 3)), m(3, m(1, m(2, rich))))
        val child = m(3, m(1, m(2, m(1, m(1, "子记录")))))
        val body = cat(m(2, m(1, "MultiMsg"), m(2, m(1, row))), m(2, m(1, "child-1"), m(2, m(1, child))))
        val sections = ForwardReader.sections(body)
        val parent = ForwardReader.rows(sections, "MultiMsg", Peer(2, "100")).single()
        val card = assertNotNull(NativeDelivery.cardData(parent))
        assertEquals("", card.path("meta").path("detail").s("resid"))
        val name = card.path("meta").path("detail").s("uniseq")
        assertEquals(
            "子记录",
            ForwardReader.rows(sections, name, Peer(2, "100")).single().path("elements")[0].path("textElement")
                .s("content")
        )
    }
}
