# falowp-bot-adapter-qq-native

Linux 单容器 QQ 适配器。模块通过 Falowp 的 `plugin-conf` 注册，自带受 Falowp Web Basic Auth 保护的扫码页面。群消息会进入 Falowp 的插件消息处理流程，插件可通过 BotApi 回复。

```kotlin
implementation("com.blr19c.falowp:falowp-bot-adapter-qq-native:${project.version}")
```

使用包含此模块的本地构建时，照常打包 Ktor 应用并运行 JAR。打开 Falowp Web 服务的 `/qq-native/`，使用现有的 `bot.system.web.auth` 密码登录，再用测试账号的手机 QQ 扫码。此模块尚未发布到 Maven Central。

首次启动会从腾讯下载与容器架构匹配的 NTQQ 3.2.34-53644 Debian 包，校验固定 SHA-256 后解包。Linux 运行环境需要 `curl`、`dpkg-deb`、`xvfb-run` 和客户端的共享库。验证目标为 Angel Petals 的 Playwright Noble 基础镜像。程序和账号文件默认位于工作目录的 `data/qq-native/`。可通过 `FALOWP_QQ_DATA_DIR` 指定其他持久化目录。

## 重启与登录恢复

在 Angel Petals 的 `/app` 工作目录下，已有 `/app/data` 持久卷的部署可以直接复用。单独挂载时，为 bot 服务添加命名卷，并在后续部署中复用同一卷名。

```yaml
services:
  bot:
    hostname: falowp-qq
    networks:
      default:
        mac_address: "02:42:ac:20:00:19" # 每个实例使用不同但固定的地址
    volumes:
      - qq-native:/app/data/qq-native
volumes:
  qq-native:
    name: falowp-qq-native-data
```

`account/` 同时保存 NTQQ 自己的登录记录和适配器首次取得的设备标识、主机名及最近登录账号。启动时优先请求 NTQQ 恢复该账号。首次使用、会话过期或 QQ 要求手机确认时，页面会显示二维码。同一数据卷一次只运行一个适配器实例。备份或迁移时先停止 bot，再复制整个目录。

