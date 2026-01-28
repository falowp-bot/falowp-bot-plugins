package com.blr19c.falowp.bot.adapter.nc.notice.event

import com.blr19c.falowp.bot.adapter.nc.expand.NapCatFileExpand
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
    val fileUrl: NapCatFileExpand.GroupFileUrl,
    /**
     * 文件信息
     */
    val fileInfo: NapCatFileExpand.GroupFileInfo
) : Plugin.Listener.Event
