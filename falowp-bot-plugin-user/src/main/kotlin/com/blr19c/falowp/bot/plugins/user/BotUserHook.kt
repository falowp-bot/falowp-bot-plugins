package com.blr19c.falowp.bot.plugins.user

import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.plugins.user.database.BotUser
import com.blr19c.falowp.bot.plugins.user.database.BotUser.sourceId
import com.blr19c.falowp.bot.plugins.user.database.BotUser.userId
import com.blr19c.falowp.bot.plugins.user.vo.BotUserVo
import com.blr19c.falowp.bot.system.api.BotApi
import com.blr19c.falowp.bot.system.api.ReceiveMessage
import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Hook.Companion.beforeHook
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

/**
 * 通过hook增加用户信息
 */
@Plugin(name = "用户信息", hidden = true)
class BotUserHook {

    private val userInfoHook = beforeHook<ReceiveMessageHook> { (receiveMessage) ->
        val botApi = this.botApi()

        //当前人
        val currentUser = botApi.currentUserOrNull()
        botApi.createUser(currentUser, receiveMessage.sender)
        //被at的人
        for (user in receiveMessage.content.at) {
            val atUser = queryByUserId(user.id, receiveMessage.source.id)
            botApi.createUser(atUser, user)
        }
    }


    private suspend fun BotApi.createUser(botUser: BotUserVo?, user: ReceiveMessage.User) {
        val avatarUrl = user.avatar.toUrl()
        if (botUser == null) {
            multiTransaction {
                BotUser.insertIgnore {
                    it[userId] = user.id
                    it[nickname] = user.nickname
                    it[avatar] = avatarUrl
                    it[auth] = user.auth.name
                    it[impression] = BigDecimal.ZERO
                    it[coins] = BigDecimal.ZERO
                    it[sourceId] = receiveMessage.source.id
                    it[sourceType] = receiveMessage.source.type.name
                }
            }
        } else {
            multiTransaction {
                BotUser.update({ (userId eq user.id).and(sourceId eq receiveMessage.source.id) }) {
                    it[nickname] = user.nickname
                    it[avatar] = avatarUrl
                    it[auth] = user.auth.name
                }
            }
        }
    }

    init {
        userInfoHook.register()
    }
}