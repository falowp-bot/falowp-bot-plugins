package com.blr19c.falowp.bot.adapter.qq.api

import com.blr19c.falowp.bot.adapter.qq.core.event.QQEventDispatcher
import com.blr19c.falowp.bot.adapter.qq.core.client.QQClient
import com.blr19c.falowp.bot.adapter.qq.core.config.QQGroupMessageMode
import com.blr19c.falowp.bot.adapter.qq.core.message.QQMessageConverter
import com.blr19c.falowp.bot.adapter.qq.core.state.QQState
import com.blr19c.falowp.bot.system.api.*
import com.blr19c.falowp.bot.system.json.safeString
import com.blr19c.falowp.bot.system.plugin.PluginManagement
import com.blr19c.falowp.bot.system.scheduling.api.SchedulingBotApiSupport
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass
import tools.jackson.databind.JsonNode

/**
 * 连接 QQ 接入层与框架消息系统
 */
object QQBotApiSupport : SchedulingBotApiSupport {
    /**
     * 本次进程中已观察到、且尚未收到退出事件的群 OpenID 集合
     */
    internal val groupIdList = CopyOnWriteArraySet<String>()
    private val privateIds = CopyOnWriteArraySet<String>()
    private val received = QQState()
    internal val sequences = QQState()
    internal lateinit var client: QQClient

    @Volatile
    internal var botSelf: BotSelf = BotSelf.Default()

    @Volatile
    private var groupMessageMode = QQGroupMessageMode.ALL

    /**
     * 初始化共享客户端、机器人身份与群消息过滤模式
     */
    internal fun configure(client: QQClient, appId: String, groupMode: QQGroupMessageMode = QQGroupMessageMode.ALL) {
        groupMessageMode = groupMode
        this.client = client
        botSelf = BotSelf.Default(appId)
    }

    /**
     * 判断本进程是否已观察到目标会话
     * @param receiveId 用户或群 OpenID
     * @return 是否可以为该目标创建定时任务 API
     */
    override suspend fun supportReceive(receiveId: String): Boolean =
        receiveId in groupIdList || receiveId in privateIds

    /**
     * 为已观察到的目标创建无消息上下文的 API
     * @param receiveId 用户或群 OpenID
     * @param originalClass 发起调用的插件类型
     * @return 指向目标会话的 QQ API
     */
    override suspend fun bot(receiveId: String, originalClass: KClass<*>): BotApi {
        val type = when (receiveId) {
            in groupIdList -> SourceTypeEnum.GROUP
            in privateIds -> SourceTypeEnum.PRIVATE
            else -> throw IllegalArgumentException("QQ 未观察到目标 $receiveId")
        }
        return QQBotApi(
            ReceiveMessage.empty().copy(
                source = ReceiveMessage.Source(receiveId, type), self = botSelf,
                adapter = ReceiveMessage.Adapter("QQ", "scheduled")
            ), originalClass
        )
    }

    /**
     * 处理关系变化事件，对消息过滤、去重后发布
     */
    internal fun dispatchMessage(
        event: JsonNode,
        publish: (ReceiveMessage) -> Unit = { PluginManagement.message(it, QQBotApi::class) },
    ) {
        val type = event.path("t").safeString()
        val data = event.path("d")
        val group = data.path("group_openid").safeString()
        val user = data.path("openid").safeString().ifBlank { data.path("user_openid").safeString() }
        when (type) {
            "GROUP_ADD_ROBOT" -> {
                if (group.isNotBlank()) groupIdList.add(group)
            }

            "GROUP_DEL_ROBOT" -> {
                groupIdList.remove(group)
            }

            "FRIEND_ADD" -> {
                if (user.isNotBlank()) privateIds.add(user)
            }

            "FRIEND_DEL" -> {
                privateIds.remove(user)
            }
        }
        if (QQEventDispatcher.dispatch(event)) return
        val message = QQMessageConverter.convert(event, botSelf, groupMessageMode) ?: return
        val key = "${message.source.type}:${message.source.id}:${message.id}:${data.path("msg_seq").safeString()}"
        if (received.next(key) != 1) return
        try {
            if (message.group()) groupIdList.add(message.source.id) else privateIds.add(message.source.id)
            publish(message)
        } catch (e: Exception) {
            received.forget(key)
            throw e
        }
    }

}
