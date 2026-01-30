package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.adapter.nc.expand.NapCatFileApiExpand
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.plugin.Plugin

/**
 * 群聊文件上传
 */
data class NapCatGroupUploadEvent(
    /**
     * 群组
     */
    val source: ReceiveMessage.Source,
    /**
     * 用户
     */
    val user: ReceiveMessage.User,
    /**
     * 文件链接
     */
    val fileUrl: NapCatFileApiExpand.FileUrl,
    /**
     * 文件信息
     */
    val fileInfo: NapCatFileApiExpand.FileInfo
) : Plugin.Listener.Event
