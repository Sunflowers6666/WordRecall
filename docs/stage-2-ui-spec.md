# 阶段 2 · 小程序 UI 规范与页面设计

> 更新时间：2026-10-20 17:00（Asia/Shanghai）
> 状态：**待确认**。确认后按本规范写页面（HANDOFF §8 第 3 条）。
> 本文件不含任何密钥；文中所有数值单位除特别说明外均为 `rpx`（750 设计宽度）。

## 1. 目标与范围

阶段 2 交付 6 个页面 + 1 套设计令牌，让「选日期 → 复习当天首次学过的词」这条主线可以真机跑通。

| 页面 | 路径 | 是否 tab |
|---|---|---|
| 日历首页 | `pages/index/index` | 是 |
| 我的 | `pages/profile/profile` | 是 |
| 新学 | `pages/learn/learn` | 否 |
| 复习 | `pages/review/review` | 否 |
| 复习结果 | `pages/result/result` | 否 |
| 选择词库 | `pages/wordbook/wordbook` | 否 |

不在阶段 2：自建词库、文件导入、头像上传、深色模式（见 §10）。

## 2. 设计原则

1. **日历是主角**：首页第一屏必须是完整月份网格，不做轮播、不做运营位。
2. **一次只做一件事**：学习/复习页整屏只有一张卡片和两个按钮。
3. **留白优先**：靠间距分层，不靠边框和分隔线堆叠。
4. **安静**：无插画、无渐变、无动效库；颜色只用来表达「可点」「已选」「有错」。
5. **中文可读性优先**：正文不小于 `32rpx`，说明文字不小于 `24rpx`，不再往下。

## 3. 设计令牌

全部写成 `app.wxss` 里的 CSS 变量，页面与组件**不得写死**颜色/字号/间距。

### 3.1 颜色（浅色，1.0 不做深色）

| 变量 | 值 | 用途 |
|---|---|---|
| `--c-bg-page` | `#F6F7F5` | 页面背景（暖白，比纯白护眼） |
| `--c-bg-card` | `#FFFFFF` | 卡片、弹层、底部操作条 |
| `--c-bg-subtle` | `#EFF2EF` | 次级块、骨架屏、输入框底、遮罩底 |
| `--c-brand` | `#2C6E5B` | 主色：主按钮、选中日期、已学圆点、进度 |
| `--c-brand-strong` | `#245A4B` | 主按钮按下态 |
| `--c-brand-tint` | `#E8F1EE` | 主色浅底：标签、选中行背景 |
| `--c-text` | `#1E232B` | 主文本 |
| `--c-text-2` | `#5B6472` | 释义、次文本 |
| `--c-text-3` | `#6E7783` | 辅助说明、占位 |
| `--c-text-disabled` | `#A6AEB8` | 禁用、未来日期 |
| `--c-line` | `#E6E9E6` | 分割线、边框 |
| `--c-accent` | `#D98324` | 错词、待复习提醒 |
| `--c-danger` | `#C8483F` | 错误、危险操作 |

对比度：`--c-text` / `--c-text-2` / `--c-text-3` 对白底均 ≥ 4.5:1；`#FFFFFF` 对 `--c-brand` 约 6.0:1。`--c-text-disabled` 仅用于禁用态。

### 3.2 字号

| 变量 | 字号/行高/字重 | 用途 |
|---|---|---|
| `--f-display` | `88rpx` / `1.15` / `700` | 单词拼写（学习、复习页） |
| `--f-h1` | `48rpx` / `1.3` / `700` | 月份标题 |
| `--f-h2` | `40rpx` / `1.35` / `600` | 卡片标题、日期 |
| `--f-body` | `32rpx` / `1.6` / `400` | 正文、释义 |
| `--f-body-b` | `32rpx` / `1.6` / `600` | 按钮文字、行标题 |
| `--f-sub` | `28rpx` / `1.5` / `400` | 音标、统计、次文本 |
| `--f-caption` | `24rpx` / `1.4` / `400` | 辅助说明（最小字号） |
| `--f-num` | `36rpx` / `1` / `600` | 日历数字 |

字体栈：

- 中文/UI：`-apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", "Microsoft YaHei", sans-serif`
- 单词拼写：`Georgia, "Times New Roman", "Songti SC", serif`（衬线更有「词典」质感；安卓真机若衬线渲染不佳，回退到上面的 UI 字体，决策记录在案）

数字（日期、统计）用 `font-variant-numeric: tabular-nums`，避免切换月份时宽度跳动。

### 3.3 间距（4/8 网格）

