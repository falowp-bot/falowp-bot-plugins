# QQNative 扩展对照

扩展分为私聊、群聊和基础三类。基础扩展包含公共消息操作和分享；群文件属于群聊扩展，专属通知在 `adapter.notice.event` 发布。闪传、在线文件、流式传输、频道、语音查询转码及运维接口不在本轮范围。

所有实现位于 Kotlin；Node 仅增加原生服务/方法白名单。调用失败抛异常，不伪造成功结果。下表“已实现”表示有完整调用路径，不等于已对真实账号执行变更操作。

## 当前已知实测限制

群分享曾返回 `errCode=1004 / invalid UTF-8`，现在对此明确错误刷新群资料后重试一次；多次复测原生 Ark 构造及私聊发送通过，持续失败仍抛错。用户名片和 B站小程序卡片已真实发送。群戳一戳在原测试群仍返回 QQ 错误 `1003`，原生方法和两种 OIDB 请求头结果相同；新建临时群内对两名测试成员及私聊戳一戳通过，不能据此判断原测试群失败的原因。

## 私聊扩展

`QQNativePrivateApiExpand.kt`：14 个入口。资料点赞、在线状态、头像和申请处理包含在本轮，不列为以后再做。

| 扩展方法 | QQ Native 状态 | 验证 |
| --- | --- | --- |
| `getCookies` | 已实现 | 只读实测通过 |
| `getFriendList` | 已实现 | 只读实测通过 |
| `getRecentContact` | 已实现 | 只读实测通过 |
| `sendLike` | 已实现 | 真实调用通过（一次点赞） |
| `setFriendAddRequest` | 已实现 | 编译通过；真实操作未逐项执行 |
| `setFriendRemark` | 已实现 | 修改、回读、恢复原值通过 |
| `getFriendsWithCategory` | 已实现 | 只读实测通过 |
| `getProfileLike` | 已实现 | 只读实测通过 |
| `getUnidirectionalFriendList` | 已实现 | 单页只读实测通过；游标续拉、去重及异常响应有回归覆盖，真实多页未验收 |
| `getUnidirectionalFriendPage` | 已实现 | 返回本页、续拉游标、完整性标记及原始响应 |
| `setOnlineStatus` | 已实现 | 编译通过；真实操作未逐项执行 |
| `setDiyOnlineStatus` | 已实现 | 编译通过；真实操作未逐项执行 |
| `setQQAvatar` | 已实现 | 编译通过；真实操作未逐项执行 |
| `setSelfLongNick` | 已实现 | 当前账号返回 999；原生及参考项目的资料协议路径均失败，原签名未改变 |

## 群聊扩展

