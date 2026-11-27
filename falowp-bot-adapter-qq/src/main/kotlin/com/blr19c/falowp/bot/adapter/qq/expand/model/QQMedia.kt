package com.blr19c.falowp.bot.adapter.qq.expand.model

import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import tools.jackson.databind.JsonNode

/**
 * 上传结果绑定目标，并保留平台的有效期，禁止跨目标复用
 */
data class QQMedia(
    /**
     * 发送富媒体时使用的文件令牌
     */
    val fileInfo: String,
    /**
     * 平台文件标识
     */
    val fileUuid: String,
    /**
     * 上传目标的用户或群 OpenID
     */
    val sourceId: String,
    /**
     * 上传目标类型，仅支持单聊和群聊
     */
    val sourceType: SourceTypeEnum,
    /**
     * 过期时间的 Unix 毫秒值，null 表示平台声明长期有效
     */
    val expiresAt: Long?,
    /**
     * 平台完整上传响应
     */
    val raw: JsonNode,
)
