package com.blr19c.falowp.bot.plugins.bili.api.data

import com.blr19c.falowp.bot.system.json.safeString
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * 直播间的基本信息
 */
data class BiliRoomInfo(
    /**
     * 主播 UID
     */
    @field:JsonProperty("uid")
    val uid: String,
    /**
     * 直播间 ID
     */
    @field:JsonProperty("room_id")
    val roomId: String,
    /**
     * 直播状态 0 未开播 1 直播中 2 轮播中
     */
    @field:JsonProperty("live_status")
    val liveStatus: Int,
    /**
     * 直播间标题
     */
    @field:JsonProperty("title")
    val title: String,
    /**
     * 直播间封面
     */
    @field:JsonProperty("cover_from_user")
    val cover: String,
    /**
     * 主播昵称
     */
    @field:JsonProperty("uname")
    val name: String,
    /**
     * 主播头像
     */
    @field:JsonProperty("face")
    val face: String
)

/**
 * B 站直播信息流消息
 *
 * @property type 经过归类的消息类型
 * @property command B 站返回的原始 `cmd`
 * @property data 能直接使用的消息内容 没有对应模型时为 `null`
 * @property raw 未经转换的完整消息 JSON
 */
data class BiliLiveStreamMessage(
    val type: BiliLiveMessageType,
    val command: String,
    val data: BiliLiveMessageData?,
    val raw: JsonNode,
) {
    companion object {
        /**
         * 把原始消息整理成方便使用的格式
         */
        fun from(raw: JsonNode): BiliLiveStreamMessage {
            val command = raw.path("cmd").safeString()
            val type = BiliLiveMessageType.from(command)
            return BiliLiveStreamMessage(type, command, BiliLiveMessageData.from(type, raw), raw)
        }
    }
}

/**
 * 从直播消息里整理出的常用数据
 */
@Suppress("SpellCheckingInspection")
sealed interface BiliLiveMessageData {

    /**
     * 普通弹幕
     *
     * @property fansMedalLevel 当前佩戴的粉丝勋章等级，没有佩戴时为 0
     */
    data class Danmu(
        val userId: Long,
        val userName: String,
        val content: String,
        val fansMedalLevel: Int,
    ) : BiliLiveMessageData

    /**
     * 赠送礼物
     */
    data class Gift(
        val userId: Long,
        val userName: String,
        val giftName: String,
        val count: Long,
        val price: Long,
    ) : BiliLiveMessageData

    /**
     * 用户进入直播间
     */
    data class Enter(val userId: Long, val userName: String) : BiliLiveMessageData

    /**
     * 购买大航海 1 总督 2 提督 3 舰长
     */
    data class Guard(
        val userId: Long,
        val userName: String,
        val guardLevel: Int,
        val count: Int,
        val price: Long,
    ) : BiliLiveMessageData

    /**
     * 醒目留言
     */
    data class SuperChat(
        val userId: Long,
        val userName: String,
        val content: String,
        val price: Int,
    ) : BiliLiveMessageData

    companion object {
        internal fun from(type: BiliLiveMessageType, raw: JsonNode): BiliLiveMessageData? = when (type) {
            BiliLiveMessageType.DANMU -> raw.path("info").let { info ->
                Danmu(
                    userId = info.path(2).path(0).asLong(),
                    userName = info.path(2).path(1).safeString(),
                    content = info.path(1).safeString(),
                    fansMedalLevel = info.path(3).path(0).asInt(),
                )
            }

            BiliLiveMessageType.GIFT -> raw.path("data").let { data ->
                Gift(
                    userId = data.path("uid").asLong(),
                    userName = data.path("uname").safeString(),
                    giftName = data.path("giftName").safeString(),
                    count = data.path("num").asLong(data.path("batch_combo_num").asLong()),
                    price = data.path("price").asLong(),
                )
            }

            BiliLiveMessageType.ENTER -> raw.path("data").let { data ->
                val userName = data.path("uname").safeString()
                if (userName.isBlank()) null else Enter(data.path("uid").asLong(), userName)
            }

            BiliLiveMessageType.GUARD -> raw.path("data").let { data ->
                Guard(
                    userId = data.path("uid").asLong(),
                    userName = data.path("username").safeString().ifBlank { data.path("uname").safeString() },
                    guardLevel = data.path("guard_level").asInt(),
                    count = data.path("num").asInt(),
                    price = data.path("price").asLong(),
                )
            }

            BiliLiveMessageType.SUPER_CHAT -> raw.path("data").let { data ->
                SuperChat(
                    userId = data.path("uid").asLong(),
                    userName = data.path("user_info").path("uname").safeString(),
                    content = data.path("message").safeString(),
                    price = data.path("price").asInt(),
                )
            }

            else -> null
        }
    }
}

/**
 * B 站直播信息流的业务消息分类
 */
@Suppress("SpellCheckingInspection")
enum class BiliLiveMessageType {
    /**
     * 普通弹幕
     */
    DANMU,

    /**
     * 进入直播间或关注
     */
    ENTER,

    /**
     * 礼物和礼物连击
     */
    GIFT,

    /**
     * 购买大航海
     */
    GUARD,

    /**
     * 醒目留言和删除消息
     */
    SUPER_CHAT,

    /**
     * 开播 封禁或警告
     */
    LIVE_STATUS,

    /**
     * 直播结束
     */
    LIVE_END,

    /**
     * 标题或分区变化
     */
    ROOM_INFO,

    /**
     * 全站广播和跑马灯
     */
    NOTICE,

    /**
     * 观看和在线人数
     */
    STATISTICS,

    /**
     * 在线榜和热门榜变化
     */
    RANK,

    /**
     * 暂时没有归类的消息
     */
    OTHER;

    companion object {
        /**
         * 按 B 站的 `cmd` 归类消息
         *
         * 带冒号后缀的指令只看前面的名称
         */
        fun from(command: String): BiliLiveMessageType = when (command.substringBefore(':')) {
            "DANMU_MSG" -> DANMU
            "INTERACT_WORD", "INTERACT_WORD_V2", "ENTRY_EFFECT" -> ENTER
            "SEND_GIFT", "COMBO_SEND", "COMBO_END" -> GIFT
            "GUARD_BUY", "USER_TOAST_MSG" -> GUARD
            "SUPER_CHAT_MESSAGE", "SUPER_CHAT_MESSAGE_JPN", "SUPER_CHAT_MESSAGE_DELETE" -> SUPER_CHAT
            "PREPARING" -> LIVE_END
            "LIVE", "ROOM_BLOCK_MSG", "WARNING", "CUT_OFF" -> LIVE_STATUS
            "ROOM_CHANGE", "ROOM_REAL_TIME_MESSAGE_UPDATE" -> ROOM_INFO
            "NOTICE_MSG", "COMMON_NOTICE_DANMAKU" -> NOTICE
            "WATCHED_CHANGE", "ONLINE_RANK_COUNT" -> STATISTICS
            "POPULAR_RANK_CHANGED", "ONLINE_RANK_V2", "ONLINE_RANK_V3", "HOT_RANK_CHANGED" -> RANK
            else -> OTHER
        }
    }
}
