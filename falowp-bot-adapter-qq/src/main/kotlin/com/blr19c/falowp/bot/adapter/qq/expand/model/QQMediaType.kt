package com.blr19c.falowp.bot.adapter.qq.expand.model

/**
 * QQ 官方富媒体类型
 */
enum class QQMediaType(
    /**
     * 上传接口的 file_type 值
     */
    val code: Int
) {
    /**
     * 图片资源
     */
    IMAGE(1),

    /**
     * 视频资源
     */
    VIDEO(2),

    /**
     * 语音资源
     */
    VOICE(3),

    /**
     * 通用文件资源
     */
    FILE(4),
}