| 扩展方法 | QQ Native 状态 | 验证 |
| --- | --- | --- |
| `delGroupNotice` | 已实现 | 编译通过；真实操作未逐项执行 |
| `getGroupNotice` | 已实现 | 只读实测通过（空列表） |
| `deleteEssenceMsg` | 已实现 | 真实移除测试精华、列表核验通过 |
| `getEssenceMsgList` | 已实现 | 非空精华列表及消息 ID 回读通过 |
| `getGroupDetailInfo` | 已实现 | 只读实测通过 |
| `getGroupIgnoreAddRequest` | 已实现 | 真实查询通过；无法解析的 QQ 号保留 UID |
| `getGroupIgnoredNotifies` | 已实现 | 真实查询通过 |
| `getGroupInfo` | 已实现 | 只读实测通过 |
| `getGroupList` | 已实现 | 只读实测通过 |
| `getGroupMemberInfo` | 已实现 | 角色、名片、禁言及头衔回读通过 |
| `getGroupMemberList` | 已实现 | 只读实测通过 |
| `setGroupTodo` | 已实现 | 编译通过；真实操作未逐项执行 |
| `getGroupShutList` | 已实现 | 禁言成员出现、解除后消失，实测通过 |
| `setEssenceMsg` | 已实现 | 添加测试精华并回读通过 |
| `setGroupAddRequest` | 已实现 | 编译通过；真实操作未逐项执行 |
| `setGroupAdmin` | 已实现 | 修改、回读、恢复原角色通过 |
| `setGroupBan` | 已实现 | 禁言、名单回读、解除通过 |
| `setGroupCard` | 已实现 | 修改、回读、恢复原值通过 |
| `setGroupKick` | 已实现 | 临时群内踢出指定测试成员并回读通过 |
| `setGroupLeave` | 已实现 | 临时群解散通过；普通成员退群未实测 |
| `setGroupName` | 已实现 | 修改、回读、恢复原值通过 |
| `setGroupWholeBan` | 已实现 | 开启、回读、关闭通过 |
| `delGroupAlbumMedia` | 已实现 | 修正当前 QQ 的六参数签名后，删除图片及空列表回读通过 |
| `doGroupAlbumComment` | 已实现 | 真实评论及评论数增加回读通过 |
| `getGroupAlbumMediaList` | 已实现 | 非空媒体列表回读通过；按原生分页字段读取并有回归覆盖 |
| `getGroupInfoEx` | 已实现 | 只读实测通过 |
| `getQunAlbumList` | 已实现 | 空/非空相册列表实测通过 |
| `sendGroupSign` | 已实现 | 真实调用返回成功 |
| `setGroupAddOption` | 已实现 | 修改、回读、恢复原值通过 |
| `setGroupAlbumMediaLike` | 已实现 | 改用服务器返回的媒体点赞 key，点赞及取消点赞均回读通过 |
| `setGroupRemark` | 已实现 | 修改、回读、恢复原值通过 |
| `setGroupRobotAddOption` | 已实现 | 补全当前 QQ 的嵌套字段后，修改、回读、恢复原值通过 |
| `setGroupSearch` | 已实现 | 真实调用返回 1288；两字段联合/分别修改及 0/1/2 均未通过，已回读确认原值未变 |
| `setGroupSign` | 已实现 | 同一底层打卡操作通过 |
| `uploadImageToQunAlbum` | 已实现 | 真实上传图片并从相册回读通过 |
| `setGroupKickMembers` | 已实现 | 临时群内批量入口踢出一个指定成员并回读通过；多成员同次操作未验收 |
| `setGroupSpecialTitle` | 已实现 | 修改、回读、恢复原值通过 |

### 群文件

入口为 `QQNativeGroupFileApiExpand.kt`，对象位于 `expand.model`，原生调用位于 `NativeGroupFiles`，不提供旧适配器兼容层。

| 能力 | 入口 | 状态 |
| --- | --- | --- |
| 根目录及子目录分页 | `getGroupFiles` | 已实现，返回 `nextIndex`、`complete`，调用方按游标续查；列表及容量直接读取本次协议响应，不消费无请求标识的列表回调 |
| 文件详情、空间和数量 | `getGroupFile`、`getGroupFileSpace` | 已实现，读取服务端容量，不使用虚构默认值 |
| 文件临时下载地址 | `getGroupFileUrl` | 已实现，使用原生文件 ID 和真实业务标识 |
| 创建、删除文件夹 | `createGroupFolder`、`deleteGroupFolder` | 已实现，检查原生业务结果 |
| 删除、移动、重命名 | `deleteGroupFile`、`moveGroupFile`、`renameGroupFile` | 已实现，删除核对逐文件成功列表 |
| 转存永久文件 | `saveGroupFile` | 已实现，对应原生 `transGroupFile` |
| 本地及远程文件上传 | `uploadGroupFile` | 已实现，可指定目标目录，沿用消息发送链路 |

以上新增接口有离线回归覆盖，尚未在真实 QQ 逐项验收。文件 ID 直接使用列表或上传事件返回的原生标识，不使用其他适配器的编码 ID。

## 基础消息扩展

