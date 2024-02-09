package com.blr19c.falowp.bot.user.plugins.user

import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Hook.Companion.beforeHook
import com.blr19c.falowp.bot.user.database.BotUser
import com.blr19c.falowp.bot.user.database.BotUser.userId
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

/**
 * 通过hook增加用户信息
 */
@Plugin(name = "用户信息", hidden = true)
class BotUserHook {

    private val userInfoHook = beforeHook<ReceiveMessageHook> { (receiveMessage) ->
        val botApi = this.botApi()

        val avatarUrl = receiveMessage.sender.avatar.toUrl()

        val currentUser = botApi.currentUserOrNull()
        if (currentUser == null) {
            transaction {
                BotUser.insertIgnore {
                    it[userId] = receiveMessage.sender.id
                    it[nickname] = receiveMessage.sender.nickname
                    it[avatar] = avatarUrl
                    it[auth] = receiveMessage.sender.auth.name
                    it[impression] = BigDecimal.ZERO
                    it[coins] = BigDecimal.ZERO
                    it[sourceId] = receiveMessage.source.id
                    it[sourceType] = receiveMessage.source.type.name
                }
            }
        } else {
            transaction {
                BotUser.update({ userId eq receiveMessage.sender.id }) {
                    it[nickname] = receiveMessage.sender.nickname
                    it[avatar] = avatarUrl
                    it[auth] = receiveMessage.sender.auth.name
                }
            }
        }
    }

    init {
        userInfoHook.register()
    }
}