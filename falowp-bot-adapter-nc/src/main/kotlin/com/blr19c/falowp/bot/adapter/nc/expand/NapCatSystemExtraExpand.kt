@file:Suppress("UNUSED", "UnusedReceiverParameter")

package com.blr19c.falowp.bot.adapter.nc.expand

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GetCollectionListDataCollectionSearchListCollectionItemListItemAuthor
 */
data class GetCollectionListDataCollectionSearchListCollectionItemListItemAuthor(
    @field:JsonProperty("type")
    val type: Long,
    @field:JsonProperty("numId")
    val numId: String,
    @field:JsonProperty("strId")
    val strId: String,
    @field:JsonProperty("groupId")
    val groupId: String,
    @field:JsonProperty("groupName")
    val groupName: String,
    @field:JsonProperty("uid")
    val uid: String
)

/**
 * GetCollectionListDataCollectionSearchListCollectionItemListItemSummary
 */
data class GetCollectionListDataCollectionSearchListCollectionItemListItemSummary(
    @field:JsonProperty("textSummary")
    val textSummary: String,
    @field:JsonProperty("linkSummary")
    val linkSummary: String,
    @field:JsonProperty("gallerySummary")
    val gallerySummary: String,
    @field:JsonProperty("audioSummary")
    val audioSummary: String,
    @field:JsonProperty("videoSummary")
    val videoSummary: String,
    @field:JsonProperty("fileSummary")
    val fileSummary: String,
    @field:JsonProperty("locationSummary")
    val locationSummary: String,
    @field:JsonProperty("richMediaSummary")
    val richMediaSummary: String
)

/**
 * GetCollectionListDataCollectionSearchListCollectionItemListItem
 */
data class GetCollectionListDataCollectionSearchListCollectionItemListItem(
    @field:JsonProperty("cid")
    val cid: String,
    @field:JsonProperty("type")
    val type: Long,
    @field:JsonProperty("status")
    val status: Long,
    @field:JsonProperty("author")
    val author: GetCollectionListDataCollectionSearchListCollectionItemListItemAuthor,
    @field:JsonProperty("bid")
    val bid: Long,
    @field:JsonProperty("category")
    val category: Long,
    @field:JsonProperty("createTime")
    val createTime: String,
    @field:JsonProperty("collectTime")
    val collectTime: String,
    @field:JsonProperty("modifyTime")
    val modifyTime: String,
    @field:JsonProperty("sequence")
    val sequence: String,
    @field:JsonProperty("shareUrl")
    val shareUrl: String,
    @field:JsonProperty("customGroupId")
    val customGroupId: Long,
    @field:JsonProperty("securityBeat")
    val securityBeat: Boolean,
    @field:JsonProperty("summary")
    val summary: GetCollectionListDataCollectionSearchListCollectionItemListItemSummary
)

/**
 * GetCollectionListDataCollectionSearchList
 */
data class GetCollectionListDataCollectionSearchList(
    @field:JsonProperty("collectionItemList")
    val collectionItemList: List<GetCollectionListDataCollectionSearchListCollectionItemListItem>,
    @field:JsonProperty("hasMore")
    val hasMore: Boolean,
    @field:JsonProperty("bottomTimeStamp")
    val bottomTimeStamp: String
)

/**
 * GetCollectionListData
 */
data class GetCollectionListData(
    @field:JsonProperty("result")
    val result: Long,
    @field:JsonProperty("errMsg")
    val errMsg: String,
    @field:JsonProperty("collectionSearchList")
    val collectionSearchList: GetCollectionListDataCollectionSearchList
)

/**
 * GetMiniAppArkDataMetaDataDetail1Host
 */
data class GetMiniAppArkDataMetaDataDetail1Host(
    @field:JsonProperty("uin")
    val uin: Long,
    @field:JsonProperty("nick")
    val nick: String
)

/**
 * GetMiniAppArkDataMetaDataDetail1
 */
data class GetMiniAppArkDataMetaDataDetail1(
    @field:JsonProperty("appid")
    val appid: String,
    @field:JsonProperty("appType")
    val appType: Long,
    @field:JsonProperty("title")
    val title: String,
    @field:JsonProperty("desc")
    val desc: String,
    @field:JsonProperty("icon")
    val icon: String,
    @field:JsonProperty("preview")
    val preview: String,
    @field:JsonProperty("url")
    val url: String,
    @field:JsonProperty("scene")
    val scene: Long,
    @field:JsonProperty("host")
    val host: GetMiniAppArkDataMetaDataDetail1Host,
    @field:JsonProperty("shareTemplateId")
    val shareTemplateId: String,
    @field:JsonProperty("shareTemplateData")
    val shareTemplateData: NapCatRawData,
    @field:JsonProperty("showLittleTail")
    val showLittleTail: String,
    @field:JsonProperty("gamePoints")
    val gamePoints: String,
    @field:JsonProperty("gamePointsUrl")
    val gamePointsUrl: String,
    @field:JsonProperty("shareOrigin")
    val shareOrigin: Long
)

