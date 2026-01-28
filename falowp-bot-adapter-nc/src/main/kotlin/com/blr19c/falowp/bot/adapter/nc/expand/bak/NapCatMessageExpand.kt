package com.blr19c.falowp.bot.adapter.nc.expand.bak

import com.blr19c.falowp.bot.adapter.nc.api.NapCatBotApi
import com.blr19c.falowp.bot.adapter.nc.message.NapCatMessage
import com.blr19c.falowp.bot.system.json.Json
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode

/**
 * 消息类扩展
 */
class NapCatMessageExpand

/**
 * 发送群聊消息
 *
 * @param groupId 群聊ID
 * @param message 消息内容
 */
suspend fun NapCatBotApi.sendGroupMsg(
    groupId: String = this.receiveMessage.source.id, message: List<NapCatMessage.Message>
): Long {
    val body = mapOf("group_id" to groupId, "message" to message)
    return apiRequest<Long>("send_group_msg", body)
}

/**
 * 发送私聊消息
 *
 * @param userId 用户ID
 * @param message 消息内容
 */
suspend fun NapCatBotApi.sendPrivateMsg(
    userId: String = this.receiveMessage.sender.id, message: List<NapCatMessage.Message>
): Long {
    val body = mapOf("user_id" to userId, "message" to message)
    return apiRequest<Long>("send_private_msg", body)
}

/**
 * 发送戳一戳
 *
 * @param userId 用户id
 * @param groupId 群组id
 */
suspend fun NapCatBotApi.sendPoke(
    userId: String = this.receiveMessage.sender.id,
    groupId: String? = if (this.receiveMessage.group()) this.receiveMessage.source.id else null
) {
    apiRequestUnit(
        "send_poke", mapOf("group_id" to groupId, "user_id" to userId)
    )
}

/**
 * 撤回消息
 *
 * @param messageId 消息id
 */
suspend fun NapCatBotApi.deleteMsg(messageId: String) {
    apiRequestUnit("delete_msg", mapOf("message_id" to messageId))
}

/**
 * 获取群历史消息
 *
 * @param groupId 群组id
 * @param messageSeq 起始消息序号（可选，不传一般表示从最新开始）
 * @param count 获取数量（默认20）
 * @return 群历史消息列表
 */
suspend fun NapCatBotApi.getGroupMsgHistory(
    groupId: String = this.receiveMessage.source.id, messageSeq: Long? = null, count: Int = 20
): ArrayNode {
    return apiRequest<ArrayNode>(
        "get_group_msg_history",
        mapOf("group_id" to groupId, "message_seq" to messageSeq, "count" to count)
    ) ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取消息详情
 *
 * @param messageId 消息id
 * @return 消息详情
 */
suspend fun NapCatBotApi.getMsg(messageId: String): NapCatMessage {
    return apiRequest("get_msg", mapOf("message_id" to messageId)) ?: throw IllegalArgumentException("无效的消息ID")
}

/**
 * 获取合并转发消息
 *
 * @param id 合并转发id
 * @return 合并转发消息内容
 */
suspend fun NapCatBotApi.getForwardMsg(id: String): JsonNode? {
    return apiRequest("get_forward_msg", mapOf("id" to id))
}

/**
 * 贴表情
 *
 * @param messageId 消息id
 * @param emojiId 表情id
 * @param set 是否贴上（true贴上 / false取消）
 */
suspend fun NapCatBotApi.setMsgEmojiLike(messageId: Long, emojiId: String, set: Boolean = true) {
    apiRequestUnit(
        "set_msg_emoji_like",
        mapOf("message_id" to messageId, "emoji_id" to emojiId, "set" to set)
    )
}

/**
 * 获取好友历史消息
 *
 * @param userId 用户id
 * @param messageSeq 起始消息序号（可选，不传一般表示从最新开始）
 * @param count 获取数量（默认20）
 * @return 好友历史消息列表
 */
suspend fun NapCatBotApi.getFriendMsgHistory(
    userId: String = this.receiveMessage.sender.id,
    messageSeq: Long? = null,
    count: Int = 20
): ArrayNode {
    return apiRequest<ArrayNode>(
        "get_friend_msg_history",
        mapOf("user_id" to userId, "message_seq" to messageSeq, "count" to count)
    ) ?: Json.objectMapper().createArrayNode()
}

/**
 * 获取贴表情详情
 *
 * @param messageId 消息id
 * @return 贴表情详情列表
 */
suspend fun NapCatBotApi.getMsgEmojiLike(messageId: Long): ArrayNode {
    return apiRequest<ArrayNode>(
        "get_msg_emoji_like",
        mapOf("message_id" to messageId)
    ) ?: Json.objectMapper().createArrayNode()
}

/**
 * 发送合并转发消息
 *
 * @param groupId 群组id
 * @param nodes 合并转发节点列表
 * @return 发送结果（通常包含message_id等）
 */
suspend fun NapCatBotApi.sendGroupForwardMsg(
    groupId: String = this.receiveMessage.source.id,
    nodes: String
): JsonNode? {
    return apiRequest(
        "send_forward_msg",
        mapOf(
            "group_id" to groupId,
            "messages" to nodes
        )
    )
}

/**
 * 获取语音消息详情
 *
 * @param file 语音file标识
 * @param outFormat 输出格式（默认mp3）
 * @return 语音详情（通常包含url/file等）
 */
suspend fun NapCatBotApi.getRecord(file: String, outFormat: String = "mp3"): JsonNode? {
    return apiRequest("get_record", mapOf("file" to file, "out_format" to outFormat))
}

/**
 * 获取图片消息详情
 *
 * @param file 图片file标识
 * @return 图片详情（通常包含url/size等）
 */
suspend fun NapCatBotApi.getImage(file: String): JsonNode? {
    return apiRequest("get_image", mapOf("file" to file))
}


/**
 * 发送群AI语音
 *
 * @param groupId 群组id
 * @param characterId 角色id
 * @param text 文本内容
 */
suspend fun NapCatBotApi.sendGroupAiVoice(
    groupId: String = this.receiveMessage.source.id,
    characterId: String,
    text: String
) {
    apiRequestUnit(
        "send_group_ai_voice",
        mapOf(
            "group_id" to groupId,
            "character_id" to characterId,
            "text" to text
        )
    )
}
