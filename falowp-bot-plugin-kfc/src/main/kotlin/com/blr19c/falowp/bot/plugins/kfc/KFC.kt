package com.blr19c.falowp.bot.plugins.kfc

import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.json.Json
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.task.cronScheduling
import com.blr19c.falowp.bot.system.readPluginResource
import tools.jackson.databind.node.ArrayNode
import kotlin.random.Random

/**
 * 肯德基疯狂星期四
 */
@Plugin(
    name = "肯德基疯狂星期四",
    tag = "聊天",
    desc = """
        <p>每周四12点推送</p>
    """
)
class KFC {

    private val kfc = cronScheduling("0 0 12 ? * THU") {
        val message = readPluginResource("kfc.json") {
            val jsonNode = Json.readJsonNode(it.readBytes().decodeToString()) as ArrayNode
            val randomIndex = Random.nextInt(jsonNode.size())
            jsonNode[randomIndex].safeString()
        }
        this.sendAllGroup(SendMessage.builder(message).build())
    }

    init {
        kfc.register()
    }


}