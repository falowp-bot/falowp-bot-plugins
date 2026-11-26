package com.blr19c.falowp.bot.adapter.qqnative.core

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeContacts
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessage
import com.blr19c.falowp.bot.adapter.qqnative.core.message.QQNativeMessages
import com.blr19c.falowp.bot.adapter.qqnative.core.notice.QQNativeNotice
import com.blr19c.falowp.bot.adapter.qqnative.core.session.QQNativeStatus
import com.blr19c.falowp.bot.adapter.qqnative.core.transport.WorkerProcess
import tools.jackson.databind.JsonNode

/**
 * 管理原生工作进程，向适配器提供消息、通知和登录状态
 */
class QQNativeClient(
    onMessage: (QQNativeMessage, QQNativeAccount) -> Unit,
    onNotice: (QQNativeNotice, QQNativeAccount) -> Unit
) {
    private val worker = WorkerProcess(onMessage, onNotice)
    val status: QQNativeStatus get() = worker.status
    fun start() = worker.start()

    /**
     * 获取绑定指定账号的操作入口；在线账号改变后，原入口的调用会失败
     */
    fun account(id: String): QQNativeAccount = worker.account(id)
}

/**
 * 绑定具体 QQ 账号的联系人、消息与扩展操作入口
 */
class QQNativeAccount internal constructor(
    val id: String, val contacts: QQNativeContacts, val messages: QQNativeMessages, val nickname: String,
    internal val extensionCall: suspend (String, Map<String, Any?>) -> JsonNode
)
