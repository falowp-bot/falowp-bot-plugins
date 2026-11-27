# QQ 官方适配器

基于 [QQ API v2](https://bot.q.qq.com/wiki/develop/api-v2/) 重新实现，仅面向 QQ 单聊和群聊。旧频道 API、Op 协议模型和 Bot Token 鉴权已删除。

## 配置

在应用配置中填写：

```yaml
bot:
  adapter:
    qq:
      appId: "你的 AppID"
      secret: "你的 AppSecret"
      mode: websocket
      groupMessageMode: all # all：全量；at：仅 @机器人
      interactions: true # 订阅交互事件
```

插件资源会注册 `com.blr19c.falowp.bot.adapter.qq`。凭证仅从服务端配置读取；不再读取旧 `token` 配置。

默认使用 WebSocket，订阅 `GROUP_AND_C2C_EVENT` 和 `INTERACTION`。尚未获得交互权限时可设置 `interactions: false`。接收 Hello 后按平台下发的周期发送心跳，确认 READY 后完成适配器注册；断线恢复 Session，失效后重新鉴权。权限、封禁等不可恢复的关闭码会停止重连并记录日志。

使用 Webhook 时：

```yaml
bot:
  adapter:
    qq:
      appId: "你的 AppID"
      secret: "你的 AppSecret"
      mode: webhook
      webhookPath: /qq/webhook
```

在 QQ 开放平台配置公网 HTTPS 回调地址，例如 `https://bot.example.com/qq/webhook`，反向代理到框架的 `bot.system.web.port`，并订阅需要的单聊、群聊事件。回调路径不使用框架 Basic Auth，使用 AppID 校验、JDK Ed25519 原始请求体验签；支持平台回调地址验证和 opcode 12 ACK。

可选配置：`apiUrl`（默认 `https://api.bot.qq.com`）、`tokenUrl`（默认 `https://api.bot.qq.com/app/getAppAccessToken`）、`gatewayUrl`（默认 `${apiUrl}/gateway`）。使用测试环境时按开放平台要求设置对应地址。

## 结构

| 文件 | 职责 |
| --- | --- |
| `QQApplication` | 组装配置、客户端、接入方式及框架注册 |
| `core.config.QQConfig` | 配置读取和校验 |
| `core.client.QQClient` | Access Token 缓存、互斥刷新、REST 请求及错误检查 |
| `core.websocket.QQGateway` | WebSocket 握手、心跳、恢复会话和退避重连 |
| `core.webhook.QQWebhook` / `QQSignature` | HTTP 回调和 Ed25519 签名验证 |
| `QQBotApiSupport` | 事件分发、去重、已观察目标及定时任务路由 |
| `core.message.QQMessageConverter` | 消息字段映射、模式过滤与指令规范化 |
| `QQBotApi` | 框架消息链转换、按目标上传富媒体和发送 |
| `core.state.QQState` | 有界 TTL 去重状态和共享回复序号 |

接入层结构：

```text
qq/
├── QQApplication.kt          # 组装接入层、事件分发及框架注册
├── core/
│   ├── client/
│   │   ├── QQClient.kt       # 共享鉴权与 REST 客户端
│   │   └── QQApiException.kt # HTTP/业务错误与 Trace ID
│   ├── config/
│   │   ├── QQConfig.kt       # 共享配置
│   │   └── QQGroupMessageMode.kt # 群消息模式
│   ├── message/
│   │   └── QQMessageConverter.kt # 无状态消息转换
│   ├── state/
│   │   └── QQState.kt        # 共享去重/序号状态
│   ├── webhook/
│   │   ├── QQWebhook.kt      # HTTP 回调、验证、ACK
│   │   └── QQSignature.kt    # Ed25519 签名与验签
│   └── websocket/
│       └── QQGateway.kt      # 握手、心跳、恢复会话、重连
├── api/                      # 框架接口实现与消息分发
└── expand/                   # QQ 专有扩展函数
    └── model/                # QQKeyboard、QQMedia、QQMediaType 数据模型
```

两个 core 接入模块都通过 `onEvent` 回调交付事件，不直接依赖 `QQBotApiSupport` 或插件系统；WebSocket 额外通过 `onSelf` 回调交付身份信息。`QQApplication` 负责连接这些回调与公共消息处理层，确保两种接入复用同一套模式过滤、去重和转换逻辑。

## 消息行为

- 接收 `C2C_MESSAGE_CREATE`、`GROUP_AT_MESSAGE_CREATE`、`GROUP_MESSAGE_CREATE`。是否推送全量群消息取决于平台配置及权限。
- 将文本、图片、语音、视频和文件附件映射到框架消息。卡片映射为 `content.share`，引用快照通过 `content.reference()` 获取，不伪造原作者或可回复的消息 ID。完整事件保留在 `ReceiveMessage.adapter.data`。
- 指令只移除正文开头的一个 `/`；URL 和正文内部斜杠保持原样。
- 发送文本、图片、视频、语音，保持消息链顺序，合并相邻文本。富媒体先上传到实际目标的 `/files`，再携带 `media.file_info` 发消息。
- 图片沿用框架 `ImageUrl.toUrl()`，本地图片和 Base64 图片需要注册图片存储转换器。视频、语音使用可公开访问的 HTTP(S) URL。扩展支持小于 5 MiB 的原始文件字节直传，以及本地 `Path` 分片上传。
- 同来源回复自动携带 `msg_id`；群消息向发送者单聊回复也可携带原 `msg_id`，是否成功取决于用户与机器人的好友关系。跨其他目标发送不携带原消息 ID。
- `reference=true` 使用入站事件 `message_scene.ext` 的 `msg_idx` 作为引用索引；缺少该索引时发送普通被动回复。主动发送不附带引用。
- 同一条入站消息的多个 BotApi 实例共享 `msg_seq`，包括群回复与私聊回复。
- 不支持的消息链元素（如 At、Emoji、Nudge）及合并转发会明确报错，避免静默丢弃。Markdown、键盘、文件上传/发送、撤回、单聊流式消息、输入状态和图文卡片通过 QQ 扩展提供。
- `sendAllGroup` 和定时任务路由使用本次进程观察到的群、用户集合；处理入群/退群、加好友/删好友事件，不声称获取了平台完整群列表。
- 去重及序号状态保存在进程内，65 分钟过期、最多 10000 项；重启不保留。多实例部署需要自行接入共享状态或单实例接收。
- 同时检查 HTTP 状态、`err_code`、`code`，错误携带 Trace ID。发送失败不自动重试，以免重复投递；401 会使下次请求重新获取凭证。平台消息次数、时效和主动消息额度由平台校验。

## 验证

```sh
./gradlew :falowp-bot-adapter-qq:test
```

测试覆盖 WebSocket 握手、心跳与失效会话重连、官方 challenge 签名向量、原始请求体验签及篡改拒绝、回调路由、消息转换、目标隔离、引用索引、共享序号、凭证并发刷新及业务错误。测试使用 MockEngine/本地测试服务，不向 QQ 平台发送真实消息。上线前仍需使用开发机器人联调 WebSocket、回调可达性和实际收发权限。

参考：[访问凭证](https://bot.q.qq.com/wiki/develop/api-v2/dev-prepare/access-token.html)、[WebSocket](https://bot.q.qq.com/wiki/develop/api-v2/dev-prepare/event-emit/websocket.html)、[Webhook](https://bot.q.qq.com/wiki/develop/api-v2/dev-prepare/event-emit/webhook.html)、[群消息发送](https://bot.q.qq.com/wiki/develop/api-v2/autogen/api/v2_groups_group_openid_messages.post.html)。

## QQ 扩展 API

参照 NC 适配器，框架以外的能力全部定义在 `com.blr19c.falowp.bot.adapter.qq.expand`，使用 `BotApi.qq()` 或属性 `BotApi.qq` 访问。无需修改 falowp-bot-system。

```kotlin
import com.blr19c.falowp.bot.adapter.qq.api.qq
import com.blr19c.falowp.bot.adapter.qq.expand.*
import com.blr19c.falowp.bot.adapter.qq.expand.model.*
import com.blr19c.falowp.bot.system.api.SourceTypeEnum

// 默认发送到当前会话，自动使用当前消息的被动回复上下文。
botApi.qq.sendMarkdown("# 今日报告\n\n**任务已完成**", reference = true)
botApi.qq().sendMarkdown("请选择操作", keyboard = QQKeyboard(id = "键盘模板ID"))

// URL 上传并发送：支持 IMAGE / VIDEO / VOICE / FILE。
val receipt = botApi.qq.sendMedia(
    "https://example.com/report.pdf", QQMediaType.FILE,
    sourceId = "目标群 OpenID", sourceType = SourceTypeEnum.GROUP,
    content = "本周报告",
)

// 原始字节直传，无需公开 URL；文件必须非空且小于 5 MiB。
botApi.qq.sendMedia(imageBytes, "image.png", QQMediaType.IMAGE)

// 分离上传和发送：结果携带 file_info、目标及有效期。
val media = botApi.qq.uploadMedia("https://example.com/video.mp4", QQMediaType.VIDEO)
botApi.qq.sendMedia(media, content = "视频说明")

// 消息发送接口返回 JsonNode，包含平台消息 ID 等回执。
botApi.qq.recallMessage(receipt.path("id").asString(),
    sourceId = "目标群 OpenID", sourceType = SourceTypeEnum.GROUP)
```

`QQKeyboard(content = jsonNode)` 支持官方 `rows/buttons` 自定义键盘对象，与 `id` 二选一。Markdown 使用原生 `markdown.content`，不混入普通文本字段。`sendMedia(media)` 校验上传目标与有效期，跨目标需要重新上传。

`apiRequest(path, body, method)` 提供底层接口扩展点，复用 Access Token 和错误处理；调用方负责符合对应接口协议。专用消息扩展与通用 `sendReply` 共享被动回复序号和引用索引。扩展默认目标来自当前消息，定时任务或跨会话调用应显式提供 `sourceId` 和 `sourceType`。

## 全量模式和 @ 模式

`bot.adapter.qq.groupMessageMode` 与接入方式 `mode` 相互独立，WebSocket / Webhook 均支持：

- `all`（默认）：接收 `GROUP_MESSAGE_CREATE` 和 `GROUP_AT_MESSAGE_CREATE`，普通消息不会被伪造为 @机器人。
- `at`：处理 `GROUP_AT_MESSAGE_CREATE`，以及 `GROUP_MESSAGE_CREATE` 中 mentions 标记 `is_you=true`（或 ID 匹配当前机器人）的消息；过滤普通群消息。单聊始终正常接收。

在 QQ 开放平台开启“接收所有消息”才能收到全量事件；本地配置不会替代平台权限或开关。两种事件共用 `GROUP_AND_C2C_EVENT (1 << 25)`。Webhook 需在平台勾选相应事件。

全量事件的开头 `<@机器人OpenID>` 会被规范化，`<@机器人OpenID> /签到` 与 @模式下 `/签到` 都能按 `签到` 匹配插件；其他成员的 @ 标记和正文中的 URL 保留。两种事件共用基于来源、消息 ID、msg_seq 的去重键，避免重复触发插件。

参考：[群消息（全量模式）](https://bot.q.qq.com/wiki/develop/api-v2/autogen/event/group_message_create.html)。

Webhook 签名使用 JDK 25 自带的 SunEC Ed25519，无需 Bouncy Castle。密钥生成器通过一次性 seed 输入恢复 QQ 固定密钥，并检查生成私钥的 seed；签名和验签每次使用独立的 `Signature` 实例。

## 交互与关系事件

沿用 system 的 `eventListener`，无需修改框架。Webhook 需在开放平台订阅 `INTERACTION_CREATE`。

```kotlin
import com.blr19c.falowp.bot.adapter.qq.expand.event.*
import com.blr19c.falowp.bot.system.plugin.event.eventListener

eventListener<QQInteractionEvent> { event ->
    if (event.type == 11 || event.type == 12) {
        // 完成权限校验和业务处理后回应
        qq.respondInteraction(event, code = 0)
        sendReply("操作已完成")
    }
}

eventListener<QQRelationEvent> { event ->
    if (event.type == "FRIEND_ADD") qq.sendMarkdown("欢迎使用")
}
```

交互类型保留官方值：11 按钮、12 菜单、13 反馈、14 清空会话、15 故事集、16 模型切换、18/19 用户或群授权、20 群授权变化。`data` 保留完整事件 `d`，从 `data.path("data").path("resolved")` 读取按钮数据和授权范围。回调响应使用 `event.id`（`d.id`），被动消息使用 `event.eventId`（外层 `id`）。每个交互只能回应一次，多个监听器应明确由谁回应；适配器不自动替业务确认成功。

关系事件覆盖加好友、删好友、机器人入群、退群以及单聊和群聊主动消息开关变化。支持被动回复的事件会自动携带 `event_id`，跨会话发送不会沿用原事件。通知通过独立事件类型订阅，不进入通用消息插件。

事件转换放在 `core.event`，流式状态放在 `core.message`，分片上传放在 `core.media`；公开事件模型放在 `expand.event`，发送方法仍从 `botApi.qq` 调用。

## 回复模式、输入状态与卡片

```kotlin
qq.sendText("主动通知", reply = QQReply.Active)
qq.sendMarkdown("回来看看", reply = QQReply.Wakeup)
qq.sendText("事件回复", reply = QQReply.Event("外层事件 ID"))
qq.sendText("消息回复", reply = QQReply.Message("消息 ID"))
qq.sendTyping(seconds = 30)
qq.sendCard(QQCard("报告", "本周统计", "https://example.com/report", "https://example.com/cover.png"))
```

默认 `QQReply.Auto` 使用当前消息或受支持事件上下文，无上下文时主动发送。`Wakeup` 发送互动召回，不携带 `msg_id` 或 `event_id`，额度和权限由平台校验。文本、Markdown、卡片和富媒体扩展均可指定回复模式。输入状态与流式消息仅支持单聊，从群上下文调用时必须显式指定目标用户 OpenID。

## 单聊流式回复

```kotlin
val response = qq.stream(markdown = true)
response.append("# 回答\n")
response.append("第一段")
response.append("第二段", finished = true)
```

`replace` 可提交累计正文，已下发前缀不可修改。会话串行发送分片，复用首片返回的 ID 和回复序号，结束后拒绝续写。网络或平台错误后停止，不自动重发无法确认是否生效的分片。

## 本地大文件

```kotlin
qq.sendMedia(java.nio.file.Path.of("/data/report.pdf"), QQMediaType.FILE)
val media = qq.uploadMedia(java.nio.file.Path.of("/data/video.mp4"), QQMediaType.VIDEO)
qq.sendMedia(media, content = "视频")
```

本地文件最大 200 MiB。上传依次计算摘要、预上传、向签名 URL PUT 分片、确认分片并合并。并发不超过服务端配置，本地上限为 4；PUT 重试遵循服务端延迟和超时，业务确认和最终发送不自动重试。存储请求不携带 QQ Access Token。上传期间不可修改文件；空文件、超限文件和不完整分片计划会报错。
