# 安卓端功能实现 Prompt

## 目标

参照 iOS 参考实现（`MyApp_GitHub/MyApp/` 下全部 Swift 文件），在 `android/` 目录把安卓端补齐到功能完全一致。本仓库 `android/` 已有基础版本，按下方「当前缺口」逐条实现，不要重写已有正常工作的部分。

安卓代码位置：`android/app/src/main/java/com/luo/wechat2/`
参考 iOS 文件：`MyApp_GitHub/MyApp/`

## 文件对照表

| iOS | 安卓 |
|---|---|
| MyApp.swift（启动、开始主动消息） | WeChatApp.kt / MainActivity.kt |
| ContentView.swift（4 Tab、聊天列表、我页面、头像组件） | MainScreen.kt / Components.kt |
| ChatView.swift（聊天页、发送流程） | ChatScreen.kt |
| ChatStorage.swift（UserDefaults 存储层） | data/ChatStorage.kt |
| AppSettings.swift（设置项） | data/AppSettings.kt |
| AISettingsView.swift（AI 设置页） | ui/SettingsScreen.kt |
| AIService.swift（提示词 + DeepSeek 请求） | network/AIService.kt |
| ProactiveMessageManager.swift（主动消息 + 通知） | ProactiveMessageManager.kt |

---

## 一、应用结构

底部 4 个 Tab，选中色微信绿（`Color.Green` / `#07C160`）：

1. **微信**：聊天列表页
2. **通讯录**：占位页，标题「通讯录」，居中灰字「通讯录」
3. **发现**：占位页，标题「发现」，居中灰字「发现」
4. **我**：设置入口页

## 二、微信 Tab（聊天列表）

- 顶部搜索栏：纯 UI 不可输入。高 34dp，圆角 6，底色 `systemGray5`，左侧放大镜图标 + 灰字「搜索」，左右外边距 10dp、上下 7dp。
- 导航标题「微信」，inline 居中；右上角「+」按钮（18sp medium，点击无功能）。
- 会话列表**只有一条**：头像资源 `administerphoto`、名字 `luo`、默认最后一条 `[图片]`、时间 `10:20`、默认未读 0。
  - 时间固定显示 `10:20`，不随消息更新（保持一致）。
  - 最后一条消息：从存储读 `chat_last_message_luo`，没存过显示默认 `[图片]`。
  - 未读：只有存在未读记录 key（`chat_unread_luo` 曾被写入过）才显示存储值，否则显示默认 0。
- 列表行高 72dp；`HStack spacing 20dp`：
  - 头像 60×60、圆角 10、裁切填充（见「五、头像」）
  - 中间：名字 17sp regular + 最后一条 14sp 灰色单行截断（spacing 6）
  - 右侧：时间 11sp 灰色 + 未读红点（spacing 8）
- 未读红点：红底胶囊，最小 18×18dp，左右内边距 3dp，白字 11sp medium；>99 显示 `99+`。
- 点击进入聊天页；存储变化时（保存消息/头像/未读等）列表自动刷新。

## 三、聊天页

- 导航标题 = 聊天名（`luo`），inline；进入后隐藏底部 Tab 栏。
- 消息区：背景 `systemGroupedBackground`，内容左右 12dp、顶部 15dp，消息间距 12dp，新消息动画滚动到底部。
- 气泡样式（必须一致）：
  - **对方（左）**：头像 50×50（圆角 10）+ 文字，头像与气泡间距 10dp；气泡底色 `Color.black.tertiary`，圆角 5，内边距 左右 12 / 上下 10，字 16sp；行尾 Spacer。
  - **自己（右）**：行首 Spacer；气泡底色 `Color.green.copy(alpha = 0.2f)`，圆角 5，内边距 左右 12 / 上下 9，字 16sp。
- 首次进入（本地无历史）预置两条：对方「你h」、自己「你好！」。
- 底部输入栏：背景 `systemGray6`，左右 10 / 上下 8dp，spacing 10：
  - 左：麦克风图标 22sp（无功能）
  - 中：单行输入框，高 38dp，深色底圆角 5，回车即发送
  - 右：笑脸图标 22sp（无功能）；**输入非空时**变成绿色「发送」按钮（白字 15sp，高 32dp，左右 10dp，圆角 5），**输入为空时**显示加号图标 22sp
- 进入聊天页（onAppear）：未读清零、加载历史、主动消息计时重置。
- 监听存储变化通知刷新消息；在页内时刷新后再次确保已读。

## 四、发送与 AI 回复流程（严格按序）

`sendMessage()`：

