package com.blr19c.falowp.bot.adapter.cq.expand

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * api结果
 */
data class GoCqApiResult<T>(
    /**
     * 状态
     */
    @field:JsonProperty("status")
    val status: String,
    /**
     * 状态码
     */
    @field:JsonProperty("retcode")
    val retCode: Int,
    /**
     * 错误消息
     */
    @field:JsonProperty("msg")
    val msg: String?,
    /**
     * 对错误的详细解释
     */
    @field:JsonProperty("wording")
    val wording: String?,
    /**
     * 数据
     */
    @field:JsonProperty("data")
    val data: T?,
    /**
     * 回执
     */
    @field:JsonProperty("echo")
    val echo: Any?,
) {

    /**
     * 是否成功
     */
    fun success(): Boolean {
        return status == "ok" || status == "async"
    }

}

/**
 * 精华消息
 */
data class EssenceMsg(
    /**
     * 发送者QQ号
     */
    @field:JsonProperty("sender_id")
    val senderId: String,
    /**
     * 发送者昵称
     */
    @field:JsonProperty("sender_nick")
    val senderNick: String,
    /**
     * 消息发送时间
     */
    @field:JsonProperty("sender_time")
    val senderTime: Long,
    /**
     * 操作者QQ号
     */
    @field:JsonProperty("operator_id")
    val operatorId: String,
    /**
     * 操作者昵称
     */
    @field:JsonProperty("operator_nick")
    val operatorNick: String,
    /**
     * 精华设置时间
     */
    @field:JsonProperty("operator_time")
    val operatorTime: Long,
    /**
     * 消息id
     */
    @field:JsonProperty("message_id")
    val messageId: String
)

/**
 * 全体成员@剩余次数
 */
data class GroupAtAllRemain(
    /**
     * 是否能@全体成员
     */
    @field:JsonProperty("can_at_all")
    val canAtAll: Boolean,
    /**
     * 群内所有管理当天剩余 @全体成员 次数
     */
    @field:JsonProperty("remain_at_all_count_for_group")
    val remainAtAllCountForGroup: Int,
    /**
     * Bot 当天剩余 @全体成员 次数
     */
    @field:JsonProperty("remain_at_all_count_for_uin")
    val remainAtAllCountForUin: Int,
)

/**
 * 群公告
 */
data class GroupNotice(
    /**
     * 公告发表者
     */
    @field:JsonProperty("sender_id")
    val senderId: String,
    /**
     * 公告发表时间
     */
    @field:JsonProperty("publish_time")
    val publishTime: Long,
    /**
     * 公告内容
     */
    @field:JsonProperty("message")
    val message: GroupNoticeMessage

)

/**
 * 公告内容
 */
data class GroupNoticeMessage(
    /**
     * 公告内容
     */
    val text: String,
    /**
     * 公告图片
     */
    val images: List<GroupNoticeMessageImage>,
)

/**
 * 公告内容图片
 */
data class GroupNoticeMessageImage(
    /**
     * 图片id
     */
    val id: String,
)


/**
 * 群组文件系统信息
 */
data class GroupFileSystemInfo(
    /**
     * 文件总数
     */
    @field:JsonProperty("file_count")
    val fileCount: Int,
    /**
     * 文件上限
     */
    @field:JsonProperty("limit_count")
    val limitCount: Int,
    /**
     * 已使用空间
     */
    @field:JsonProperty("used_space")
    val usedSpace: Long,
    /**
     * 空间上限
     */
    @field:JsonProperty("total_space")
    val totalSpace: Long
)

/**
 * 群组文件信息
 */
data class GroupFileInfo(
    /**
     * 文件列表
     */
    val files: List<GroupFile>,
    /**
     * 文件夹列表
     */
    val folders: List<GroupFolder>
)

/**
 * 群组文件
 */
data class GroupFile(
    /**
     * 群组id
     */
    @field:JsonProperty("group_id")
    val groupId: String,
    /**
     * 文件id
     */
    @field:JsonProperty("file_id")
    val fileId: String,
    /**
     * 文件名
     */
    @field:JsonProperty("file_name")
    val fileName: String,
    /**
     * 文件类型
     */
    @field:JsonProperty("busid")
    val busId: Int,
    /**
     * 文件大小
     */
    @field:JsonProperty("file_size")
    val fileSize: Long,
    /**
     * 上传时间
     */
    @field:JsonProperty("upload_time")
    val uploadTime: Long,
    /**
     * 过期时间,永久文件为0
     */
    @field:JsonProperty("dead_time")
    val deadTime: Long,
    /**
     * 最后修改时间
     */
    @field:JsonProperty("modify_time")
    val modifyTime: Long,
    /**
     * 下载次数
     */
    @field:JsonProperty("download_times")
    val downloadTimes: Int,
    /**
     * 上传者ID
     */
    @field:JsonProperty("uploader")
    val uploader: String,
    /**
     * 上传者名字
     */
    @field:JsonProperty("uploader_name")
    val uploaderName: String
)

/**
 * 群组文件夹
 */