| 扩展方法 | QQ Native 状态 | 验证 |
| --- | --- | --- |
| `sendGroupMsg` | 已实现 | 底层群消息真实发送通过 |
| `sendPrivateMsg` | 已实现 | 底层私聊消息真实发送及回读通过 |
| `sendPoke` | 已实现 | 私聊、新建群通过；原测试群仍返回 1003 |
| `markAllAsRead` | 已实现 | 编译通过；真实操作未逐项执行 |
| `deleteMsg` | 已实现 | 群聊、私聊真实撤回；重启恢复原文和事件 hook 通过 |
| `forwardFriendSingleMsg` | 已实现 | 指定测试好友间单条转发通过 |
| `forwardGroupSingleMsg` | 已实现 | 真实群单条转发通过 |
| `getMsg` | 已实现 | 已发送群聊、私聊消息回读通过 |
| `markGroupMsgAsRead` | 已实现 | 真实调用返回成功 |
| `markMsgAsRead` | 已实现 | 群会话真实调用返回成功 |
| `markPrivateMsgAsRead` | 已实现 | 真实调用返回成功 |
| `arkShareGroup` | 已实现 | 原生 Ark 构造及私聊发送通过；特定瞬时错误最多重试一次 |
| `arkSharePeer` | 已实现 | 原生构造及私聊发送通过 |
| `clickInlineKeyboardButton` | 已实现 | 编译通过；真实操作未逐项执行 |
| `fetchEmojiLike` | 已实现 | 群表情回应列表非空回读通过 |
| `getEmojiLikes` | 已实现 | 群表情回应列表非空回读通过 |
| `setMsgEmojiLike` | 已实现 | 添加、回读、取消通过 |

## 共享消息与分享

| 功能 | 入口 / 替代 | 状态 |
| --- | --- | --- |
| 自定义 JSON 分享 | `builder.customCardMessage(json)` | 已实现，普通发送和新建转发均可编码 |
| 用户名片 | `builder.qqNative(api) { shareUserMessage(id) }` | 已实现，构造及私聊发送通过 |
| 群名片 | `shareGroupMessage(id)` / `arkShareGroup(id)` | 已实现，原生 Ark 构造复测通过 |
| B站/微博模板小程序 | `getMiniAppArk` / `cardMessage` | 已实现，B站模板构造及群发送通过 |
| 自定义小程序 | `getCustomMiniAppArk` | 已实现 |
| QQ 音乐小程序卡片 | `qqMusicCardMessage` | 已实现；不是音乐播放签名接口 |
| B站视频小程序卡片 | `biliVideoCardMessage` | 已实现 |
| 已读、撤回、单条转发、表情点赞、按钮点击 | `NativeBaseApiExpand.kt` 对应方法 | 已读、撤回、好友单条转发及表情回应通过；按钮未实测 |
| 输入状态 | `setInputStatus` | 已实现 |
| 消息分隔 | `adapter.message.partition()` | 已实现原生消息分隔 |
| 消息匹配 | `MessageMatch.qqNative()` / `Build.qqNative()` | 已实现 |
| 收到消息的文本、图片、语音、视频、引用、分享转换 | `adapter.message.NativeMessages` | 已支持框架标准消息类型 |

## 接收消息字段

- 商城表情保留表情 ID、表情包 ID、摘要、key 和图片地址，映射为框架图片及表情；普通消息与合并转发共用转换
- 分享卡片按 `view` 选取数据，提取应用名称、标题、描述、链接和封面，支持小程序、结构化分享及群/用户名片的字段差异
- 被 @ 用户查询好友备注或群名片及角色；查询失败保留消息中的名称，@全体成员不查询用户资料
- 上述字段已覆盖转换、SQLite 重开恢复及框架权限映射测试，本轮未进行真实账号验收

## 包结构

`expand` 保留扩展方法及消息构建作用域；`expand.model` 集中存放私聊/群聊 DTO、分享卡片对象、表情回应对象和在线状态枚举。显式引用这些类型时从 `expand.model` 导入，扩展方法仍从 `expand` 导入。

## 使用方式与签名差异