/**
 * GetMiniAppArkDataMetaData
 */
data class GetMiniAppArkDataMetaData(
    @field:JsonProperty("detail_1")
    val detail1: GetMiniAppArkDataMetaDataDetail1
)

/**
 * GetMiniAppArkDataConfig
 */
data class GetMiniAppArkDataConfig(
    @field:JsonProperty("type")
    val type: String,
    @field:JsonProperty("width")
    val width: Long,
    @field:JsonProperty("height")
    val height: Long,
    @field:JsonProperty("forward")
    val forward: Long,
    @field:JsonProperty("autoSize")
    val autoSize: Long,
    @field:JsonProperty("ctime")
    val ctime: Long,
    @field:JsonProperty("token")
    val token: String
)

/**
 * GetMiniAppArkData
 */
data class GetMiniAppArkData(
    @field:JsonProperty("appName")
    val appName: String,
    @field:JsonProperty("appView")
    val appView: String,
    @field:JsonProperty("ver")
    val ver: String,
    @field:JsonProperty("desc")
    val desc: String,
    @field:JsonProperty("prompt")
    val prompt: String,
    @field:JsonProperty("metaData")
    val metaData: GetMiniAppArkDataMetaData,
    @field:JsonProperty("config")
    val config: GetMiniAppArkDataConfig
)

/**
 * GetRkeyDataItem
 */
data class GetRkeyDataItem(
    @field:JsonProperty("type")
    val type: String,
    @field:JsonProperty("rkey")
    val rkey: String,
    @field:JsonProperty("created_at")
    val createdAt: Long,
    @field:JsonProperty("ttl")
    val ttl: String
)

/**
 * GetRkeyServerData
 */
data class GetRkeyServerData(
    @field:JsonProperty("private_rkey")
    val privateRkey: String,
    @field:JsonProperty("group_rkey")
    val groupRkey: String,
    @field:JsonProperty("expired_time")
    val expiredTime: Long,
    @field:JsonProperty("name")
    val name: String
)

/**
 * GetRobotUinRangeDataItem
 */
data class GetRobotUinRangeDataItem(
    @field:JsonProperty("minUin")
    val minUin: String,
    @field:JsonProperty("maxUin")
    val maxUin: String
)

/**
 * NcGetRkeyDataItem
 */
data class NcGetRkeyDataItem(
    @field:JsonProperty("rkey")
    val rkey: String,
    @field:JsonProperty("ttl")
    val ttl: String,
    @field:JsonProperty("time")
    val time: Long,
    @field:JsonProperty("type")
    val type: Long
)

/**
 * NcGetUserStatusData
 */
data class NcGetUserStatusData(
    @field:JsonProperty("status")
    val status: Long,
    @field:JsonProperty("ext_status")
    val extStatus: Long
)

/**
 * NapCatSystemExtraExpand
 */
class NapCatSystemExtraExpand

/**
 * 账号退出
 */
suspend fun NapCatBotApi.botExit() {
    apiRequestUnit("bot_exit")
}

/**
 * 获取收藏表情
 */
suspend fun NapCatBotApi.fetchCustomFace(count: Long? = null): List<String> {
    return apiRequest("fetch_custom_face", mapOf("count" to count))
}

/**
 * 获取收藏列表
 */
suspend fun NapCatBotApi.getCollectionList(category: Long, count: Long): GetCollectionListData {
    return apiRequest("get_collection_list", mapOf("category" to category, "count" to count))
}

/**
 * 获取小程序卡片
 */
suspend fun NapCatBotApi.getMiniAppArk(): GetMiniAppArkData {
    return apiRequest("get_mini_app_ark")
}

/**
 * 获取rkey
 */
suspend fun NapCatBotApi.getRkey(): List<GetRkeyDataItem> {
    return apiRequest("get_rkey")
}

/**
 * 获取rkey服务
 */
suspend fun NapCatBotApi.getRkeyServer(): GetRkeyServerData {
    return apiRequest("get_rkey_server")
}

/**
 * 获取机器人账号范围
 */
suspend fun NapCatBotApi.getRobotUinRange(): List<GetRobotUinRangeDataItem> {
    return apiRequest("get_robot_uin_range")
}

/**
 * nc获取rkey
 */
suspend fun NapCatBotApi.ncGetRkey(): List<NcGetRkeyDataItem> {
    return apiRequest("nc_get_rkey")
}

/**
 * 获取用户状态
 */
suspend fun NapCatBotApi.ncGetUserStatus(userId: Long): NcGetUserStatusData {
    return apiRequest("nc_get_user_status", mapOf("user_id" to userId))
}

/**
 * 发送自定义组包
 */
suspend fun NapCatBotApi.sendPacket() {
    apiRequestUnit("send_packet")
}
