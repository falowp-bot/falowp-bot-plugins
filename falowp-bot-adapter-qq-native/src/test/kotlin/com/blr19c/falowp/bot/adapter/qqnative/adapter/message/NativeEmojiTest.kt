package com.blr19c.falowp.bot.adapter.qqnative.adapter.message

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeEmojiTest {
    @Test
    fun knownFacesAndUnicodeHaveNamesAndUnknownIdsAreRetained() {
        val face = NativeEmoji.resolve("14")
        assertEquals("little_emoji", face.type)
        assertEquals("微笑", face.display)
        assertEquals("big_emoji", NativeEmoji.resolve("358").type)
        assertEquals("骰子", NativeEmoji.resolve("358").display)
        assertEquals("👍", NativeEmoji.resolve("128077").display)
        assertEquals("unicode_emoji", NativeEmoji.resolve("128077").type)
        val unknown = NativeEmoji.resolve("99999999")
        assertEquals("99999999", unknown.id)
        assertTrue(unknown.type.isNotBlank() && unknown.display.contains(unknown.id))
    }
}
