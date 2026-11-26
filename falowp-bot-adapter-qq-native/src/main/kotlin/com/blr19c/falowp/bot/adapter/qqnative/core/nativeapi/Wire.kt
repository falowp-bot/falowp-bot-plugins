package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.*

internal val mapper = JsonMapper.builder().build()
internal fun j(value: Any?): JsonNode = mapper.valueToTree(value)
internal fun JsonNode.s(key: String) = path(key).asString("")
internal fun JsonNode.i(key: String) = path(key).asInt(0)
internal fun JsonNode.l(key: String) = path(key).asLong(0)
internal fun JsonNode.list(key: String) = path(key).toList()
internal fun JsonNode.hasValue(key: String) = has(key) && !path(key).isNull
internal fun obj(vararg pairs: Pair<String, Any?>) = linkedMapOf(*pairs)
internal fun binary(bytes: ByteArray) = obj($$"$buffer" to Base64.getEncoder().encodeToString(bytes))
internal fun JsonNode.binary(): ByteArray = Base64.getDecoder().decode(s($$"$buffer"))
internal fun nativeMap(vararg pairs: Pair<Any, Any?>) = obj($$"$map" to pairs.map { listOf(it.first, it.second) })
internal fun JsonNode.mapEntries() = list($$"$map").map { it[0].asString() to it[1] }
internal fun cat(vararg parts: ByteArray): ByteArray =
    ByteArrayOutputStream().apply { parts.forEach { write(it) } }.toByteArray()

internal fun text(bytes: ByteArray) = bytes.toString(Charsets.UTF_8)
internal fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 255) }

/**
 * 编解码所需的 Protobuf 线格式，并完整保留无符号 64 位标识的位模式
 */
internal object Wire {
    fun varint(value: ULong): ByteArray {
        var n = value
        val out = ByteArrayOutputStream()
        do {
            val b = (n and 127u).toInt()
            n = n shr 7
            out.write(b or if (n != 0uL) 128 else 0)
        } while (n != 0uL)
        return out.toByteArray()
    }

    fun n(field: Int, value: Number) = cat(varint((field.toULong() shl 3)), varint(value.toLong().toULong()))
    fun m(field: Int, vararg parts: ByteArray): ByteArray {
        val bytes = cat(*parts)
        return cat(varint((field.toULong() shl 3) or 2u), varint(bytes.size.toULong()), bytes)
    }

    fun m(field: Int, value: String) = m(field, value.toByteArray())
    fun fields(bytes: ByteArray): Map<Int, List<Any>> {
        var offset = 0
        fun integer(): ULong {
            var n = 0uL
            for (shift in 0..63 step 7) {
                require(offset < bytes.size) { "Truncated protobuf integer" }
                val b = bytes[offset++].toInt() and 255
                require(shift != 63 || b <= 1) { "Protobuf integer overflow" }
                n = n or ((b and 127).toULong() shl shift)
                if (b < 128) return n
            }
            error("Protobuf integer overflow")
        }

        val out = linkedMapOf<Int, MutableList<Any>>()
        while (offset < bytes.size) {
            val tag = integer()
            val field = (tag shr 3).toInt()
            val wire = (tag and 7u).toInt()
            require(field in 1..0x1fffffff) { "Invalid protobuf field" }
            val value: Any = if (wire == 0) integer() else {
                val size = when (wire) {
                    2 -> integer()
                    1 -> 8uL
                    5 -> 4uL
                    else -> error("Invalid protobuf wire")
                }
                require(size <= (bytes.size - offset).toULong()) { "Invalid protobuf length" }
                bytes.copyOfRange(offset, offset + size.toInt()).also { offset += size.toInt() }
            }
            out.getOrPut(field) { mutableListOf() }.add(value)
        }
        return out
    }

    fun all(bytes: ByteArray, field: Int) = fields(bytes)[field].orEmpty().filterIsInstance<ByteArray>()
    fun b(bytes: ByteArray, field: Int) = all(bytes, field).firstOrNull() ?: byteArrayOf()
    fun u(bytes: ByteArray, field: Int) = fields(bytes)[field].orEmpty().filterIsInstance<ULong>().firstOrNull() ?: 0uL
    fun s(bytes: ByteArray, field: Int) = text(b(bytes, field))
}

internal fun gzip(bytes: ByteArray) =
    ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()

internal fun deflate(bytes: ByteArray) =
    ByteArrayOutputStream().also { out -> DeflaterOutputStream(out).use { it.write(bytes) } }.toByteArray()

internal fun unpack(bytes: ByteArray, gzip: Boolean = false, limit: Int = 1024 * 1024): ByteArray {
    val stream = if (gzip) GZIPInputStream(bytes.inputStream()) else InflaterInputStream(bytes.inputStream())
    return stream.use {
        val data = it.readNBytes(limit + 1)
        require(data.size <= limit) { "解压结果过大" }
        data
    }
}