| 变量 | 值 |
|---|---|
| `--s-1` … `--s-8` | `8 / 16 / 24 / 32 / 40 / 48 / 64rpx` |

- 页面左右内边距：`32rpx`
- 卡片内边距：`32rpx`（学习卡 `40rpx`）
- 卡片间距：`24rpx`
- 列表行最小高度：`104rpx`，行内上下 `24rpx`
- 底部固定条高度：`120rpx` + `env(safe-area-inset-bottom)`
- 页面内容底部留白：`200rpx`（避开固定条）

### 3.4 圆角与阴影

| 变量 | 值 | 用途 |
|---|---|---|
| `--r-card` | `24rpx` | 卡片 |
| `--r-btn` | `16rpx` | 按钮、输入框 |
| `--r-chip` | `8rpx` | 标签、徽标 |
| `--r-modal` | `32rpx` | 弹层 |
| `--r-circle` | `50%` | 头像、日历选中/今天 |
| `--sh-card` | `0 4rpx 16rpx rgba(30,35,43,.06)` | 普通卡片 |
| `--sh-float` | `0 8rpx 32rpx rgba(30,35,43,.10)` | 学习卡、底部条 |

### 3.5 动效

- 统一 `150ms ease-out`：按钮按下、卡片出现、月份切换（横向淡入）。
- 不做 shimmer 骨架动画、不做页面转场定制、不做手势切换（见 §8）。
- 1.0 不提供「减少动效」开关，但所有动效仅做透明度/位移，不做缩放弹跳。

## 4. 组件与状态规范

### 4.1 按钮

| 类型 | 样式 | 用法 |
|---|---|---|
| 主按钮 | 底色 `--c-brand`、白字、`96rpx` 高、`--r-btn`、`--f-body-b` | 每屏最多 1 个 |
| 次按钮 | 白底、`1rpx` `--c-line` 边框、`--c-brand` 字 | 并列的第二动作 |
| 文字按钮 | 无底无框、`--c-brand` 字、点击区 ≥ `88rpx` | 「今天」「返回日历」 |
| 危险文字 | `--c-danger` 字 | 退出登录、清空 |

状态：

- 按下：主按钮换 `--c-brand-strong`；次按钮底色换 `--c-bg-subtle`。
- 禁用：底色 `--c-line`、字 `--c-text-disabled`、无阴影、不响应。
- 加载：左侧 `32rpx` 白色旋转圈 + 文案改为进行时（「登录中…」），期间禁用点击，**禁止**同时弹 toast。
- 触控区最小 `88rpx × 88rpx`（44pt）。

### 4.2 列表行

左标题（`--f-body`）+ 右值（`--f-sub`、`--c-text-3`）+ 箭头 `›`；行高 `104rpx`，分割线 `1rpx --c-line` 且左侧内缩 `32rpx`（首行无上边线，末行无下边线）。

### 4.3 提示与弹窗

| 场景 | 方式 |
|---|---|
| 操作成功、轻提示 | `wx.showToast`（`icon: none` 或 `success`），≤ 15 字 |
| 需要用户决策 | `wx.showModal`，标题 ≤ 8 字，内容一句话，按钮「取消」「确定」 |
| 页面级错误 | **不用 toast**，用页面内错误块（避免一闪而过） |
| 表单校验失败 | 输入框下方 `--f-caption` `--c-danger` 文案，聚焦对应字段 |

### 4.4 三态（加载 / 空 / 错误）

统一由 `components/state-view` 渲染，结构：容器垂直居中 → `160rpx` 圆形浅底（`--c-bg-subtle`）内嵌内置 `<icon>` `48rpx` → 主文案（`--f-body`、`--c-text-2`）→ 辅助文案（`--f-caption`、`--c-text-3`）→ 可选按钮。

| 状态 | 图标 | 主文案 | 辅助文案 | 动作 |
|---|---|---|---|---|
| 首屏加载 | `waiting` | 骨架屏（3 行灰块），不显示文案 | — | — |
| 某日无记录 | `info` | 这天没有学习记录 | 选一个绿色圆点的日期，或开始今天的学习 | 次按钮「回到今天」 |
| 整月无记录 | `info` | 这个月还没开始学习 | 点「开始学习」添加第一批单词 | — |
| 复习无错词 | `success` | 没有错词 | 这一轮全对，继续保持 | — |
| 网络错误 | `warn` | 网络开小差了 | 检查网络后重试 | 次按钮「重试」 |
| 登录失效 | `warn` | 登录已失效 | 重新进入小程序即可恢复 | 主按钮「重新登录」 |
| 服务端 5xx | `warn` | 服务暂时不可用 | 请稍后再试 | 次按钮「重试」 |