容器部署还应固定宿主名称和 MAC，迁移时保留原有 `/etc/machine-id`：本轮实测 Docker 的默认 MAC 在同一容器停止/启动后改变，适配器保存的 GUID 则保持不变。上面的值用于首次部署；已有登录环境应沿用当前值，不要直接换成示例地址。`networks.mac_address` 需要 Compose 2.23.2 或更新版本，见 [Docker 网络属性说明](https://docs.docker.com/reference/compose-file/container-spec/#mac_address-1)。需要重建容器时，可先把原 `/etc/machine-id` 复制到稳定路径，再只读挂载回容器的同一路径；不要改宿主操作系统的机器标识。

2026-11-25 的 Linux arm64 验收中，固定 MAC 并挂载原机器标识后，容器重建及再次重启均自动恢复登录；重启前后 MAC、保存的 GUID 与设备名称不变，原生 GUID 与保存值相同。这不能保证 QQ 永不要求手机验证，但已消除本测试环境中默认 MAC 随重启变化的问题。

启动日志会报告是否复用设备记录、原生 GUID 是否匹配、账号记录是否支持快速登录及失败结果码；不输出完整 GUID 或登录凭据。显示二维码本身不代表已确定设备变更：自动恢复不可用或 20 秒内未完成也会显示二维码。

登录与会话 AppID 必须匹配原生客户端架构：固定的 3.2.34-53644 使用 x64 `537391664`、ARM64 `537391665`。架构由 Electron 上报，Kotlin 统一选择，未知版本/架构明确报错。

扫码状态、登录恢复、设备持久化、消息收发和会话管理由 Kotlin 实现。Electron 主进程只加载腾讯 `wrapper.node` 并透传原生调用和回调，通过标准输入输出与 JVM 通信。QQ 的原生接口随客户端版本变化，升级固定版本时需重新验证登录、消息和媒体协议。

## 基础事件与 hook

普通消息、定时任务和原生通知触发的事件回复均经过系统的 `PluginBotApi` 发送包装层，支持 `SendMessageHook` 修改/拦截，以及 `SendMessageEvent`、异常和 finally hook。底层 `QQNativeBotApi` / `QQNativeMessages` 仍是直接调用入口；需要发送 hook 时使用系统交给插件的 `BotApi`，或显式用 `PluginBotApi(nativeApi)` 包装。

进退群同时解析 `onRecvSysMsg` 系统消息与灰色提示，区分 `approve`、`invite`、`leave`、`kick`、`kick_me`、`disband`。同一成员变动在两条路径间做 10 秒去重。已注册戳一戳、进退群、好友/入群申请和撤回六类系统事件，以及管理员、禁言、名片、头衔、精华、群文件上传、消息表情回应、输入状态和资料点赞九类专属事件。通知中的用户会查询群名片、昵称和角色；查不到时使用原生通知提供的名字，权限按普通成员处理，不猜测管理员身份。

文件及 `appendMessage` 支持新建合并转发；历史消息按当前账号内的 ID 定位，定时任务无需当前会话，发生 ID 冲突时指定群号或 QQ 号。精华和表情回应事件携带可空的框架接收消息，群文件上传事件携带可空的下载地址；附加查询失败不会丢弃通知。群文件列表与空间查询使用独立请求响应，避免迟到的空列表回调串扰。上述新增发送与通知行为已完成离线回归，真实 QQ 端尚待验收。

接收视频会把封面映射到 `ReceiveMessage.Video.thumbnail`，核心附件通过可空的 `QQNativeReceivedPart.Attachment.thumbnail` 传递。普通视频优先使用账号目录内可读取的原生 `thumbPath` 图片；缺失或无效时，从已下载的视频提取首帧并缓存到账号目录的 `falowp-thumbnails/`。合并转发视频解析 QQ 返回的封面资源地址。封面失败不丢弃视频正文。封面地址随消息记录保存，不额外备份缓存或远程图片，原文件清理/远程地址过期后不能保证可读。

## 消息记录与撤回恢复

默认开启消息记录。`auto` 模式检测到 `falowp-bot-utils-db` 时复用其主数据源；未引入该模块时使用本地 SQLite。两种存储均通过 Exposed 操作；`falowp-bot-utils-db` 为 `compileOnly` 可选依赖，Exposed 和 SQLite 驱动由适配器直接引入。

在应用的 `application.yaml` 中配置：

```yaml
bot:
  adapter:
    qqNative:
      history:
        enabled: true
        backend: auto
        database: ""          # db 模式的数据源名称；空字符串使用主数据源
        sqlitePath: messages.sqlite # 相对 FALOWP_QQ_DATA_DIR，也可填绝对路径
        retentionDays: 30
```

- `enabled: false` 或 `backend: memory`：仅保留最多 8192 条会话内存记录，不读取/写入持久数据库。已有数据库文件不会被删除。
- `backend: db`：必须引入并配置 `falowp-bot-utils-db`。数据源不存在、连接失败或表初始化失败会明确报错，不会自动换用 SQLite。`auto` 检测到数据库模块但配置错误时同样报错。
- `backend: sqlite`：即使引入了数据库模块，也明确使用本地 SQLite。默认文件为 `data/qq-native/messages.sqlite`，跟随账号数据卷持久化。
- 数据库表名为 `qq-native-message`，默认使用主数据库；需要建表、查询、插入、更新、删除权限。记录键包含账号、会话类型、会话 ID 和消息 ID，防止账号/群之间串记录。已保存的接收消息和本机发送回执内容可在重启后用于撤回事件。
- JVM 读取到完整消息/通知回调后，先同步写入按账号隔离的 `event-inbox/` 并刷盘，之后才释放发送回执、进入处理队列。处理完成后删除收件箱记录；进程退出、解析失败或处理未完成的记录保留，账号及监听器就绪后按顺序重放。重放采用至少一次语义，处理成功但删除记录前崩溃可能产生重复事件，插件的重要副作用需按消息 ID 去重。关闭持久化时不写该收件箱。
- 处理原始消息回调时先保存原始元素，再解析正文/下载媒体；不支持的元素和解析失败的消息也会保留原始回调。解析结果与附件地址一并存储，不额外备份附件二进制；原附件被清理后，数据库中的地址不能保证仍可下载。持久记录读取受保留期限约束，写入时最多每小时清理一次过期记录。
- 消息表同时保存账号、消息 ID 和所属会话索引，供消息 ID 已读入口定位；相同消息 ID 在不同账号或会话中不会互相覆盖。
- 持久化开启时，每次写数据库前先将记录原子写入 `history-pending/` 并刷盘。运行期间数据库写入失败时保留待重放文件；下次写入或重启后重放，撤回优先读取待重放记录。关闭持久化时也不写这些文件。
- 未曾收到/记录的消息、关闭记录后重启前的消息、过期记录无法保证恢复原文。此时仍发布 `WithdrawMessageEvent`，保留消息 ID 和已知身份，正文为空；其 `receiveMessage.adapter.data as QQNativeMessage` 的 `contentAvailable` 为 `false`，不填造假的原文。

自动化测试包含真实 SQLite 文件重开恢复、实际 `falowp-bot-utils-db` 主/指定数据源（H2）、模块缺席自动选择、关闭开关、记录隔离和过期清理；另覆盖数据库故障时本地记录跨重启重放、未知元素/解析失败时原始回调保留和完整的系统事件回复 hook 链路。MySQL/MariaDB、PostgreSQL 使用对应二进制列建表语法，尚未对这些数据库做真实服务验收。

## 三层结构

三个 Kotlin 子包共用根包 `com.blr19c.falowp.bot.adapter.qqnative`。

| 层级 | 入口 | 扩展位置 |
| --- | --- | --- |
| 核心层 | `core.QQNativeClient` | 读取会话状态，通过账号的 `contacts` 查询联系人，通过 `messages` 收取发送回执和解析引用。Kotlin 原生客户端实现位于内部包 `core.nativeapi`，进程通信位于 `core.transport`。客户端安装脚本和最小 Node 桥接位于 `src/main/resources/qq-native/core`。 |
| 适配器层 | `adapter.QQNativeApplication`、`QQNativeBotApi` | 将核心事件映射到 Falowp，处理插件权限与消息调用，组装核心和 UI。 |
| UI 交互层 | `ui.SessionRoutes.kt`、`ui/src` | Ktor 提供鉴权路由与会话状态，React 通过 HTTP 读取状态，HeroUI 提供交互组件。 |

开发前端时，在 `ui` 目录执行 `npm ci`、`npm run dev`，访问 Vite 输出地址下的 `/qq-native/`。开发代理默认连接本机 8041 端口的 Falowp。构建适配器源码需要满足 Vite 的 Node.js 版本要求并提供 npm；Gradle 会构建前端并把静态资源装入 JAR。使用已构建的适配器无需单独部署前端。

## 合并转发

`account.messages.forward(from, to, receipts)` 将同一会话里已有的消息按回执顺序合并。回执可以指向已有的转发卡片，因此可以保留内层记录。用 `readForward(from, rootId, parentId)` 逐层读取时，始终传入最外层的 `rootId`。用 `sendForwardCard(from, id, to, QQNativeForwardCardStyle(title = "测试记录"))` 可以为已有卡片指定标题并发送副本。副本保留服务端资源 ID，重启后可以从消息本身读取正文，不依赖内存映射。

Falowp 插件可以用 `sendGroup(chainA, chainB, forward = true)` 请求按链分层的转发。新建的文本、提及、QQ 表情、图片、语音和视频由 Kotlin 编码和上传；取得外层资源 ID 后才向目标会话发送一次卡片。不会通过先发送普通消息再合并的方式构造新记录，也不会因为重复使用消息链而改成转发历史消息。历史消息中的引用会读取原会话记录并内嵌正文，保留消息 ID、发送者和来源；回读优先使用内嵌内容，避免跨会话误查。引用原文缺失或出现循环时明确报错。

单条链中的 `partition()` 会把签到汇总和各个角色的明细分成同一卡片里的多条记录；语音和视频各自占一条记录。使用本模块的 `com.blr19c.falowp.bot.adapter.qqnative.adapter.message.partition` 扩展。多条链形成外层卡片和每条链的内层记录。QQ 可能把内层卡片转换为 XML，并将内容放在外层资源的子段中，读取逻辑已处理这种形式。

## Kotlin 与原生边界

非 UI 的业务实现均在 Kotlin：

- `NativeSession`：登录、二维码刷新、设备持久化和账号恢复。
- `NativeContacts`、`NativeEvents`：联系人、身份和群角色缓存，消息去重、申请、入退群、撤回和戳一戳事件。
- `NativeMessages`、`NativeDelivery`：消息转换、分段发送、引用、发送确认和已有消息合并。
- `ForwardUpload`、`ForwardReader`、`ForwardMedia`：新建转发、嵌套正文、媒体上传和下载地址。
- `Wire`、`Highway`、`MediaHashes`、`NativeMedia`：二进制协议、分块传输、校验、转码和本地文件下载。

只保留三个 `.cjs` 桥接文件：`worker.cjs` 加载原生模块并转发白名单方法/回调，`native-rpc.cjs` 无损传递 Buffer、Map、BigInt 和监听器，`packet-bridge.cjs` 校验原生客户端版本并加载二进制修正补丁。Kotlin/JVM 不能直接加载 Node 原生扩展，因此仍需 Electron/Node 运行环境。业务逻辑由 Kotlin 实现。账号入口由 `NativeAccount` 校验会话后，直接委托给联系人、消息和扩展服务；登录状态、接收消息及通知通过回调传递，不再包装成工作进程事件。消息元素在 Kotlin 层使用类型对象，仅在原生调用、协议编码和历史存储边界转换。

NTQQ 此版本的 SSO 接口会把二进制请求/响应当作 UTF-8 字符串转换，导致转发请求损坏。`src/main/native/packet.c` 的小型 N-API 补丁只修正这处二进制转换。它与指定架构的 `wrapper.node` SHA-256 绑定，版本不符会拒绝加载。源码会随 sources JAR 提供。在装有 N-API 头文件和交叉编译器的 Linux 上可重建：

```sh
sh src/main/native/build.sh arm64 x64
```

编译器可用 `ARM64_CC` / `X64_CC` 指定，头文件路径可用 `NODE_INCLUDE` 指定。

## 验证

从 `falowp-bot-plugins` 目录执行：

```sh
./gradlew :falowp-bot-adapter-qq-native:build
```

本轮普通构建通过 119 个 Kotlin 回归测试和 3 个 JS 桥接测试，另有 2 项未引入数据库模块的测试。普通构建不会向 QQ 发消息。Kotlin 测试覆盖分段、引用、私聊路由、广播路由、新建/嵌套转发、服务端 XML 转换、媒体 SHA-1 中间状态、上传回执、下载回调/重试、角色更新和事件转换。

Linux arm64 单容器实测已覆盖：持久化账号恢复、联系人和 self、普通文本/提及/表情/图片/语音/视频、引用回读、单链签到分段、两层新建转发、转发内三类媒体、定时任务的签到汇总、已有消息嵌套合并、修改卡片标题、重启后回读媒体记录。媒体使用全新内容触发真实上传，视频超过 1 MiB，并验证服务端正文与下载内容。视频缩略图还完成了真实 QQ 普通消息回读、原生封面优先、清空测试副本封面字段后的 FFmpeg 抽帧及缓存复用、合并转发封面下载和图片解码验证；SQLite 重开保留封面字段由回归测试覆盖。本轮还在授权的两个联系人间验证了私聊文本、图片、转发、名片、戳一戳和撤回；群管理修改均有回读和恢复，精华与表情回应完成添加/查询/移除。群相册真实上传、列表回读、评论、点赞/取消点赞、图片删除通过，临时相册已清理。重启后的真实撤回事件能恢复原文，事件内回复经过发送 hook。入口消费暂停期间同步保存的消息已通过真实重启、撤回原文和回复 hook 验证。临时群内戳一戳、单人踢人和批量入口踢出一个成员已通过；测试临时群已解散。广播及申请审批等保留自动化模拟覆盖，详细限制见扩展对照表。Linux x64 桥接已编译并打包，尚未完成该架构的真实账号验收。

需要复跑真实核心验收时，可在 Linux 上使用独占的已登录数据目录，显式指定测试账号、唯一测试群及本地 PNG/音频/视频文件，再运行 `liveTest`。该任务会向指定群发送验收消息，不属于普通 `build`：

```sh
export FALOWP_QQ_DATA_DIR=/app/data/qq-native
export FALOWP_QQ_LIVE_ACCOUNT=<测试QQ号>
export FALOWP_QQ_LIVE_GROUP=<已授权测试群号>
export FALOWP_QQ_LIVE_IMAGE=/fixtures/test.png
export FALOWP_QQ_LIVE_VOICE=/fixtures/test.wav
export FALOWP_QQ_LIVE_VIDEO=/fixtures/test.mp4
./gradlew :falowp-bot-adapter-qq-native:liveTest
```

运行前停止使用同一数据目录的 bot 实例。任务先验证登录账号与群列表，再发送内容；账号不匹配会直接失败。使用全新媒体内容可避免服务端缓存跳过上传。

## 扩展功能

插件中的 `BotApi` 可通过 `com.blr19c.falowp.bot.adapter.qqnative.adapter.qqNative` 扩展转换为 `QQNativeBotApi`；沿用当前消息或定时任务支持层提供的账号上下文。

私聊、群聊与公共基础扩展的入口、签名变化、已适配/未适配清单及真实验证限制见 [扩展对照表](src/main/kotlin/com/blr19c/falowp/bot/adapter/qqnative/adapter/expand/README.md)。