data class GroupFolder(
    /**
     * 群组id
     */
    @field:JsonProperty("group_id")
    val groupId: String,
    /**
     * 文件夹ID
     */
    @field:JsonProperty("folder_id")
    val folderId: String,
    /**
     * 文件名
     */
    @field:JsonProperty("folder_name")
    val folderName: String,
    /**
     * 创建时间
     */
    @field:JsonProperty("create_time")
    val createTime: Long,
    /**
     * 创建者
     */
    @field:JsonProperty("creator")
    val creator: String,
    /**
     * 创建者名字
     */
    @field:JsonProperty("creator_name")
    val creatorName: String,
    /**
     * 子文件数量
     */
    @field:JsonProperty("total_file_count")
    val totalFileCount: Int
)

/**
 * ocr信息
 */
data class OcrImage(
    /**
     * ocr文本
     */
    val texts: List<OcrImageText>,
    /**
     * 语言
     */
    val language: String
)

/**
 * ocr信息文本
 */
data class OcrImageText(
    /**
     * 文本
     */
    val text: String,
    /**
     * 置信度
     */
    val confidence: Int
)

/**
 * 登录号信息
 */
data class LoginInfo(
    /**
     * 用户id
     */
    @field:JsonProperty("user_id")
    val userId: String,
    /**
     * 昵称
     */
    @field:JsonProperty("nickname")
    val nickname: String,
)

/**
 * 陌生人信息
 */
data class StrangerInfo(
    /**
     * 用户id
     */
    @field:JsonProperty("user_id")
    val userId: String,
    /**
     * 昵称
     */
    @field:JsonProperty("nickname")
    val nickname: String,
    /**
     * 性别,male或female或unknown
     */
    @field:JsonProperty("sex")
    val sex: String,
    /**
     * 年龄
     */
    @field:JsonProperty("age")
    val age: Int,
    /**
     * qid ID身份卡
     */
    @field:JsonProperty("qid")
    val qid: String,
    /**
     * 等级
     */
    @field:JsonProperty("level")
    val level: Int,
    /**
     * 登录天数
     */
    @field:JsonProperty("login_days")
    val loginDays: Int
)

/**
 * 好友信息
 */
data class FriendInfo(
    /**
     * 用户id
     */
    @field:JsonProperty("user_id")
    val userId: String,
    /**
     * 昵称
     */
    @field:JsonProperty("nickname")
    val nickname: String,
    /**
     * 备注名
     */
    @field:JsonProperty("remark")
    val remark: String
)

/**
 * 群信息
 */
data class GroupInfo(
    /**
     * 群组id
     */
    @field:JsonProperty("group_id")
    val groupId: String,
    /**
     * 群名称
     */
    @field:JsonProperty("group_name")
    val groupName: String,
    /**
     * 群备注
     */
    @field:JsonProperty("group_memo")
    val groupMemo: String?,
    /**
     * 群创建时间
     */
    @field:JsonProperty("group_create_time")
    val groupCreateTime: Long,
    /**
     * 群等级
     */
    @field:JsonProperty("group_level")
    val groupLevel: Int,
    /**
     * 成员数
     */
    @field:JsonProperty("member_count")
    val memberCount: Int,
    /**
     * 最大成员数(群容量)
     */
    @field:JsonProperty("max_member_count")
    val maxMemberCount: Int
)

/**
 * 群成员信息
 */
data class GroupMemberInfo(
    /**
     * 用户id
     */
    @field:JsonProperty("user_id")
    val userId: String,
    /**
     * 昵称
     */
    @field:JsonProperty("nickname")
    val nickname: String,
    /**
     * 群名片/备注
     */
    @field:JsonProperty("card")
    val card: String,
    /**
     * 性别,male或female或unknown
     */
    @field:JsonProperty("sex")
    val sex: String,
    /**
     * 年龄
     */
    @field:JsonProperty("age")
    val age: Int,
    /**
     * 地区
     */
    @field:JsonProperty("area")
    val area: String,
    /**
     * 进群时间
     */
    @field:JsonProperty("join_time")
    val joinTime: Long,
    /**
     * 最后发言时间戳
     */
    @field:JsonProperty("last_sent_time")
    val lastSentTime: Long,
    /**
     * 成员等级
     */
    @field:JsonProperty("level")
    val level: String,
    /**
     * 角色,owner或admin或member
     */
    @field:JsonProperty("role")
    val role: String,
    /**
     * 是否不良记录成员
     */
    @field:JsonProperty("unfriendly")
    val unfriendly: Boolean,
    /**
     * 专属头衔
     */
    @field:JsonProperty("title")
    val title: String,
    /**
     * 专属头衔过期时间戳
     */
    @field:JsonProperty("title_expire_time")
    val titleExpireTime: Long,
    /**
     * 是否允许修改群名片
     */
    @field:JsonProperty("card_changeable")
    val cardChangeable: Boolean,
    /**
     * 禁言到期时间
     */
    @field:JsonProperty("shut_up_timestamp")
    val shutUpTimestamp: Long
)

/**
 * 群成员信息-粗略
 */