```kotlin
import com.blr19c.falowp.bot.adapter.qqnative.adapter.qqNative
import com.blr19c.falowp.bot.adapter.qqnative.adapter.expand.*

// 在 BotApi 插件接收者中转换，保留当前消息、账号和 originalClass。
val api = qqNative()
val members = api.getGroupMemberList(groupId = "目标群号")
val chain = SendMessage.builder().qqNative(api) {
    shareUserMessage("目标QQ号")
}.build()
api.sendGroup(chain, sourceId = "目标群号")
```

- `BotApi.qqNative()` 的转换规则：已经是 `QQNativeBotApi` 就直接返回，否则以当前 `receiveMessage` 和 `originalClass` 创建，不解包系统包装、不额外代理发送。
- 定时任务沿用 `SchedulingBotApiSupport` 提供的账号上下文，不在转换时重新选择账号。任务没有默认回复会话，操作时显式传入目标群号/QQ号，不使用 `sendReply`。
- 通用发送继续使用原来的插件/任务 `BotApi` 以经过系统发送 hook；`qqNative()` 用于访问 Native 扩展。
- 接收者是 `QQNativeBotApi`，无需构造临时全局 bot。公共分享作用域显式绑定当前 `api`。
- `getMsg`、撤回、单条转发和表情点赞使用当前会话定位原生消息 ID。
- `sendGroupMsg` / `sendPrivateMsg` 使用 Falowp `SendMessageChain`，返回 `Unit`；需要回执时使用核心 `QQNativeMessages.send`。
- 已读操作是会话级，可指定消息 ID 定位会话，也可直接指定群号/QQ号。消息定位按账号隔离并持久化；ID 冲突时要求指定会话，未保存的消息需同时提供会话并由原生接口核验。不会把其他会话误标为已读。
- 精华列表分开提供 `msgSeq`、`msgRandom` 和可空的原生 `messageId`；本地没有消息时不把序列号伪装成消息 ID。移除精华可用序列号和随机值。
- 在线状态使用独立 `QQNativeOnlineStatus`；自定义状态直接传 `faceId` / `faceType`。
- 公告图片、点赞用户、最近会话原始消息等复杂数据使用 `JsonNode`。
- 相册媒体列表使用 `next_has_more / next_attach_info` 自动翻页；需要自行分页时调用 `getGroupAlbumMediaPage`。相册、媒体保留 `raw` 完整字段，图片的媒体 ID/URL 从 `image.lloc / image.default_url.url` 解析；QQ 未返回的封面、创建时间和媒体字段使用可空值。
- 相册点赞参数 `id` 指上传批次 ID（原生 `batch_id`），`lloc` 指单张媒体位置；`set=false` 可取消点赞。单张媒体使用原生列表返回的 `like.key` 定位，不手工拼接媒体标识。
- `getUnidirectionalFriendList` 每页最多请求 99 条，服务端提供 `bytes_cookies` 时自动续拉并去重。满页且未提供游标字段时拒绝返回不确定的全量结果，可用 `getUnidirectionalFriendPage` 查看本页和原始响应；真实多页尚未验收。
- 头像和群相册上传支持本地路径、file URI、HTTP(S) 和 `base64://` 输入；临时文件在操作结束后清理。

## 特殊消息发送

| 能力 | 入口 | 状态 |
| --- | --- | --- |
| 本地文件、HTTP(S) 文件 | `SendMessage.Builder.fileMessage` | 已实现，支持直发和新建合并转发；远程文件发送前下载，发送结束清理临时下载 |
| 商城表情 | `marketFaceMessage` | 已实现，直发和合并转发保留 ID、包 ID、key 和显示文字 |
| 骰子与猜拳 | `diceMessage`、`rpsMessage` | 已实现，可省略或指定结果，实际显示结果仍需真实 QQ 验收 |
| 跨平台音乐播放卡片 | `qqNative(api) { musicMessage(type, id) }` | 已实现，须配置 `qqNative.musicSignUrl` 或显式传入签名地址，失败抛错 |
| 拼接历史原始元素 | `qqNative(api) { appendMessage(messageId) }` | 已实现，按 ID 在本账号历史中定位，支持跨会话及定时任务；可指定 `groupId` 或 `userId` 查询原生记录或消除 ID 歧义 |

