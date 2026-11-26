package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeOutgoingPart
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.m
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.Wire.n
import tools.jackson.databind.JsonNode

/**
 * 商城表情和互动表情的原生及转发编码
 */
internal object NativeSpecialMessages {
    fun element(part: QQNativeOutgoingPart): JsonNode = when (part) {
        is QQNativeOutgoingPart.MarketFace -> {
            require(part.id.matches(Regex("[a-fA-F0-9]{32}")) && part.packageId > 0 && part.key.isNotBlank()) { "商城表情参数无效" }
            j(
                obj(
                    "elementType" to 11, "elementId" to "", "marketFaceElement" to obj(
                        "emojiId" to part.id,
                        "emojiPackageId" to part.packageId,
                        "key" to part.key,
                        "faceName" to part.name
                    )
                )
            )
        }

        is QQNativeOutgoingPart.Game -> {
            require(part.result == null || part.result in 1..if (part.dice) 6 else 3) { "游戏结果超出范围" }
            j(
                obj(
                    "elementType" to 6, "elementId" to "", "faceElement" to (obj(
                        "faceIndex" to if (part.dice) 358 else 359,
                        "faceType" to 3,
                        "faceText" to if (part.dice) "[骰子]" else "[包剪锤]",
                        "packId" to "1",
                        "stickerId" to if (part.dice) "33" else "34",
                        "sourceType" to 1,
                        "stickerType" to 2,
                        "surpriseId" to ""
                    ) +
                            (part.result?.let { obj("resultId" to it.toString()) } ?: emptyMap()))))
        }

        else -> error("需要商城表情或互动表情")
    }

    fun forward(part: QQNativeOutgoingPart): EncodedPart {
        element(part)
        return when (part) {
            is QQNativeOutgoingPart.MarketFace -> EncodedPart(
                listOf(
                    m(
                        6,
                        m(1, part.name),
                        n(2, 6),
                        n(3, 1),
                        m(4, part.id.chunked(2).map { it.toInt(16).toByte() }.toByteArray()),
                        n(5, part.packageId),
                        n(6, 3),
                        m(7, part.key),
                        n(10, 300),
                        n(11, 300),
                        m(13, n(8, 1))
                    )
                ), part.name
            )

            is QQNativeOutgoingPart.Game -> {
                val name = if (part.dice) "[骰子]" else "[包剪锤]"
                val face = cat(
                    m(1, "1"),
                    m(2, if (part.dice) "33" else "34"),
                    n(3, if (part.dice) 358 else 359),
                    n(4, 1),
                    n(5, 2),
                    *(part.result?.let { listOf(m(6, it.toString())) } ?: emptyList()).toTypedArray(),
                    n(9, 1))
                EncodedPart(listOf(m(53, n(1, 37), m(2, face), n(3, 1)), m(1, m(1, name))), name)
            }

            else -> error("需要商城表情或互动表情")
        }
    }
}