骨架屏：3 行 `--c-bg-subtle` 圆角块（高 `28rpx`、`40rpx`、`28rpx`，间距 `16rpx`），静态无动画。

### 4.5 学习卡片（`components/word-card`）

- 白底、`--r-card`、`--sh-float`、内边距 `40rpx`、最小高 `640rpx`。
- 内容顺序：单词拼写（`--f-display`、居中）→ 音标（`--f-sub`、`--c-text-3`、居中）→ 分割线 → 词性+释义（`--f-body`、`--c-text-2`、左对齐）→ 例句块。
- 例句块：标签「例句」`--f-caption` `--c-text-3`；内容 `--f-body` `--c-text-2`；英文例句 + 中文翻译两行。
- 数据为空时（ECDICT 无例句）：显示「暂无例句」`--f-caption` `--c-text-3`，不显示标签，**不留空白占位**。
- 遮罩模式（仅新学）：释义区被 `--c-bg-subtle` 块覆盖，居中「点击查看释义」`--f-sub` `--c-text-3`；点击后淡入 `150ms` 显示释义，并保持展开。
- 错词标记：卡片右上角 `--c-chip` 徽标「错」、`--c-accent` 底、白字 `--f-caption`。

## 5. 页面规范

### 5.1 是否使用自定义导航栏：**否**

全部页面用系统默认导航栏，避免胶囊适配与状态栏高度踩坑；标题写在 `app.json`：

| 页面 | 导航标题 |
|---|---|
| index | 单词日历 |
| learn | 新学 |
| review | 复习 |
| result | 复习完成 |
| profile | 我的 |
| wordbook | 选择词库 |

`tabBar` 用系统原生，**只配文字不配图标**（`iconPath` 省略），2 个 tab：日历 / 我的。等有视觉资产再升级自定义 tabBar。

### 5.2 日历首页 `pages/index/index`

```text
┌─────────────────────────────┐
│ ‹   2026年10月        [今天] │  ← 月份行
│  一   二   三   四   五  六 日│
│          1   2   3   4   5   │
│   6   7   8   9  10  11  12   │
│  13  14  15 (16) 17  18  19   │  ← 16 选中；圆点为已学
│  20  21  22  23  24  25  26   │
│  27  28  29  30  31          │
│                              │
│ ┌─────────────────────────┐  │
│ │ 10月16日 周五            │  │  ← 选中日期摘要卡
│ │ 学了 20 词 · 错 3 词     │  │
│ │ [ 复习这天的单词 ]       │  │  ← 次按钮
│ └─────────────────────────┘  │
│                              │
│ ┌─────────────────────────┐  │  ← 固定底部条
│ │ [CET-4 ▾]    [开始学习] │  │
│ └─────────────────────────┘  │
└─────────────────────────────┘
```

- 月份行：左右箭头（点击区 `88rpx`）+ 月份标题 `--f-h1`；右侧「今天」文字按钮，当前月时隐藏。
- 星期表头：周一开头，`28rpx`、`--c-text-3`，每列宽 `(750-64)/7 ≈ 98rpx`。
- 网格固定 6 行 × 7 列，格高 `96rpx`，不因当月天数变化而跳动；非本月格留空不显示数字。
- 日期数字 `--f-num`；已学日期在数字下加 `8rpx` 圆点（`--c-brand`，选中时变白）。
- 今天：`72rpx` 圆形 `1.5rpx` `--c-brand` 描边；选中：`72rpx` `--c-brand` 实心圆 + 白字（今天+选中同时成立时以实心圆为准）。
- 未来日期：`--c-text-disabled`、不可点。
- 点击某日 → 请求该日数据 → 刷新摘要卡；点击时不整页 loading，仅在摘要卡内显示骨架。
- 摘要卡：日期 `--f-h2`；统计 `--f-sub` `--c-text-3`（「学了 N 词 · 错 M 词」；M=0 时不显示错词段）；有记录显示「复习这天的单词」次按钮，无记录换 empty 态（见 §4.4）。
- 底部固定条：左侧词库 chip（`--c-brand-tint` 底、`--c-brand` 字、点击进词库列表），右侧主按钮「开始学习」。
- 首屏加载（整月）：骨架屏；下拉刷新：开启，主色 loading。

### 5.3 新学 `pages/learn/learn`

