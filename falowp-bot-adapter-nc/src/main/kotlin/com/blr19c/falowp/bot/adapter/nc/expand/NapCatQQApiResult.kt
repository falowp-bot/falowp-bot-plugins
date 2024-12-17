package com.blr19c.falowp.bot.adapter.nc.expand

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode

/**
 * API结果
 */
data class NapCatQQApiResult<T>(
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
 * 推荐卡片
 */
data class ArkSharePeer(
    /**
     * 卡片信息
     */
    val arkJson: String?,
    /**
     * 错误代码
     */
    val errCode: String?,
    /**
     * 错误信息
     */
    val errMsg: String?
)

/**
 * 机器人账号区间
 */
data class RobotUinRange(
    /**
     * 最小
     */
    val minUin: Long,
    /**
     * 最大
     */
    val maxUin: Long,
)

/**
 * 好友分类
 */
data class FriendsWithCategory(
    /**
     * 分类id
     */
    val categoryId: String,
    /**
     * 分类排序id
     */
    val categorySortId: Int,
    /**
     * 分类名称
     */
    val categoryName: String,
    /**
     * 分类内好友总数量
     */
    val categoryMbCount: Int,
    /**
     * 在线数量
     */
    val onlineCount: Int,
    /**
     * 好友列表
     */
    val buddyList: List<FriendsWithCategoryBuddy>
)

/**
 * 分类好友列表
 */
data class FriendsWithCategoryBuddy(
    /**
     * 分类id
     */
    val categoryId: String,
    /**
     * QQ ID
     */
    val qid: String?,
    /**
     * 签名
     */
    val longNick: String?,
    /**
     * 生日
     */
    @field:JsonProperty("birthday_year")
    val birthdayYear: String?,
    /**
     * 生日
     */
    @field:JsonProperty("birthday_month")
    val birthdayMonth: String?,
    /**
     * 生日
     */
    @field:JsonProperty("birthday_day")
    val birthdayDay: String?,
    /**
     * 年龄
     */
    val age: Int?,
    /**
     * 性别
     */
    val sex: String?,
    /**
     * 邮箱
     */
    val eMail: String?,
    /**
     * 手机号
     */
    val phoneNum: String?,
    /**
     * richTime
     */
    val richTime: String?,
    /**
     * richBuffer
     */
    val richBuffer: JsonNode?,
    /**
     * QQ号
     */
    val uid: String?,
    /**
     * QQ号
     */
    val uin: String?,
    /**
     * 昵称
     */
    val nick: String?,
    /**
     * 备注
     */
    val remark: String?,
    /**
     * QQ号
     */
    @field:JsonProperty("user_id")
    val userId: String?,
    /**
     * 昵称
     */
    val nickname: String?,
    /**
     * 等级
     */
    val level: Int?
)

/**
 * 文件信息
 */
data class FileInfo(
    /**
     * 文件链接或路径
     */
    val file: String?,
    /**
     * 文件链接或路径
     */
    val url: String?,
    /**
     * 文件大小
     */
    @field:JsonProperty("file_size")
    val fileSize: Long?,
    /**
     * 文件名
     */
    @field:JsonProperty("file_name")
    val fileName: String?,
    /**
     * base64
     */
    val base64: String?
)

/**
 * 点赞信息
 */
data class ProfileLike(
    /**
     * 总点赞数
     */
    @field:JsonProperty("total_count")
    val totalCount: Int,
    /**
     * 新点赞数
     */
    @field:JsonProperty("new_count")
    val newCount: Int,
    /**
     * 新增附近的人点赞数
     */
    @field:JsonProperty("new_nearby_count")
    val newNearbyCount: Int,
    /**
     * 用户信息
     */
    @field:JsonProperty("userInfos")
    val userInfos: ArrayNode
)

/**
 * nc rKey
 */
data class NCRKey(
    @field:JsonProperty("rkey")
    val rKey: String,
    val ttl: String,
    val time: Long,
    val type: String,
)

/**
 * 群聊被禁言用户信息
 */
data class GroupShutInfo(
    /**
     * 用户id
     */
    val uid: String,
    /**
     * 解禁时间
     */
    val shutUpTime: Long,
)

/**
 * ai语音信息
 */
data class AiCharacters(
    /**
     * 类型
     */
    val type: String,
    /**
     * 人物列表
     */
    val characters: List<AiCharactersInfo>
)

/**
 * ai语音人物信息
 */
data class AiCharactersInfo(
    /**
     * 人物id
     */
    @field:JsonProperty("character_id")
    val characterId: String,
    /**
     * 人物名称
     */
    @field:JsonProperty("character_name")
    val characterName: String,
    /**
     * 预览
     */
    @field:JsonProperty("preview_url")
    val previewUrl: String?
)