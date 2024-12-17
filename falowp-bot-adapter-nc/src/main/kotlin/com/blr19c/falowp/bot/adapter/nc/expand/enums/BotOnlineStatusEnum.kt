package com.blr19c.falowp.bot.adapter.nc.expand.enums

/**
 * 在线状态
 */
@Suppress("UNUSED")
enum class BotOnlineStatusEnum(val status: Int, val extStatus: Int) {

    /**
     * 在线
     */
    ONLINE(10, 0),

    /**
     * Q我吧
     */
    Q_ME(60, 0),

    /**
     * 离开
     */
    AWAY(30, 0),

    /**
     * 忙碌
     */
    BUSY(50, 0),

    /**
     * 请勿打扰
     */
    DND(70, 0),

    /**
     * 隐身
     */
    STEALTH(40, 0),

    /**
     * 听歌中
     */
    MUSIC(10, 1028),

    /**
     * 春日限定
     */
    SPRING(10, 2037),

    /**
     * 一起元梦
     */
    PLAY_YM(10, 2025),

    /**
     * 求星搭子
     */
    PLAY_YM_BUDDY(10, 2026),

    /**
     * 被掏空
     */
    EXHAUSTED(10, 2014),

    /**
     * 今日天气
     */
    WEATHER(10, 1030),

    /**
     * 我crash了
     */
    CRASH(10, 2019),

    /**
     * 爱你
     */
    LOVE(10, 2006),

    /**
     * 恋爱中
     */
    IN_LOVE(10, 1051),

    /**
     * 好运锦鲤
     */
    LUCKY(10, 1071),

    /**
     * 水逆退散
     */
    BAD_LUCK_DISAPPEARS(10, 1201),

    /**
     * 嗨到飞起
     */
    ON_CLOUD_NINE(10, 1056),

    /**
     * 元气满满
     */
    FULL_ENERGY(10, 1058),

    /**
     * 宝宝认证
     */
    BABY_MODE(10, 1070),

    /**
     * 一言难尽
     */
    UNBEARABLE_PAIN(10, 1063),

    /**
     * 难得糊涂
     */
    NEGLIGENCE(10, 2001),

    /**
     * emo中
     */
    EMO(10, 1401),

    /**
     * 我太难了
     */
    STRUGGLING(10, 1062),

    /**
     * 我想开了
     */
    EPIPHANY(10, 2013),

    /**
     * 我没事
     */
    OVER_NOW(10, 1052),

    /**
     * 想静静
     */
    WANT_QUIET(10, 1061),

    /**
     * 悠哉哉
     */
    UNHURRIED(10, 1059),

    /**
     * 去旅行
     */
    TRAVELING(10, 2015),

    /**
     * 信号弱
     */
    LOW_SIGNAL(10, 1011),

    /**
     * 出去浪
     */
    HAVE_FUN(10, 2003),

    /**
     * 肝作业
     */
    GRIND_AWAY(10, 2012),

    /**
     * 学习中
     */
    STUDYING(10, 1018),

    /**
     * 搬砖中
     */
    WORKING(10, 2023),

    /**
     * 摸鱼中
     */
    IDLE(10, 1300),

    /**
     * 无聊中
     */
    BORED(10, 1060),

    /**
     * timi中
     */
    TIMI(10, 1027),

    /**
     * 睡觉中
     */
    SLEEPING(10, 1016),

    /**
     * 熬夜中
     */
    STAY_UP(10, 1032),

    /**
     * 追剧中
     */
    TV_THEATER(10, 1021),

    /**
     * 我的电量
     */
    BATTERY(10, 1000),
}