1. 去首尾空白，空则忽略。
2. 追加自己消息 → 存消息 JSON → 存最后一条消息 → 清空输入框。
3. **重置主动消息计时**。
4. 随机延迟：取 `AppSettings.replyDelayMaxSeconds`（0–600，秒），若 >0 则随机等待 `0...maxDelay` 秒（协程 sleep）。
5. 调 `AIService.sendMessage(全部历史)`，按「六、提示词体系」拼 system prompt。
6. 回调（主线程）：追加 AI 消息 → 存消息 → 存最后一条 → **若不在聊天页则未读 +1** → **再重置主动消息计时**。
7. 无 API Key 时不发请求，直接回调 `请先配置 DeepSeek API Key`。

未读 `markAsRead` 必须加 guard（计数为 0 时不写入、不发变更通知），避免存储通知死循环。

## 五、自定义头像（当前缺口，需新实现）

存储 key：`chat_avatar_<聊天名>`，值为 JPEG 字节数组。

1. **显示优先级**（列表 60dp / 聊天页 50dp，圆角 10，centerCrop）：
   - 有自定义图 → 显示自定义图
   - 否则 `avatar == "administerphoto"` → 显示内置资源 `R.drawable.administerphoto`
   - 否则灰色占位（人形图标，26sp，底 `systemGray5`，同样 60dp 圆角 10）
2. **更换入口**：我 →「聊天头像」Section →「更换聊天头像」行（绿色相册图标 + 副标题「给 luo 上传一张照片」）。用系统相册选择器（Photo Picker / `GetContent`，不需要存储权限），选中后：
   - 等比缩放到最长边 512px
   - JPEG 压缩质量 0.85
   - 存 `chat_avatar_administerphoto` 并刷新列表/聊天头像
3. **恢复默认头像**：同 Section 内红字按钮「恢复默认头像」→ 删除该 key 并刷新。
4. 保存/删除后必须触达所有使用头像的界面（列表 + 聊天页）。

## 六、提示词体系（当前缺口，需新实现）

每次请求的 system prompt 结构：

```
(customSystemPrompt 非空 ? customSystemPrompt : systemPrompt + "\n\n" + humanChatPrompt)
+ "\n\n当前时间：" + 当前时间 HH:mm
```

- **systemPrompt + humanChatPrompt 必须与 iOS 逐字一致**，来源：
  - `MyApp_GitHub/MyApp/AIService.swift` 中 `systemPrompt`（约 8–39 行）与 `humanChatPrompt`（约 43–187 行，含「真人聊天增强 HumanChat」全文及结尾自检清单）
  - humanChat 正文原始文件：`E:\wechatapp1\给他的prompt.md`
  - 安卓现在只有简化版 `SYSTEM_PROMPT`，**必须替换补全**。
- **当前时间**：每次拼 `HH:mm`（24 小时制），普通消息和主动消息都要有。
- **主动消息**：在完整 system prompt 后再追加 `\n\n` + `PROACTIVE_PROMPT`（安卓已有该段，保留）。
- **自定义提示词**：存 key `custom_system_prompt`：
  - 空值/不存在 → 用内置默认
  - 保存规则：编辑框内容去首尾空白后为**空**或与默认全文**完全一致** → 删除该 key（恢复跟随内置）；否则存原文
  - 保存后立即生效，无需重启（每次请求现读）

## 七、DeepSeek API 规格

- URL：`POST https://api.deepseek.com/chat/completions`
- Header：`Content-Type: application/json`、`Authorization: Bearer <apiKey>`
- Body：`{"model":"deepseek-chat","messages":[{"role":"system",...},{"role":"user"/"assistant",...}],"stream":false}`
- 历史映射：`Message.isMe == true → role "user"`，否则 `"assistant"`；system 放第一条。
- 解析：`choices[0].message.content`；`choices` 为空 → `AI 没有返回内容`。
- 错误文案（必须一致）：
  - 无 Key → `请先配置 DeepSeek API Key`
  - 网络异常 → `网络请求失败，请检查网络连接`
  - HTTP 非 200 → `AI 请求失败，请检查 API Key 和网络`
  - 解析失败 → `AI 返回的数据无法解析`
- API Key 只存本机，**绝不写入源码/仓库**。

## 八、AI 设置页（我 → AI 设置）

Form/分组列表，标题「AI 设置」，4 个 Section + 保存按钮，全部改完点保存一次性生效：

