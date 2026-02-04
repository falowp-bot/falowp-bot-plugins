package com.blr19c.falowp.bot.adapter.nc.notice.handlers

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApiSupport
import com.blr19c.falowp.bot.adapter.nc.expand.NapCatFileApiExpand
import com.blr19c.falowp.bot.adapter.nc.expand.getGroupFileUrl
import com.blr19c.falowp.bot.adapter.nc.notice.NapCatNotice
import com.blr19c.falowp.bot.adapter.nc.notice.event.NapCatGroupUploadEvent
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.api.SourceTypeEnum
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import tools.jackson.databind.JsonNode

/**
 * 群聊文件上传
 */
object NapCatGroupUploadNotice : NapCatNotice.NapCatNoticeInterface {

    override suspend fun toBotEvent(originalMessage: JsonNode): Plugin.Listener.Event {
        log().info("NapCat-通知-群聊文件上传:{}", originalMessage)

        val groupId = originalMessage.path("group_id").safeString()
        val userId = originalMessage.path("user_id").safeString()
        val fileId = originalMessage.path("file").path("id").safeString()
        val fileName = originalMessage.path("file").path("name").safeString()
        val fileSize = originalMessage.path("file").path("size").safeString()
        val busid = originalMessage.path("file").path("busid").safeString()

        val source = ReceiveMessage.Source(groupId, SourceTypeEnum.GROUP)
        val actor = NapCatBotApiSupport.getGroupMemberInfo(groupId, userId)
        val groupFileUrl = NapCatBotApiSupport.tempBot.getGroupFileUrl(groupId, fileId, busid.toIntOrNull())
        val groupFileInfo = NapCatFileApiExpand.FileInfo(fileId, fileName, fileSize.toLong(), busid.toIntOrNull())

        return NapCatGroupUploadEvent(source, actor, groupFileUrl, groupFileInfo)
    }
}