- 顶部：`4rpx` 进度条（底 `--c-bg-subtle`、条 `--c-brand`）+ 右侧「3 / 20」`--f-sub` `--c-text-3`。
- 中部：学习卡片，默认遮罩模式（先想再看）。
- 底部：次按钮「上一个」（第一词禁用）+ 主按钮「下一个」；最后一词主按钮文案改「学完了，去复习」。
- 学完：卡片区换成完成态（「本批 20 词已学完」+ 主按钮「现在复习」+ 文字按钮「回到日历」），不跳新页面。
- 中途返回：有进度时 `wx.showModal`「要退出吗」「已学的单词会保留」。已提交到后端的记录不回滚。
- 请求失败：卡片区换成错误态 + 「重试」；已加载的批次不丢。

### 5.4 复习 `pages/review/review`

- 顶部：日期「10月16日 · 20 词」+ 进度「3 / 20」。
- 卡片：同 `word-card`，**不遮罩**（直接显示释义）。
- 底部：主按钮「记住了」+ 次按钮「没记住」；点「没记住」给当前卡片加错词徽标并前移。
- 最后一词点完 → 自动提交 → 进入结果页；提交中主按钮转 loading，禁止重复提交。
- 当天无记录：整页 empty（「这天没有学习记录」，无动作按钮）。

### 5.5 复习结果 `pages/result/result`

- 大数字：错词数（`--f-display`、有错用 `--c-accent`、全对用 `--c-brand`）。
- 文案：「20 个单词里错了 3 个」/「20 个单词全部记住」。
- 错词列表：卡片内每行「单词 + 释义」（释义单行省略），最多显示全部，超过 5 条可滚动。
- 按钮：主「再复习错词」（仅错词 > 0）、文字「返回日历」。

### 5.6 我的 `pages/profile/profile`

- 头部：头像 `120rpx` 圆形（无头像时 `--c-bg-subtle` 底 + 昵称首字）+ 昵称。
- 行 1「昵称」→ 弹层输入（`input type="nickname"`），保存 `PATCH /api/users/me`。
- 行 2「当前词库 CET-4」→ 进词库列表。
- 行 3「已学习 N 词 · N 天」（阶段 3 接口未就绪时整行不显示，不留空行）。
- 行 4「关于」→ `wx.showModal` 版本号 + 词库来源 ECDICT（MIT）。
- 头像上传放到阶段 3（需对象存储），阶段 2 只改昵称。

### 5.7 选择词库 `pages/wordbook/wordbook`

- 3 行：名称 + 词数（`--f-sub` `--c-text-3`）+ 右侧选中态（`--c-brand` 勾）。
- 点击行 → `PUT /api/users/me/wordbook` → 成功后返回并 toast「已切换为 CET-6」。
- 切换后当天已学记录不变（学习记录已绑定 `wordbook_id`）。
- 失败：行内保持原选中态 + toast 错误信息。

## 6. 小程序工程结构

```text
miniprogram/
├─ app.js              全局登录态、静默 wx.login（不阻塞首屏）
├─ app.json            页面注册、window、tabBar
├─ app.wxss            设计令牌（§3 全部 CSS 变量）
├─ project.config.json appid 留占位，由你填入
├─ config/index.js     BASE_URL（开发 / 生产）、每批词数 batchSize = 20
├─ config/feature.js   enableUserWordbook / enableFileImport = false
├─ utils/request.js    统一请求：注入 Bearer、超时 15s、错误码映射、401 重登一次
├─ utils/date.js       月份网格、日期格式化、周几
├─ utils/auth.js       ensureLogin()、token 存取
├─ components/word-card/
├─ components/state-view/
└─ pages/{index,learn,review,result,profile,wordbook}/
```

约束：

- 不引入 npm、不使用 TypeScript/Uni-app/Taro（README 已定）。
- 页面不直接写 `wx.request`，一律走 `utils/request.js`。
- 不引入 UI 库；组件只做 `word-card` 与 `state-view`。
- 基础库最低版本 `2.20.0`。

## 7. 接口契约（阶段 2，已实现并冒烟通过）

统一 `{code, message, data}`，`code=0` 成功；鉴权 `Authorization: Bearer <token>`。
已实现：`POST /api/auth/login {code}` → `{token, user}`，以及下表 7 个接口。

