package com.blr19c.falowp.bot.adapter.qqnative.core.nativeapi

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class NativeFriendListTest {
    @Test
    fun followsOpaqueCookiesAndDeduplicatesOverlappingPages(): Unit = runBlocking {
        val cookies = mutableListOf<String>()
        val list = NativeFriendList(NativeBridge { _, args ->
            val p = j(args).path("arguments")
            val request = mapper.readTree(Wire.s(p[1].binary(), 3))
            assertEquals("9", request.s("uint64_uin"))
            assertEquals(99, request.i("uint32_req_num"))
            val cookie = request.s("bytes_cookies")
            cookies.add(cookie)
            val ids = if (cookie.isEmpty()) (1..99).toList() else listOf(99, 100)
            val reply = obj(
                "ErrorCode" to 0, "ActionStatus" to "OK", "bytes_cookies" to if (cookie.isEmpty()) "opaque+/=" else "",
                "rpt_block_list" to ids.map {
                    obj(
                        "uint64_uin" to it.toString(),
                        "bytes_nick" to "5pi156ew",
                        "bytes_source" to "",
                        "str_uid" to "u_$it"
                    )
                })
            j(obj("result" to 0, "rspbuffer" to binary(Wire.m(4, mapper.writeValueAsString(reply)))))
        }, "9")
        val rows = list.all().toList()
        assertEquals(listOf("", "opaque+/="), cookies)
        assertEquals(100, rows.size)
        assertEquals("昵称", rows.first().s("nick_name"))
    }

    @Test
    fun rejectsRepeatedCookiesAndDoesNotReturnTruncatedFullPages(): Unit = runBlocking {
        for (cookie in listOf("repeated", "")) {
            var calls = 0
            val list = NativeFriendList(NativeBridge { _, _ ->
                calls++
                val reply = obj("rpt_block_list" to (1..99).map { obj("uint64_uin" to it.toString()) }) +
                        if (cookie.isBlank()) emptyMap() else obj("bytes_cookies" to cookie)
                j(obj("result" to 0, "rspbuffer" to binary(Wire.m(4, mapper.writeValueAsString(reply)))))
            }, "9")
            assertFailsWith<IllegalStateException> { list.all() }
            assertEquals(if (cookie.isBlank()) 1 else 2, calls)
            val page = list.page("", 99)
            assertFalse(page.path("complete").asBoolean())
            assertEquals(99, page.list("friends").size)
        }
    }

    @Test
    fun serverErrorsAreNotReportedAsEmptyLists(): Unit = runBlocking {
        val list = NativeFriendList(NativeBridge { _, _ ->
            j(obj("result" to 0, "rspbuffer" to binary(Wire.m(4, """{"ErrorCode":42,"ErrorInfo":"denied"}"""))))
        }, "9")
        assertFailsWith<IllegalStateException> { list.all() }
    }
}
