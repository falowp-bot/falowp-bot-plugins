package com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.model

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * 群聊扩展数据模型
 */
class QQNativeGroupApiExpand {

    /**
     * 群成员信息
     */
    data class GroupMember(
        /**
         * 群ID
         */
        @field:JsonProperty("group_id")
        val groupId: String,

        /**
         * 用户ID
         */
        @field:JsonProperty("user_id")
        val userId: String,

        /**
         * 昵称
         */
        val nickname: String,

        /**
         * 群成员角色
         */
        val role: String,

        /**
         * 群头衔
         */
        val title: String? = null,

        /**
         * 群名片
         */
        val card: String? = null,

        /**
         * 性别
         */
        val sex: String? = null,

        /**
         * 年龄
         */
        val age: Int? = null,

        /**
         * 地区
         */
        val area: String? = null,

        /**
         * 群等级
         */
        val level: Int? = null,

        /**
         * QQ 等级
         */
        @field:JsonProperty("qq_level")
        val qqLevel: Int? = null,

        /**
         * 加群时间
         */
        @field:JsonProperty("join_time")
        val joinTime: Long? = null,

        /**
         * 最后发言时间
         */
        @field:JsonProperty("last_sent_time")
        val lastSentTime: Long? = null,

        /**
         * 头衔过期时间
         */
        @field:JsonProperty("title_expire_time")
        val titleExpireTime: Long? = null,

        /**
         * 是否机器人
         */
        @field:JsonProperty("is_robot")
        val isRobot: Boolean,

        /**
         * 禁言到期时间戳
         * 0 表示未禁言
         */
        @field:JsonProperty("shut_up_timestamp")
        val shutUpTimestamp: Long,
    )

    /**
     * 群公告内容
     */
    data class GroupNoticeItemItemMessage(
        /**
         * 文本内容
         */
        val text: String,
        /**
         * 图片列表
         */
        val image: List<JsonNode>,
        /**
         * 图片列表
         */
        val images: List<JsonNode>
    )

    /**
     * 群公告
     */
    data class GroupNoticeItemItem(
        /**
         * 发送者QQ
         */
        @field:JsonProperty("sender_id")
        val senderId: Long,
        /**
         * 发布时间
         */
        @field:JsonProperty("publish_time")
        val publishTime: Long,
        /**
         * 公告ID
         */
        @field:JsonProperty("notice_id")
        val noticeId: String,
        /**
         * 公告内容
         */
        val message: GroupNoticeItemItemMessage,
        /**
         * 公告设置
         */
        val settings: JsonNode?,
        /**
         * 阅读数
         */
        @field:JsonProperty("read_num")
        val readNum: Long?
    )

    /**
     * 精华消息
     */
    data class EssenceMsg(
        /**
         * 消息 ID
         */
        @field:JsonProperty("message_id")
        val messageId: String?,
        @field:JsonProperty("msg_seq") val msgSeq: String,
        @field:JsonProperty("msg_random") val msgRandom: String,

        /**
         * 发送人
         */
        @field:JsonProperty("sender_id")
        val senderId: String,

        /**
         * 操作人
         */
        @field:JsonProperty("operator_id")
        val operatorId: String,

        /**
         * 操作时间
         */
        @field:JsonProperty("operator_time")
        val operatorTime: Long,

        /**
         * 消息内容
         */
        val content: List<JsonNode>
    )

    /**
     * 群详细资料
     */
    data class GroupDetailInfo(
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: Long,
        /**
         * 群名称
         */
        @field:JsonProperty("group_name")
        val groupName: String,
        /**
         * 成员数量
         */
        @field:JsonProperty("member_count")
        val memberCount: Long,
        /**
         * 最大成员数量
         */
        @field:JsonProperty("max_member_count")
        val maxMemberCount: Long,
        /**
         * 全员禁言状态
         */
        @field:JsonProperty("group_all_shut")
        val groupAllShut: Long,
        /**
         * 群备注
         */
        @field:JsonProperty("group_remark")
        val groupRemark: String
    )

    /**
     * 已忽略的入群申请
     */
    data class GroupIgnoreAddRequestItemItem(
        /**
         * 请求ID
         */
        @field:JsonProperty("request_id")
        val requestId: Long,
        /**
         * 邀请者QQ
         */
        @field:JsonProperty("invitor_uin")
        val invitorUin: Long,
        /**
         * 邀请者昵称
         */
        @field:JsonProperty("invitor_nick")
        val invitorNick: String?,
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: Long,
        /**
         * 验证信息
         */
        val message: String?,
        /**
         * 群名称
         */
        @field:JsonProperty("group_name")
        val groupName: String?,
        /**
         * 是否已处理
         */
        val checked: Boolean,
        /**
         * 处理者QQ
         */
        val actor: Long,
        /**
         * 请求者昵称
         */
        @field:JsonProperty("requester_nick")
        val requesterNick: String?,
        /**
         * QQ 号无法解析时数字字段为 0，保留 UID 供调用方识别
         */
        @field:JsonProperty("invitor_uid") val invitorUid: String = "",
        @field:JsonProperty("actor_uid") val actorUid: String = ""
    )

    /**
     * 已忽略的群申请列表
     */
    data class GroupIgnoredNotifies(
        /**
         * 邀请请求列表
         */
        @field:JsonProperty("invited_requests")
        val invitedRequests: List<GroupIgnoreAddRequestItemItem>,
        /**
         * 加入请求列表
         */
        @field:JsonProperty("join_requests")
        val joinRequests: List<GroupIgnoreAddRequestItemItem>
    )

    /**
     * 群基本资料
     */
    data class GroupInfo(
        /**
         * 是否全员禁言
         */
        @field:JsonProperty("group_all_shut")
        val groupAllShut: Long,
        /**
         * 群备注
         */
        @field:JsonProperty("group_remark")
        val groupRemark: String,
        /**
         * 群号
         */
        @field:JsonProperty("group_id")
        val groupId: String,
        /**
         * 群名称
         */
        @field:JsonProperty("group_name")
        val groupName: String,
        /**
         * 成员人数
         */
        @field:JsonProperty("member_count")
        val memberCount: Long?,
        /**
         * 最大成员人数
         */
        @field:JsonProperty("max_member_count")
        val maxMemberCount: Long?
    )

    /**
     * 群禁言信息
     */
    data class GroupShut(
        /**
         * 用户 ID
         */
        @field:JsonProperty("user_id")
        val userId: Long,
        /**
         * 用户昵称
         */
        val nickname: String,
        /**
         * 禁言截止时间
         */
        @field:JsonProperty("shut_up_time")
        val shutUpTime: Long
    )

    /**
     * 相册信息
     */
    data class Album(
        /**
         * 相册 ID
         */
        @field:JsonProperty("album_id")
        val albumId: String,
        /**
         * 相册名称
         */
        @field:JsonProperty("album_name")
        val albumName: String,
        /**
         * 相册封面 URL
         */
        @field:JsonProperty("cover_url")
        val coverUrl: String?,
        /**
         * 创建时间（Unix 时间戳，秒）
         */
        @field:JsonProperty("create_time")
        val createTime: Long?,
        /**
         * 相册原始数据，包含完整封面信息
         */
        val raw: JsonNode? = null
    )

    /**
     * 相册媒体信息
     */
    data class AlbumMedia(
        /**
         * 媒体 ID
         */
        @field:JsonProperty("media_id")
        val mediaId: String?,
        /**
         * 媒体访问 URL
         */
        val url: String?,
        /**
         * 完整的原生媒体记录，用于保留不同 QQ 版本的图片和视频字段
         */
        val raw: JsonNode
    )

}
