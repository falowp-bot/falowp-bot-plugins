package com.blr19c.falowp.bot.adapter.qqnative.adapter

import com.blr19c.falowp.bot.adapter.qqnative.RecordingAccount
import com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.nativeNotice
import com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event.QQNativeInputStatusEvent
import com.blr19c.falowp.bot.adapter.qqnative.adapter.notice.event.QQNativeMessageReactionEvent
import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation
import com.blr19c.falowp.bot.adapter.qqnative.core.message.*
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.*
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.listener.events.*
import com.blr19c.falowp.bot.system.listener.hooks.*
import com.blr19c.falowp.bot.system.plugin.event.EventPluginRegister
import com.blr19c.falowp.bot.system.plugin.hook.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class NativeNoticeHooksTest {
    @Test
    fun reactionEventExposesFullFrameworkMessageAndReactionCollection(): Unit = runBlocking(Dispatchers.IO) {
        val finished = CompletableDeferred<Unit>()
        val account = RecordingAccount("9", "Bot").account
        val original = QQNativeMessage(
            QQNativeConversation.Group("42"), "42",
            QQNativeMessageReceipt("message", "19", "100", "11"), "发送者", 2,
            listOf(QQNativeReceivedPart.Text("原文内容"))
        )
        val listener = EventPluginRegister(QQNativeMessageReactionEvent::class, { event ->
            try {
                assertEquals("message", event.message?.id)
                assertEquals("11", event.message?.sender?.id)
                assertEquals("66", event.reactions.single().emoji.id)
                assertEquals("little_emoji", event.reactions.single().emoji.type)
                assertEquals("爱心", event.reactions.single().emoji.display)
                assertEquals(4, event.reactions.single().count)
                finished.complete(Unit)
            } catch (error: Throwable) {
                finished.completeExceptionally(error)
            }
        }, javaClass.kotlin)
        listener.register()
        try {
            nativeNotice(
                QQNativeNotice(
                    QQNativeConversation.Group("42"), "12",
                    QQNativeNoticeDetail.MessageReaction(
                        "message",
                        "19",
                        listOf(QQNativeReaction("66", 4)),
                        true,
                        original
                    )
                ), account
            )
            withTimeout(5000.milliseconds) { finished.await() }
        } finally {
            listener.unregister()
        }
    }

    @Test
    fun dedicatedPrivateNoticeRetainsAccountAndSendHooks(): Unit = runBlocking(Dispatchers.IO) {
        val finished = CompletableDeferred<Unit>()
        val sent = CompletableDeferred<RecordingAccount.Sent>()
        val hooks = AtomicInteger()
        val account = RecordingAccount("9", "Bot", onSend = { sent.complete(it) }).account
        val before = HookPluginRegister(
            0, SendMessageHook::class, HookTypeEnum.BEFORE,
            block = { hooks.incrementAndGet() }, originalClass = javaClass.kotlin
        )
        val listener = EventPluginRegister(QQNativeInputStatusEvent::class, { event ->
            try {
                assertEquals("正在输入", event.text)
                assertEquals("11", event.source.id)
                sendReply("收到")
                finished.complete(Unit)
            } catch (error: Throwable) {
                finished.completeExceptionally(error)
            }
        }, javaClass.kotlin)
        before.register()
        listener.register()
        try {
            nativeNotice(
                QQNativeNotice(
                    QQNativeConversation.Private("11"),
                    "11",
                    QQNativeNoticeDetail.InputStatus(1, "正在输入")
                ), account
            )
            withTimeout(5000.milliseconds) { finished.await() }
            val result = withTimeout(5000.milliseconds) { sent.await() }
            assertEquals(QQNativeConversation.Private("11"), result.to)
            assertEquals(1, hooks.get())
        } finally {
            before.unregister(); listener.unregister()
        }
    }

    @Test
    fun noticeReplyRunsSystemSendHookAndEventOnceWithEnrichedActor(): Unit = runBlocking(Dispatchers.IO) {
        val sent = CompletableDeferred<RecordingAccount.Sent>()
        val published = CompletableDeferred<SendMessageEvent>()
        val finished = CompletableDeferred<Unit>()
        val hooks = AtomicInteger()
        val account = RecordingAccount("9", "Bot", onSend = { sent.complete(it) }).account
        val before = HookPluginRegister(0, SendMessageHook::class, HookTypeEnum.BEFORE, block = { hook ->
            hooks.incrementAndGet()
            hook.sendMessageChain.clear()
            hook.sendMessageChain.add(SendMessage.builder("hook 修改后的回复").build())
        }, originalClass = javaClass.kotlin)
        val sendEvent =
            EventPluginRegister(SendMessageEvent::class, { event -> published.complete(event) }, javaClass.kotlin)
        val nudgeEvent = EventPluginRegister(NudgeEvent::class, { event ->
            try {
                assertEquals("管理员名片", event.actor.nickname)
                assertEquals(ApiAuth.MANAGER, event.actor.auth)
                sendReply("原始回复")
                finished.complete(Unit)
            } catch (error: Throwable) {
                finished.completeExceptionally(error)
            }
        }, javaClass.kotlin)
        before.register()
        sendEvent.register()
        nudgeEvent.register()
        try {
            nativeNotice(
                QQNativeNotice(
                    QQNativeConversation.Group("42"),
                    "11",
                    QQNativeNoticeDetail.Nudge("9"),
                    QQNativeNoticeUser("11", "管理员名片", 3)
                ), account
            )
            withTimeout(5000.milliseconds) { finished.await() }
            val outgoing = withTimeout(5000.milliseconds) { sent.await() }
            withTimeout(5000.milliseconds) { published.await() }
            assertEquals(1, hooks.get())
            assertEquals("42", outgoing.to.id)
            assertEquals(
                "hook 修改后的回复",
                (outgoing.chains.single().parts.single() as QQNativeOutgoingPart.Text).value
            )
        } finally {
            before.unregister()
            sendEvent.unregister()
            nudgeEvent.unregister()
        }
    }

    @Test
    fun sendHookRejectionPreventsNativeSendAndReachesExceptionAndFinallyHooks(): Unit = runBlocking(Dispatchers.IO) {
        val finished = CompletableDeferred<Unit>()
        val throwing = AtomicInteger()
        val finalizing = AtomicInteger()
        val account = RecordingAccount("9", "Bot").account
        val before = HookPluginRegister(
            0,
            SendMessageHook::class,
            HookTypeEnum.BEFORE,
            block = { error("blocked by hook") },
            originalClass = javaClass.kotlin
        )
        val afterThrow = HookPluginRegister(0, SendMessageHook::class, HookTypeEnum.AFTER_THROWING, block = {
            throwing.incrementAndGet()

        }, originalClass = javaClass.kotlin)
        val afterFinally = HookPluginRegister(0, SendMessageHook::class, HookTypeEnum.AFTER_FINALLY, block = {
            finalizing.incrementAndGet()

        }, originalClass = javaClass.kotlin)
        val listener = EventPluginRegister(NudgeEvent::class, {
            try {
                assertFailsWith<IllegalStateException> { sendReply("blocked") }
                finished.complete(Unit)
            } catch (error: Throwable) {
                finished.completeExceptionally(error)
            }
        }, javaClass.kotlin)
        before.register()
        afterThrow.register()
        afterFinally.register()
        listener.register()
        try {
            nativeNotice(
                QQNativeNotice(QQNativeConversation.Private("11"), "11", QQNativeNoticeDetail.Nudge("9")),
                account
            )
            withTimeout(5000.milliseconds) { finished.await() }
            assertEquals(1, throwing.get())
            assertEquals(1, finalizing.get())
        } finally {
            before.unregister()
            afterThrow.unregister()
            afterFinally.unregister()
            listener.unregister()
        }
    }
}
