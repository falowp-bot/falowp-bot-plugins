package com.blr19c.falowp.bot.plugins.bili.api.data

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import java.math.BigDecimal

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
)

/**
 * 用户当前佩戴的粉丝灯牌。
 *
 * 灯牌可能属于其他直播间，可使用 [anchorId] 与当前主播 UID 比较。
 */
data class BiliFansMedal(
    val level: Int,
    val name: String,
    val anchorId: Long,
    val anchorRoomId: Long,
)

/**
 * 从直播消息里整理出的常用数据
 */
@Suppress("SpellCheckingInspection")
sealed interface BiliLiveMessageData {

    /** 用户当前佩戴的粉丝灯牌，没有携带灯牌信息时为 `null`。 */
    val fansMedal: BiliFansMedal?

    /**
     * 普通弹幕
     *
     */
    data class Danmu(
        val userId: Long,
        val userName: String,
        val content: String,
        override val fansMedal: BiliFansMedal?,
    ) : BiliLiveMessageData

    /**
     * 赠送礼物
     *
     * @property price 礼物单价，单位为金瓜子或银瓜子
     * @property totalCoin 本次送礼的实际瓜子总数，不一定等于 `price * count`
     * @property coinType 货币类型，`gold` 为付费金瓜子，`silver` 为免费银瓜子
     * @property batteryValue 付费礼物折算的电池价值，100 金瓜子为 1 电池
     * @property yuanValue 付费礼物折算的人民币价值，10 电池为 1 元
     */
    data class Gift(
        val userId: Long,
        val userName: String,
        val giftName: String,
        val count: Long,
        val price: Long,
        val totalCoin: Long,
        val coinType: String,
        override val fansMedal: BiliFansMedal?,
    ) : BiliLiveMessageData {
        val batteryValue: BigDecimal?
            get() = totalCoin.takeIf { coinType == "gold" }
                ?.let(BigDecimal::valueOf)
                ?.movePointLeft(2)
                ?.stripTrailingZeros()

        val yuanValue: BigDecimal?
            get() = batteryValue
                ?.movePointLeft(1)
                ?.stripTrailingZeros()
    }

    /**
     * 用户交互消息
     *
     * @property msgType 1 进入直播间，2 关注主播，3 分享直播间
     */
    data class Enter(
        val userId: Long,
        val userName: String,
        val msgType: Int,
        override val fansMedal: BiliFansMedal?,
    ) : BiliLiveMessageData

    /**
     * 购买大航海 1 总督 2 提督 3 舰长
     */
    data class Guard(
        val userId: Long,
        val userName: String,
        val guardLevel: Int,
        val count: Int,
        val price: Long,
        override val fansMedal: BiliFansMedal? = null,
    ) : BiliLiveMessageData

    /**
     * 醒目留言
     */
    data class SuperChat(
        val userId: Long,
        val userName: String,
        val content: String,
        val price: Int,
        override val fansMedal: BiliFansMedal?,
    ) : BiliLiveMessageData

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
