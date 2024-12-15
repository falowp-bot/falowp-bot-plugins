package com.blr19c.falowp.bot.adapter.nc.expand

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * API结果
 */
data class NapCatQQApiResult<T>(
    /**
     * 状态
     */
    @field:JsonProperty("status")
    val status: String,
    /**
     * 状态码
     */
    @field:JsonProperty("retcode")
    val retCode: Int,
    /**
     * 错误消息
     */
    @field:JsonProperty("msg")
    val msg: String?,
    /**
     * 对错误的详细解释
     */
    @field:JsonProperty("wording")
    val wording: String?,
    /**
     * 数据
     */
    @field:JsonProperty("data")
    val data: T?,
    /**
     * 回执
     */
    @field:JsonProperty("echo")
    val echo: Any?,
) {
    /**
     * 是否成功
     */
    fun success(): Boolean {
        return status == "ok" || status == "async"
    }
}