文件、历史消息、商城表情和互动表情均支持普通发送及新建合并转发。历史消息中的媒体使用原会话信息下载，再按目标会话上传；未支持的原始元素会明确报错，不静默删除。文件在合并转发中单独成条，保留前后文字。历史消息中的引用按原会话定位，优先读取账号历史，再查询原生记录；转发记录内嵌被引用正文及消息 ID，回读时通过框架 `content.reference()` 读取，不在目标会话按序列号查找。引用支持多层嵌套（最多 8 层），原文缺失、来源不匹配或循环引用会中止发送并报错。文件可发群聊和私聊，群目录只允许用于群聊。音乐签名服务只接收歌曲平台和 ID，不接收 QQ 登录凭据；未配置时明确报错。

```kotlin
val native = qqNative()
val chain = SendMessage.builder()
    .fileMessage(Path.of("/data/report.txt"))
    .diceMessage(6)
    .qqNative(native) {
        musicMessage("qq", "歌曲ID")
    }.build()
// 使用框架给插件的 BotApi 发送，以经过发送 Hook
sendGroup(chain, sourceId = "目标群号")
```

## 专属通知

继续使用 Falowp 的 `Plugin.Listener.Event` 和 `PluginBotApi.publishEvent`，监听方式与系统事件一致。

| 类别 | 事件类 | 关键字段 |
| --- | --- | --- |
| 群管理员变更 | `QQNativeGroupAdminEvent` | `user`、`enabled` |
| 群禁言变更 | `QQNativeGroupMuteEvent` | `user`、`duration`，成员 0 表示全员，-1 表示全员禁言，0 表示解除 |
| 群名片变更 | `QQNativeGroupCardEvent` | `user`、`oldCard`、`newCard`，清空名片也会触发 |
| 群头衔变更 | `QQNativeGroupTitleEvent` | `user`、`title` |
| 群精华变更 | `QQNativeGroupEssenceEvent` | `messageId`、`sequence`、`sender`、`added`、`message: ReceiveMessage?` |
| 群文件上传 | `QQNativeGroupFileUploadEvent` | `fileId`、`name`、`size`、`busId`、`messageId`、`url: String?` |
| 消息表情回应 | `QQNativeMessageReactionEvent` | `messageId`、`sequence`、`reactions`、`added`、`message: ReceiveMessage?` |
| 私聊输入状态 | `QQNativeInputStatusEvent` | `status`、`text` |
| 资料点赞 | `QQNativeProfileLikeEvent` | `count`、`time` |

