package com.blr19c.falowp.bot.adapter.qqnative.adapter

import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.listener.hooks.SendMessageHook
import com.blr19c.falowp.bot.system.plugin.PluginBotApi
import com.blr19c.falowp.bot.system.plugin.event.EventBotApi
import com.blr19c.falowp.bot.system.plugin.hook.*
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.test.*

class QQNativeBotApiConversionTest {
    private class HookOwner

    private val message = ReceiveMessage.empty().copy(
        self = BotSelf.Default(id = "9"),
        source = ReceiveMessage.Source("42", SourceTypeEnum.GROUP), adapter = ReceiveMessage.Adapter("QQNative", Unit)
    )

    @Test
    fun wrappedConversionKeepsTheOuterEventAndHookContext() {
        val native = QQNativeBotApi(message, javaClass.kotlin)
        assertSame(native, native.qqNative())
        val eventMessage = message.copy(source = ReceiveMessage.Source("84", SourceTypeEnum.GROUP))
        val event = EventBotApi(PluginBotApi(native), eventMessage)
        val register = HookPluginRegister(
            0,
            SendMessageHook::class,
            HookTypeEnum.BEFORE,
            block = {},
            originalClass = HookOwner::class
        )
        val hook = HookBotApi(event, register)
        val converted = hook.qqNative()
        assertSame(eventMessage, converted.receiveMessage)
        assertSame(HookOwner::class, converted.originalClass)
        assertEquals("9", converted.receiveMessage.self.id)
    }

    @Test
    fun schedulingSupportContextConvertsWithoutSelectingAnAccountAgain(): Unit = runBlocking(Dispatchers.IO) {
        val support = object : SchedulingBotApiSupport {
            override suspend fun supportReceive(receiveId: String) = receiveId == "42"
            override suspend fun bot(receiveId: String, originalClass: KClass<*>): QQNativeBotApi {
                check(supportReceive(receiveId))
                return QQNativeBotApi(
                    ReceiveMessage.empty().copy(self = message.self, adapter = message.adapter),
                    originalClass
                )
            }
        }
        assertTrue(support.supportReceive("42"))
        val selected = PluginBotApi(support.bot("42", javaClass.kotlin))
        val native = selected.qqNative()
        assertSame(selected.receiveMessage, native.receiveMessage)
        assertSame(javaClass.kotlin, native.originalClass)
        assertEquals("9", native.receiveMessage.self.id)
        assertEquals("QQNative", native.receiveMessage.adapter.id)
        assertEquals("", native.receiveMessage.source.id)
        // 定时任务没有可回复的消息上下文
        assertFailsWith<IllegalStateException> { native.sendReply("no conversation") }
        assertFailsWith<IllegalStateException> { native.sendReply(SendMessage.builder("no conversation").build()) }
    }
}