1. **DeepSeek**：API Key 密码输入框（不自动大写、不纠错）+ 灰字「API Key 只保存在本机，不写入 GitHub 源码。」
2. **主动消息**：`自动回复间隔` 行，右侧数字输入框 + 「分钟」。说明文案：`例如填写 30，就是每 30 分钟检查一次主动消息。最少 1 分钟。` 保存时 `coerceIn(1, ∞)`，默认 30。
3. **回复延迟**：`最大延迟` 行，右侧数字输入框 + 「秒」。说明文案：`AI 回复前会在 0 到该秒数之间随机等待。0 表示立即回复，最多 600 秒。` 保存时 `coerceIn(0, 600)`，默认 3。
4. **提示词**：多行编辑器（高约 260dp）+ 灰字说明 `自定义 AI 的系统提示词，保存后立即生效。改回默认内容再保存，即可恢复内置提示词。`
   - 打开时显示当前生效内容：有自定义显示自定义，否则显示内置默认全文
   - 保存按「六」规则处理
5. **保存按钮**：绿色文字，宽占满，文案在 `保存设置` / `已保存`（1.5 秒后复原）间切换；保存后按新间隔**立即重置主动消息计时**。

「我」页面：Section 1「AI 设置」行（绿色脑图标 + 副标题「API Key、自动回复时间」→ 进入设置页）；Section 2 标题「聊天头像」（见第五节）。分组圆角列表样式，标题「我」。

## 九、主动消息 + 通知

- App 启动即初始化：请求通知权限（alert/sound/badge），启动单次定时器（不在构造函数里做网络/权限）。
- 定时器：单次 `N 分钟`（`proactiveMinutes`，≥1）。触发时：
  - `isGenerating` 防重入，生成中跳过本轮
  - 调 `sendProactiveMessage(完整历史)`（prompt 见第六节）
  - 回调（主线程）：置回 `isGenerating=false` → 追加消息 `isMe=false` 存入（**同时**写消息、最后一条、未读 +1）→ 安排通知 → **重置下一轮计时**
- 通知：标题 = 聊天名，正文 = 消息内容，默认声音，角标 1；**延迟 2 秒**发出；identifier/id = `proactive_luo`（发送前先取消同 id 旧通知）；仅在有通知权限时发送；**App 在前台也显示**。
- **重置计时的时机**（每次都要）：用户发消息后、AI 回复完成后、进入聊天页时、上一轮主动消息完成后、设置页保存后。
- 平台差异说明：iOS 定时器只在 App 存活时工作；安卓端最低要求对等实现（前台定时器），可选用 `AlarmManager`/`WorkManager` 实现关掉 App 也能发，属加分项不属必需。

## 十、数据存储键位表（两端 key 必须一致）

| Key | 类型 | 说明 |
|---|---|---|
| `deepseek_api_key` | String | API Key，空 = 未配置 |
| `proactive_minutes` | Int | 默认 30，最小 1 |
| `reply_delay_max_seconds` | Int | 默认 3，范围 0–600 |
| `custom_system_prompt` | String | 自定义提示词，缺省/空 = 跟随内置 |
| `chat_last_message_<名>` | String | 列表预览文本 |
| `chat_unread_<名>` | Int | 未读数；**key 存在**才算有未读记录 |
| `chat_messages_<名>` | JSON | `[{"id":"uuid","text":"...","isMe":true/false}]` |
| `chat_avatar_<名>` | ByteArray/JPEG | 自定义头像 |

所有写入需通知界面刷新（现有 ChatStorage 通知机制即可）。

## 十一、UI 常量速查（不得偏离）

- 微信绿选中色 / 发送按钮：`Color.Green`
- 列表行高 72；头像 60（列表）/ 50（聊天页），圆角 10；cell spacing 20
- 名字 17sp、预览 14sp、时间 11sp、未读 11sp、气泡 16sp
- 气泡圆角 5；左 `black.tertiary`、右 `green 20%`
- 搜索栏高 34 圆角 6；输入框高 38 圆角 5；发送按钮高 32 圆角 5
- 设置页副标题 13sp、说明文字 12sp 灰色

## 十二、当前安卓缺口清单（按优先级）

1. **提示词对齐**：补全 `systemPrompt` + `humanChatPrompt`（与 iOS 逐字一致）+ 每次请求追加「当前时间 HH:mm」
2. **自定义提示词设置**：设置页第 4 个 Section + `custom_system_prompt` 存取 + 保存规则
3. **自定义头像**：相册选图 → 512px/JPEG 0.85 → 存储 → 双处显示 → 恢复默认
4. （可选）关 App 后仍能触发主动消息（AlarmManager/WorkManager）

## 十三、实现原则

- 最小修改：已验证可用的代码不重构；对照缺口补功能即可。
- UI 不偏离既定尺寸与文案（见第十一节）。
- 不硬编码 API Key、密钥、token 到源码。
- Kotlin 代码风格与现有文件保持一致（回调 + 主线程 Handler / 协程均可，跟文件现状走）。
- 每完成一项自查：编译通过 + 与 iOS 对应行为逐条比对。