精华和表情回应优先复用当前进程已解析的原文，否则查询原生记录并结合本地记录恢复 `ReceiveMessage`。无法查询时 `message` 为 `null`，仍保留消息 ID（能查到时）与原始序列号并发布事件。`reactions` 中每项包含框架表情和数量，表示本次通知涉及的回应，不冒充完整快照。表情目录提供 QQ 表情的类型和显示文字，Unicode 回应显示对应字符；未知 ID 保留原值并使用可识别的占位名称。目录数据来自 [QQ face_config](https://github.com/NapNeko/NapCatQQ/blob/main/packages/napcat-core/external/face_config.json)，运行时不依赖其他适配器。群文件地址查询失败时 `url` 为 `null`，文件元数据和事件照常保留。QQ 未提供操作者时 `actor.id` 为 `0`，不将受影响成员冒充操作者。群名片先建立基线，初次加载不产生变更事件；重复消息和序列更早的消息不会回滚名片。损坏的 JSON/XML 通知按元素隔离，不阻断同条消息中的正文及后续通知。新增回调经过账号隔离的持久化收件箱；重放仍遵循至少一次语义。

群文件请求关联、两类文件转发编码、分块上传及失败回执、原文媒体重传、跨会话定位、通知补全与解析隔离均有离线回归覆盖。九类通知均有解析和对象转换回归覆盖，真实账号是否在各场景推送对应原生回调尚待验收。精华协议参考 [Lagrange 的通知定义](https://github.com/LagrangeDev/Lagrange.Core/blob/e1bd4baa16fa86ef000154e3eecb7816a29b63a7/Lagrange.Core/Internal/Packets/Notify/GroupEssenceMessage.cs)，表情回应和灰条参考 [NapCat 的群通知解析](https://github.com/NapNeko/NapCatQQ/blob/main/packages/napcat-onebot/api/group.ts)。

## 未适配的独立扩展

以下能力尚未实现，不提供占位接口。

| 类别 | 未适配方法 |
| --- | --- |
| 通用请求封装 | `apiRequestResult`, `apiRequest`, `apiRequestUnit` |
| 文件操作 | `getFile`, `getImage`, `getPrivateFileUrl`, `createFlashTask`, `downloadFileset`, `getFilesetId`, `getFilesetInfo`, `getFlashFileList`, `getFlashFileUrl`, `getShareLink`, `sendFlashMsg`, `sendOnlineFile`, `sendOnlineFolder`, `cancelOnlineFile`, `getOnlineFileMsg`, `receiveOnlineFile`, `refuseOnlineFile` |
| 频道 | `getGuildList`, `getGuildServiceProfile` |
| 语音 | `getAiCharacters`, `getAiRecord`, `sendGroupAiRecord`, `getRecord` |
| 流式传输 | `downloadFileStream`, `uploadFileStream`, `cleanStreamTempFile`, `downloadFileImageStream`, `downloadFileRecordStream`, `testDownloadStream` |
| 系统管理 | `canSendImage`, `canSendRecord`, `cleanCache`, `getCredentials`, `getCsrfToken`, `getDoubtFriendsAddRequest`, `getGroupSystemMsg`, `getLoginInfo`, `getStatus`, `getVersionInfo`, `setDoubtFriendsAddRequest`, `setRestart`, `botExit`, `fetchCustomFace`, `getCollectionList`, `getRKey`, `getRKeyServer`, `getRobotUinRange`, `sendPacket`, `createCollection`, `getClientKey`, `ocrImage`, `translateEn2zh` |

## 验证边界

普通 `build` 不会向真实 QQ 发送扩展操作。扩展与系统事件回归测试随普通构建运行，包含 3 个 JS 桥接测试。新增测试覆盖卡片直发与转发回读、相册分页与原始数据保留、群 Web 的 CSRF 票据选择、并发回调隔离、有效禁言筛选、UID 解析、成员管理参数、禁言范围、关闭开关的零值、原生早到回调、好友 DTO、申请定位、相册分页/取消点赞、64 位待办序列号、小程序响应转换与桥接白名单。逐项实测情况见上表。群管理操作已回读并恢复，测试精华与表情回应已移除。新建临时群、邀请测试联系人、群戳一戳、单人踢人、批量入口踢出一人和解散已完成；再次邀请被踢成员未立即加入，不计为通过。申请审批、头像/在线状态等仍有验收限制，群搜索仍返回错误，不能算全部通过。

本轮实测环境：2026-11-25，Linux arm64，NTQQ 3.2.34-53644。借助参考项目的创建/删除协议建立临时相册，真实上传图片、非空列表、评论及计数回读、点赞/取消点赞及状态回读、删除图片全部通过；临时相册已删除。多页数据使用当前原生字段的回归测试覆盖，真实多页、批次整体点赞及视频相册尚未验收。

## 补充实现对照

2026-11-25 对照了 [LuckyLilliaBot v8.3.2 的协议实现](https://github.com/LuckyLilliaDev/LuckyLilliaBot/tree/dc2b2b90092ef0f571b5546878288b83fff1e436/src/main/qqProtocol/mixins)。其最新版已把原生实现拆到独立插件，因此参考保留实现的固定版本。对照范围为戳一戳、资料修改及相册协议；本模块的业务实现仍为 Kotlin，不引入该框架运行时。
