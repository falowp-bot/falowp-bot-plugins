package com.blr19c.falowp.bot.adapter.qq.op

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

/**
 * 附件
 */
data class OpAttachment(
    /**
     * 资源id
     */
    val id: String?,

    /**
     * 资源地址
     */
    val url: URI,

    /**
     * 资源ContentType
     */
    @field:JsonProperty("content_type")
    val contentType: String,

    /**
     * 资源名称
     */
    val filename: String?,

    /**
     * 资源大小
     */
    val size: Long?,

    /**
     * 如果是图片,则存在图片属性height
     */
    val height: Int?,

    /**
     * 如果是图片,则存在图片属性width
     */
    val width: Int?
)
