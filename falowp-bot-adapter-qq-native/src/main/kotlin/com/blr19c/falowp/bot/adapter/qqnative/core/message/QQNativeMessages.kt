package com.blr19c.falowp.bot.adapter.qqnative.core.message

import com.blr19c.falowp.bot.adapter.qqnative.core.contact.QQNativeConversation

/**
 * 合并转发卡片的展示设置
 */
data class QQNativeForwardCardStyle(
    val title: String? = null,
    val summary: String? = null,
    val preview: List<String>? = null,
    val prompt: String? = null
)

/**
 * 当前账号的消息发送、转发和查询接口
 */
interface QQNativeMessages {
    /**
     * 发送消息链，replyTo 指定引用消息，forward 为 true 时合并转发
     */
    suspend fun send(
        to: QQNativeConversation,
        vararg chains: QQNativeOutgoingChain,
        replyTo: QQNativeMessage? = null,
        forward: Boolean = false
    ): List<QQNativeMessageReceipt>

    /**
     * 将指定回执对应的消息转发到目标会话
     */
    suspend fun forward(
        from: QQNativeConversation,
        to: QQNativeConversation,
        receipts: List<QQNativeMessageReceipt>
    ): QQNativeMessageReceipt

    /**
     * 转发已有的合并转发卡片，并替换展示设置
     */
    suspend fun sendForwardCard(
        from: QQNativeConversation,
        id: String,
        to: QQNativeConversation,
        style: QQNativeForwardCardStyle
    ): QQNativeMessageReceipt

    /**
     * 读取合并转发内容，parentId 用于定位嵌套转发
     */
    suspend fun readForward(
        from: QQNativeConversation,
        rootId: String,
        parentId: String = rootId
    ): List<QQNativeMessage>

    /**
     * 解析引用消息，原文不可用时返回 null
     */
    suspend fun resolve(message: QQNativeMessage, reply: QQNativeReceivedPart.Reply): QQNativeMessage?
}
