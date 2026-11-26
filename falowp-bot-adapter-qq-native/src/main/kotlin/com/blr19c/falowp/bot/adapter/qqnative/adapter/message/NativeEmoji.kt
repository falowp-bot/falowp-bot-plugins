package com.blr19c.falowp.bot.adapter.qqnative.adapter.message

import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.mapper
import com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi.s
import com.blr19c.falowp.bot.system.api.ReceiveMessage

/**
 * QQ 系统表情名称及 Unicode 回应，未知 ID 保留原值
 */
internal object NativeEmoji {
    private val catalog by lazy {
        checkNotNull(javaClass.getResourceAsStream("/qq-native/face-catalog.json")) { "缺少 QQ 表情目录" }
            .use { mapper.readTree(it) }
    }

    fun resolve(id: String): ReceiveMessage.Emoji {
        val known = catalog.path(id)
        if (known.isObject) return Face(id, known.s("type"), known.s("display"))
        val code = id.toIntOrNull()
        if (code != null && (code in 0x1f000..0x1faff || code in 0x2600..0x27bf))
            return Face(id, "unicode_emoji", String(Character.toChars(code)))
        return Face(id, "qq_face", "[QQ表情$id]")
    }

    private data class Face(override val id: String, override val type: String, override val display: String) :
        ReceiveMessage.Emoji
}
