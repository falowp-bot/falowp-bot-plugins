package com.blr19c.falowp.bot.plugins.auth

import com.blr19c.falowp.bot.plugins.auth.database.BanInfo
import com.blr19c.falowp.bot.plugins.auth.database.BanInfo.sourceId
import com.blr19c.falowp.bot.plugins.auth.database.BanInfo.userId
import com.blr19c.falowp.bot.plugins.db.multiTransaction
import com.blr19c.falowp.bot.system.api.ApiAuth
import com.blr19c.falowp.bot.system.listener.hooks.EventPluginExecutionHook
import com.blr19c.falowp.bot.system.listener.hooks.ReceiveMessageHook
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.hook.beforeHook
import com.blr19c.falowp.bot.system.plugin.message.message
import io.ktor.util.collections.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * 禁用用户
 */
@Plugin(name = "禁用用户", tag = "权限", desc = "ban @禁用的人 unban@解禁的人")
class Ban {

    private val banSet by lazy {
        val concurrentSet = ConcurrentSet<String>()
        concurrentSet.addAll(
            multiTransaction {
                BanInfo.selectAll().map { it[userId] + it[sourceId] }.toSet()
            }
        )
        concurrentSet
    }

    private val ban = message(Regex("ban"), auth = ApiAuth.ADMINISTRATOR) {
        val banList = multiTransaction {
            val banList = mutableListOf<String>()
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

    private val unban = message(Regex("unban"), auth = ApiAuth.ADMINISTRATOR) {
        val unbanList = multiTransaction {
            val unbanList = mutableListOf<String>()
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

    private val banMessageHook = beforeHook<ReceiveMessageHook>(order = Int.MIN_VALUE) { (receiveMessage) ->
        if (banSet.contains(receiveMessage.sender.id + receiveMessage.source.id)) this.terminate()
    }

    private val banEventHook = beforeHook<EventPluginExecutionHook>(order = Int.MIN_VALUE) { (event) ->
        if (banSet.contains(event.actor.id + event.source.id)) this.terminate()
    }

    init {
        ban.register()
        unban.register()
        banMessageHook.register()
        banEventHook.register()
    }
}