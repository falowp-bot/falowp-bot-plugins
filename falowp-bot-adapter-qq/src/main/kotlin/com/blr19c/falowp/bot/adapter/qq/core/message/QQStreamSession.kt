package com.blr19c.falowp.bot.adapter.qq.core.message

import com.blr19c.falowp.bot.adapter.qq.api.QQBotApi
import com.blr19c.falowp.bot.adapter.qq.expand.apiRequest
import com.blr19c.falowp.bot.system.json.safeString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tools.jackson.databind.JsonNode

/**
 * 管理单条流式消息的分片顺序，失败后停止继续发送
 */
class QQStreamSession internal constructor(
    private val bot: QQBotApi,
    private val path: String,
    private val context: Map<String, Any>,
    private val markdown: Boolean,
) {
    private val mutex = Mutex()
    private var id = ""
    private var index = 0
    private var closed = false

    /**
     * 追加文本，结束片发送成功后不可继续写入
     * @param content 本次追加的正文，结束片允许为空
     * @param finished 是否结束生成，默认继续生成
     * @return 平台原始 JSON 响应
     */
    suspend fun append(content: String, finished: Boolean = false): JsonNode =
        send(content, "append", finished)

    /**
     * 提交累计正文，已展示的前缀必须保持不变
     * @param content 当前累计正文，已下发前缀不可修改
     * @param finished 是否结束生成，默认继续生成
     * @return 平台原始 JSON 响应
     */
    suspend fun replace(content: String, finished: Boolean = false): JsonNode =
        send(content, "replace", finished)

    private suspend fun send(content: String, mode: String, finished: Boolean): JsonNode = mutex.withLock {
        check(!closed) { "流式消息已结束或发送失败" }
        require(content.isNotEmpty() || finished) { "非结束片内容不能为空" }
        val body = context + mapOf(
            "input_mode" to mode, "input_state" to if (finished) 10 else 1,
            "index" to index, "content_type" to if (markdown) "markdown" else "text", "content_raw" to content
        ) +
                if (id.isBlank()) emptyMap() else mapOf("stream_msg_id" to id)
        // 响应丢失时无法确认分片是否生效，不盲目重发
        closed = true
        val response = bot.apiRequest(path, body)
        val nextId = response.path("id").safeString()
        check(nextId.isNotBlank() && (id.isBlank() || nextId == id)) { "流式消息响应 ID 无效" }
        id = nextId
        index++
        closed = finished
        response
    }
}
