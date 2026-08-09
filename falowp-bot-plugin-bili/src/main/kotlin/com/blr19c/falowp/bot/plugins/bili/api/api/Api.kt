@file:Suppress("UNUSED")

package com.blr19c.falowp.bot.plugins.bili.api.api

// 请求来源
const val ORIGIN = "https://space.bilibili.com/360691087"

// 扫码登录
const val QRCODE_GENERATE = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
const val QRCODE_POLL = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"

// 导航和 WBI 密钥
const val WBI_NAV = "https://api.bilibili.com/x/web-interface/nav"

// 用户空间
const val SPACE_INFO = "https://api.bilibili.com/x/space/wbi/acc/info"

// 视频
const val VIDEO_INFO = "https://api.bilibili.com/x/web-interface/view"
const val VIDEO_AI_SUMMARY = "https://api.bilibili.com/x/web-interface/view/conclusion/get"

// 动态
const val DYNAMIC_HISTORY = "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/space_history"
const val DYNAMIC_INFO = "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/get_dynamic_detail"
const val SPACE_DYNAMIC_INFO = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space"

// 直播
const val BATCH_ROOM_INFO = "https://api.live.bilibili.com/room/v1/Room/get_status_info_by_uids"
const val LIVE_DM_INFO = "https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo"
const val LIVE_MESSAGE_SEND = "https://api.live.bilibili.com/msg/send"
const val LIVE_ORIGIN = "https://live.bilibili.com"