data class GroupMemberRoughInfo(
    /**
     * 用户id
     */
    @field:JsonProperty("user_id")
    val userId: String,
    /**
     * 昵称
     */
    @field:JsonProperty("nickname")
    val nickname: String,
    /**
     * 群名片/备注
     */
    @field:JsonProperty("card")
    val card: String,
    /**
     * 成员等级
     */
    @field:JsonProperty("level")
    val level: String,
)

/**
 * 群荣誉信息
 */
data class GroupHonorInfo(
    /**
     * 当前龙王
     */
    @field:JsonProperty("current_talkative")
    val currentTalkative: CurrentTalkative?,
    /**
     * 历史龙王
     */
    @field:JsonProperty("talkative_list")
    val talkativeList: List<HistoryHonorInfo>?,
    /**
     * 群聊之火
     */
    @field:JsonProperty("performer_list")
    val performerList: List<HistoryHonorInfo>?,
    /**
     * 群聊炽焰
     */
    @field:JsonProperty("legend_list")
    val legendList: List<HistoryHonorInfo>?,
    /**
     * 冒尖小春笋
     */
    @field:JsonProperty("strong_newbie_list")
    val strongNewbieList: List<HistoryHonorInfo>?,
    /**
     * 快乐之源
     */
    @field:JsonProperty("emotion_list")
    val emotionList: List<HistoryHonorInfo>?
)

/**
 * 群荣誉历史数据
 */
data class HistoryHonorInfo(
    /**
     * 用户id
     */
    @field:JsonProperty("user_id")
    val userId: String,
    /**
     * 昵称
     */
    @field:JsonProperty("nickname")
    val nickname: String,
    /**
     * 头像 URL
     */
    @field:JsonProperty("avatar")
    val avatar: String,
    /**
     * 荣誉描述
     */
    @field:JsonProperty("description")
    val description: String
)

/**
 * 龙王数据
 */
data class CurrentTalkative(
    /**
     * 用户id
     */
    @field:JsonProperty("user_id")
    val userId: String,
    /**
     * 昵称
     */
    @field:JsonProperty("nickname")
    val nickname: String,
    /**
     * 头像 URL
     */
    @field:JsonProperty("avatar")
    val avatar: String,
    /**
     * 持续天数
     */
    @field:JsonProperty("day_count")
    val dayCount: Int
)

/**
 * 图片
 */
data class Image(
    /**
     * 图片源文件大小
     */
    val size: Long,
    /**
     * 图片文件原名
     */
    val filename: String,
    /**
     * 图片下载地址
     */
    val url: String
)


/**
 * 运行状态
 */
data class Status(
    /**
     * 表示BOT是否在线
     */
    val online: Boolean,
    /**
     * 在线设备信息
     */
    val good: OnlineClients,
    /**
     * 运行统计
     */
    val stat: Statistics
)

/**
 * 运行统计
 */
data class Statistics(
    /**
     * 收到的数据包总数
     */
    @field:JsonProperty("packet_received")
    val packetReceived: ULong,
    /**
     * 发送的数据包总数
     */
    @field:JsonProperty("packet_sent")
    val packetSent: ULong,
    /**
     * 数据包丢失总数
     */
    @field:JsonProperty("packet_lost")
    val packetLost: ULong,
    /**
     * 接受信息总数
     */
    @field:JsonProperty("message_received")
    val messageReceived: ULong,
    /**
     * 发送信息总数
     */
    @field:JsonProperty("message_sent")
    val messageSent: ULong,
    /**
     * TCP 链接断开次数
     */
    @field:JsonProperty("disconnect_times")
    val disconnectTimes: ULong,
    /**
     * 账号掉线次数
     */
    @field:JsonProperty("lost_times")
    val lostTimes: ULong,
    /**
     * 最后一条消息时间
     */
    @field:JsonProperty("最后一条消息时间")
    val lastMessageTime: Long
)

/**
 * 版本信息
 */
data class VersionInfo(
    /**
     * 应用标识
     */
    @field:JsonProperty("app_name")
    val appName: String,
    /**
     * 应用版本
     */
    @field:JsonProperty("app_version")
    val appVersion: String,
    /**
     * 应用完整名称
     */
    @field:JsonProperty("app_full_name")
    val appFullName: String,
    /**
     * 协议名称
     */
    @field:JsonProperty("protocol_name")
    val protocolName: Int,
    /**
     * OneBot标准版本
     */
    @field:JsonProperty("protocol_version")
    val protocolVersion: Int,
    /**
     * 运行版本
     */
    @field:JsonProperty("runtime_version")
    val runtimeVersion: String,
    /**
     * 运行环境
     */
    @field:JsonProperty("runtime_os")
    val runtimeOs: String,
    /**
     * 应用版本
     */
    @field:JsonProperty("version")
    val version: String
)

/**
 * 在线设备信息
 */
data class OnlineClients(
    /**
     * 设备列表
     */
    val clients: List<Device>
)

/**
 * 客户端设备信息
 */
data class Device(
    /**
     * 设备id
     */
    @field:JsonProperty("app_id")
    val appId: String,
    /**
     * 设备名称
     */
    @field:JsonProperty("device_name")
    val deviceName: String,
    /**
     * 设备类型
     */
    @field:JsonProperty("device_kind")
    val deviceKind: String
)