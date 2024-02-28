package com.blr19c.falowp.bot.auth.plugins.ban

import com.blr19c.falowp.bot.auth.plugins.ban.database.BanInfo
import com.blr19c.falowp.bot.auth.plugins.ban.database.BanInfo.sourceId
import com.blr19c.falowp.bot.auth.plugins.ban.database.BanInfo.userId
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.plugin.MessagePluginRegisterMatch
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Listener.Hook.Companion.beforeHook
import com.blr19c.falowp.bot.system.plugin.Plugin.Message.message
import io.ktor.util.collections.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 禁用用户
 */
@Plugin(name = "禁用用户", tag = "权限", desc = "ban @禁用的人 unban@解禁的人")
class Ban {

    private val banSet by lazy {
        val concurrentSet = ConcurrentSet<String>()
        concurrentSet.addAll(
            transaction {
                BanInfo.selectAll().map { it[userId] + it[sourceId] }.toSet()
            }
        )
        concurrentSet
    }

    private val ban = message(MessagePluginRegisterMatch(Regex("ban"), auth = ApiAuth.ADMINISTRATOR, atMe = true)) {
        val banList = transaction {
            val banList = listOf<String>()
            for (user in this@message.receiveMessage.content.at) {
                if (user.auth == ApiAuth.ADMINISTRATOR) continue
                BanInfo.insertIgnore {
                    it[userId] = user.id
                    it[sourceId] = receiveMessage.source.id
                }
                banList.addLast(user.nickname)
                banSet.add(user.id + receiveMessage.source.id)
            }
            banList
        }
        if (banList.isNotEmpty()) {
            this.sendReply("已禁用用户:${banList.joinToString(",")}", reference = true)
        }
    }

    private val unban = message(MessagePluginRegisterMatch(Regex("unban"), auth = ApiAuth.ADMINISTRATOR, atMe = true)) {
        val unbanList = transaction {
            val unbanList = listOf<String>()
            for (user in this@message.receiveMessage.content.at) {
                BanInfo.deleteWhere {
                    (userId eq user.id).and(sourceId eq receiveMessage.source.id)
                }
                unbanList.addLast(user.nickname)
                banSet.remove(user.id + receiveMessage.source.id)
            }
            unbanList
        }
        if (unbanList.isNotEmpty()) {
            this.sendReply("已解禁用户:${unbanList.joinToString(",")}", reference = true)
        }
    }

    private val banHook = beforeHook<ReceiveMessageHook>(order = Int.MIN_VALUE) { (receiveMessage) ->
        if (banSet.contains(receiveMessage.sender.id + receiveMessage.source.id)) this.terminate()
    }

    init {
        ban.register()
        unban.register()
        banHook.register()
    }
}