| 方法 | 路径 | 鉴权 | 请求 | 响应 `data` |
|---|---|---|---|---|
| GET | `/api/wordbooks` | 可选 | — | `[{id,name,description,wordCount,selected}]` |
| PUT | `/api/users/me/wordbook` | 需要 | `{wordbookId}` | `{user}` |
| PATCH | `/api/users/me` | 需要 | `{nickname}` | `{user}` |
| GET | `/api/calendar?year=2026&month=10` | 需要 | — | `{month:"2026-10", days:[{date,wordCount,wrongCount}], summary:{days,words,wrong}}` |
| POST | `/api/study/sessions` | 需要 | `{size:20}` | `{words:[{id,spelling,phonetic,meaning,exampleSentence,exampleTranslation,isWrong}], todayWordCount, remaining}` |
| GET | `/api/review?date=2026-10-16` | 需要 | — | `{date, words:[...同上]}` |
| POST | `/api/review/submit` | 需要 | `{date, wrongWordIds:[1,2]}` | `{date, total:20, wrong:3}` |

规则：

- 未登录访问 `/api/wordbooks` 时 `selected` 全为 `false`（首页可匿名浏览词库）。
- `POST /api/study/sessions`：从当前词库按 `sort_order` 取未学过的词，写入当天 `learning_record`；同一单词同一天只记一次（`uk_user_word_date`）。`size` 范围 1–50。`remaining` 是本批之后该词库剩余未学词数；`words` 为空表示词库已学完（客户端提示「去练练错词」）。
- `GET /api/review`：当天无记录返回 200 + `words: []`，客户端显示空状态，不当作错误。
- `POST /api/review/submit`：`wrongWordIds` 置 `is_wrong=1`，当天其余记录置 `0`（以最后一次提交为准）。当天无记录返回 404；单词不属于该日期返回 400。
- 日历 `wrongCount` = 当天 `is_wrong=1` 的记录数；`summary.days` = 有记录的天数。
- 400/401/404/409 统一异常处理，`message` 为可直接展示的中文；参数格式错误（如 `date=not-a-date`、`month=13`）返回 400 而不是 500。

## 8. 交互细节与边界

- **登录时机**（README 已定）：首页、词库列表可匿名浏览；「开始学习」「提交复习」「改昵称」触发 `ensureLogin()`。
- **token**：只存 `wx.setStorageSync('token')`，不进 URL、不进日志。
- **401**：静默 `wx.login` → 换 token → 原请求重放一次；再失败则页面错误态「登录已失效」。
- **重复点击**：所有请求按钮加 `loading` 锁。
- **原生返回**：页面 `onUnload` 清理未提交的复习进度前，用 `wx.showModal` 确认。
- **无网络**：优先 `wx.getNetworkType`，离线直接展示网络错误态，不发请求。
- **时间基准**：所有日期以 `Asia/Shanghai` 为准，客户端 `new Date()` 生成 `YYYY-MM-DD`，不依赖设备时区（设备时区非 +08:00 时以服务端返回为准）。
- **空词库**：当前词库已学完时，「开始学习」点击后提示「这个词库学完了，去练练错词」，并跳当天复习。

## 9. 验收标准

- [ ] §3 全部令牌落在 `app.wxss`，页面无写死色值/字号
- [ ] 6 个页面在 iOS 与 Android 真机均可打开，无横向滚动、无遮挡
- [ ] 每个页面都有加载 / 空 / 错误三态
- [ ] 首页日历：切换月份、点日期、今天、未来日期禁用全部正确
- [ ] 新学 → 学完 → 复习 → 结果 全链路可用，刷新后数据不丢
- [ ] 401 自动重登一次生效，第二次失败进入错误态
- [ ] 请求全部走 `utils/request.js`，代码中无裸 `wx.request`
- [ ] 仓库与小程序包内无任何密钥；`project.config.json` 只含 AppID
- [ ] `feature.js` 两个开关默认 `false`，对应入口不可见

## 10. 1.0 不做

深色模式、自定义 tabBar 图标、头像上传、分享卡片、离线缓存、骨架动画、手势切换卡片、自建词库、文件导入、学习提醒（订阅消息）。

## 11. 确认结果

已确认（2026-10-20）：

1. 主色 **深青绿 `#2C6E5B`**。
2. 单词拼写用**衬线体**（Georgia / Songti SC，安卓回退无衬线）。
3. tabBar **纯文字两个 tab**（日历 / 我的），不需要图标资源。
4. 本轮先补**后端接口 + 单元测试**，页面下一轮写。

沿用默认值（未反对即按此实现）：

5. 所有页面用系统默认导航栏（不自定义），标题写在 `app.json`。
6. 新学卡片默认「遮罩释义、点击查看」，做成页面内开关，复习页不遮罩。
7. 每批新学默认 20 词，阶段 2 固定，不开放设置。

待用户提供：小程序 **AppID**（可发聊天）；AppSecret 只填本地 `backend/application-secret.yml`。
