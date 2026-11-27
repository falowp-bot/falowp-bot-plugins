package com.blr19c.falowp.bot.adapter.qq.core.client

/**
 * QQ REST 请求失败
 * @property status HTTP 状态码
 * @property code 平台业务错误码，无可解析错误码时为 null
 * @property traceId 平台链路追踪 ID
 */
class QQApiException(val status: Int, val code: Long?, val traceId: String?, detail: String) :
    RuntimeException("QQ API 失败: HTTP=$status, code=$code, traceId=$traceId, $detail")
