# WordRecall 交接文档

> 更新时间：2026-10-20 19:55（Asia/Shanghai）
> 新窗口请先读本文件，再读 `README.md` 和 `git log`。本文件不包含任何密码、PAT、AppSecret、JWT 密钥。

## 1. 项目基本信息

| 项目 | 值 |
|---|---|
| 小程序名称 | 单词日历 |
| 代码仓库 | WordRecall |
| 本地路径 | `E:\Develop\Projects\WordRecall` |
| GitHub 账号 | `Sunflowers6666` |
| GitHub 仓库 | `https://github.com/Sunflowers6666/WordRecall`（已创建，public） |
| 当前阶段 | **阶段 4 自建词库已完成，待真机验证**（后端 60 单测 + 19 冒烟，小程序 16 项逻辑冒烟） |

产品核心：按学习批次回看。用户选择任意历史日期，只复习自己在该日期首次学习的单词；1.0 只开放预置词库。

## 2. 当前完成状态

### 已完成

- Spring Boot 3.5.16 + Java 17 后端骨架
- MySQL `wordapp` 四张表：`wordbook`、`user`、`word`、`learning_record`
- 索引：`idx_user_date(user_id, learn_date)`、`uk_user_word_date(user_id, word_id, learn_date)` 等
- ECDICT（MIT）生成的 CET-4 / CET-6 / IELTS 词库，已导入 MySQL
- `POST /api/auth/login`：WxJava `wx-java-miniapp-spring-boot-starter:4.8.0` + JWT（JJWT 0.12.7）
- JWT filter、统一 `{code,message,data}`、400/401 异常处理
- 单元测试 7 个全部通过
- 应用启动成功；`GET /api/health` 200
- README、LICENSE、NOTICE、Issue/PR 模板、`backend-test.yml`
- 微信开发者工具缓存已迁到 `E:\Develop\WeChatDevTools\User Data`（Junction）
- GitHub 仓库已创建并推送 `main`、`develop`、`feature/stage-1-backend`
- 阶段 1 Issue **#1** 已关闭；PR **#2** 已合并到 `develop`
- GitHub Actions `backend-test` 在 PR #2 上通过（30s）
- `main` 已同步阶段 1 代码（默认分支不再只有阶段 0）
- Secret scanning 与 Push protection 均为 enabled

### 阶段 2（已合并，PR #4 → develop，2026-10-20）

- `docs/stage-2-ui-spec.md`：设计令牌（深青绿 `#2C6E5B` 主色、衬线单词字体、rpx 字号/间距/圆角）、三态规范、6 个页面布局、工程结构、接口契约、验收标准；§11 已记录确认结果
- 后端 7 个接口：`GET /api/wordbooks`、`PATCH /api/users/me`、`PUT /api/users/me/wordbook`、`GET /api/calendar`、`POST /api/study/sessions`、`GET /api/review`、`POST /api/review/submit`
- 新增 `LearningRecordService`、`StudyService`、`ReviewService`、`CalendarService` 及对应 DTO/Controller
- 单元测试 **32 个全部通过**（阶段 2 新增 25 个：日历 3、学习记录 6、新学 8、复习 4、用户 4）
- 真实 MySQL 冒烟 **34 项全部通过**（起真实 jar + JWT，覆盖匿名/登录、两批新学不重复、错词提交与回读、词库切换、401/400/404 边界）
- 修复：`?date=not-a-date` 这类参数格式错误原先返回 500，`GlobalExceptionHandler` 现在返回 400；同时补 405/415 处理
- 冒烟用测试用户与学习记录已清理（`users=1` 为真实账号，`learning_record` 已清空）

### 阶段 2 小程序端（同分支）

- AppID `wx1a3282cb951d51b8` 已写入 `miniprogram/project.config.json`；`backend/application-secret.yml` 的 `WX_MINIAPP_APPID` / `WX_MINIAPP_SECRET` 均已填，且 AppSecret 已验证被微信接受（假 code 返回 `40029`）
- 6 个页面：日历首页（tab）、新学、复习、复习结果、我的（tab）、选择词库
- 2 个组件：`word-card`（学习/复习共用，支持遮罩释义）、`state-view`（加载/空/错误三态）
- `utils/request.js`：统一 Bearer、15s 超时、错误分 network/auth/server/biz、401 静默重登并重放一次
- `utils/auth.js`：`ensureLogin()`（并发共用一次 wx.login）；首页与词库可匿名浏览
- 设计令牌全部落在 `app.wxss` 的 CSS 变量，页面不写死颜色/字号
- 自检：`python miniprogram/scripts/check.py` 通过（JSON 解析、node 语法、WXML 标签配对、页面四件套、组件路径、tabBar 引用）
- 页面逻辑冒烟：`node miniprogram/scripts/logic-test.js` **11 项通过**（月历网格、今天/未来标记、月份切换与未来月拦截、新学翻页、复习只回传错词、结果页数据、词库切换）
- 已验证（2026-10-20 17:57，开发者工具）：真实 `wx.login` → `code2Session` → 建用户 → 拉 20 词并写入 `learning_record`；页面渲染、日历回读正常
- **记录时机改为「翻过才记」（2026-10-20）**：`POST /api/study/sessions` 只预览不写记录；新增 `POST /api/study/records` 由客户端在点「下一个 / 学完了」时逐词上报（幂等，重复上报 `recorded=0`）。避免“点进学习页又退出”留下幽灵记录
- 真实接口验证：预览 5 词 → 0 条记录；上报 1 → 1 条；重复上报 → 0；再报 2 → 3 条；再次预览自动跳过已记录的 3 个
- 真机已验证（2026-10-20）：开发者工具「真机调试」+ 手机微信打开，局域网 IP `172.20.242.224:8080` + 手机端「打开调试」跳过域名校验，页面与交互正常

### 阶段 3（已合并，PR #6 → develop，2026-10-20）

- 学习统计接口 `GET /api/users/me/stats`：累计天数/词数、错词、连续天数（今天没学时从昨天起算）、今日词数
- `user.batch_size`（10/20/30，默认 20）+ 迁移脚本 `db/migration-v3-batch-size.sql`；`PATCH /api/users/me` 支持 `{nickname?, batchSize?}`
- `POST /api/study/sessions` 的 `size` 改为可选，缺省用用户设置
- 单测 32 → **41 全部通过**；真实接口冒烟 9 项通过
- **UI 2.0 令牌 v2 已落地**：蓝白 + 厚按钮（`app.wxss`、tabBar、两个组件、全部页面）；「我的」页新增统计卡与每批词数选择；学习页不再传 size
- 页面逻辑冒烟 12 项通过（新增“我的页统计与每批词数”）
- **发音（方案 A，Windows SAPI 离线生成）**：`scripts/dict/generate_audio.py` 用系统音色 `Microsoft Zira Desktop` 生成美音 mp3（约 9KB/词），输出 `backend/data/audio/us/<wordId>.mp3`；`GET /api/words/{id}/audio?accent=us` 公开返回，`Cache-Control: max-age=604800`；单词卡加了「🔊 美」按钮
  - 英音暂缺：本机无 en-GB 语音包；Windows 设置 → 时间和语言 → 语言和区域 → 添加 English (United Kingdom) 语音包后会多出 `Microsoft Hazel Desktop`，重跑脚本加 `--accent uk` 即可
  - 音频与例句数据都在 `backend/data/`（已 gitignore），换机器需重跑两个脚本
- 后端单测 41 → **46 全部通过**（新增音频服务 5 个）
- **例句（DeepSeek 批量生成，已完成）**：`scripts/dict/generate_examples.py` 按词库难度生成英文例句 + 中文翻译（CET-4 ≤8 词 / CET-6 ≤12 / IELTS ≤16），953 次调用约 51 分钟；`import_examples.py` 导入 `word.example_*`（只填空值）
  - 覆盖率：CET-4 3848/3849（100%）、CET-6 5392/5407（99.7%）、IELTS 5028/5040（99.8%），合计 14268/14296
  - 未覆盖的 28 个词保持「暂无例句」（生成结果没通过“句子必须包含目标词 + 不超词数”的校验）
  - 重新生成：删掉 `backend/data/examples/*.jsonl` 或对应行后重跑脚本（可断点续跑）
- 待做：真机适配；英音（装 en-GB 语音包后 `--accent uk`）
- **真机验证通过（2026-10-20）**：手机微信真机调试打开，日历、开始学习、例句、🔊 美 发音、统计页均正常，用户确认无问题
- 英音仍待做：装 en-GB 语音包后 `--accent uk`

### 实测数据

```text
total_words = 14296
CET-4 = 3849
CET-6 = 5407
IELTS = 5040
users = 1
learning_record = 0（测试数据已清理；真实登录验证时曾写入 40 条）
backend_tests = 41 passed
miniprogram_checks = check.py OK（JSON/JS/WXML 静态检查）
miniprogram_logic = 11 passed
audio_us = 14296 个 mp3（138.4 MB，生成 565 秒，0 失败）
examples = 14268 条入库：CET-4 3848/3849（100%）、CET-6 5392/5407（99.7%）、IELTS 5028/5040（99.8%）
```

### 未完成 / 待用户操作

- **真实微信登录已通过（2026-10-20 17:57）**：开发者工具里点「开始学习」走真实 `wx.login` → `code2Session` → 建用户（id=2）→ 写入学习记录，日历与词库接口回读正常。测试数据随后已按你的要求清空（今天从 0 开始）
- 真机（iOS / Android）**已验证通过**（2026-10-20，真机调试扫码 + 手机端打开调试；局域网 IP + http）
- 「小程序代码上传密钥」曾误粘进 `application-secret.yml`，已清理；**需在公众平台重置该上传密钥**（阶段 6 会重新生成）
- 云服务器未采购
- Dependabot 未启用（可选，需用户确认）

## 3. Git 状态

```text
main:                        阶段 1 完成，与 develop 同步（v1.0.0 时打 tag）
develop:                     8354393 Merge pull request #6（阶段 3 全部内容）+ 后续文档提交
feature/stage-3-learning:    已合并，远程分支已删除
feature/stage-2-miniprogram: 已合并，远程分支已删除
feature/stage-1-backend:     已合并，保留
工作区:                      clean
远程:                        origin https://github.com/Sunflowers6666/WordRecall.git
已推送:                      是（main / develop）
Issue:                       #1 #3 #5 全部 CLOSED
PR:                          #2 #4 #6 全部 MERGED
```

分支策略：`main` 稳定、`develop` 集成、`feature/*` 功能分支；每个阶段 PR 到 `develop` 后删除功能分支。`main` 按计划在阶段 6 统一更新。

## 4. 本机环境

| 软件 | 路径 / 说明 |
|---|---|
| JDK 17 | `E:\Develop\JDK`，`JAVA_HOME` 已指向 |
| MySQL 8.0.46 | `E:\Develop\MySQL\mysql-8.0.46-winx64`，方式 B（不注册服务） |
| MySQL 数据 | `E:\Develop\MySQL\data`，端口 3306，仅 127.0.0.1 |
| MySQL 启停 | `scripts\setup\windows\start-mysql.cmd` / `status-mysql.cmd` / `stop-mysql.cmd` |
| 密钥目录 | `E:\Develop\MySQL\.secrets\`（root/app，禁止提交） |
| Maven 备用 | `E:\Develop\Maven\apache-maven-3.10.0`，不加入 PATH |
| Maven 仓库 | `%USERPROFILE%\.m2\settings.xml` → `E:/Develop/Maven/repository` |
| GitHub CLI | `E:\Develop\gh\bin\gh.exe`，2.102.0，已登录 `Sunflowers6666`（PAT 已导入；token 文件已删除；凭据在 Windows Credential Manager） |
| 微信开发者工具 | `E:\Develop\wxDevelop\微信web开发者工具`，缓存已 junction 到 E |
| 本地 Maven 问题 | `mvnw.cmd` 首次下载 Maven 3.10.0 卡住；本机用上面的本地 Maven 构建 |

`backend/application-secret.yml` 是本地文件，已被 `.gitignore` 排除；里面是数据库密码、JWT 密钥和 `CHANGE_ME` 的微信配置。**不要提交、不要打印。**

## 5. 关键设计决定

- Spring Boot **3.5.16**（≥ 3.5.14），Spring Security 6.5.11 由 BOM 管理
- MyBatis-Plus **3.5.17**，使用 `mybatis-plus-spring-boot3-starter`，不引 JPA
- WxJava 实际 artifact 是 `wx-java-miniapp-spring-boot-starter:4.8.0`；用户最初写的 `weixin-java-...` 在 Maven Central 不存在
- JWT HS256，有效期 7 天，不做 refresh token
- 首次登录默认词库 CET-4
- 阶段 2 视觉：主色深青绿 `#2C6E5B`、单词用衬线字体、tabBar 纯文字两 tab、全部页面用系统导航栏（不做自定义导航）
- 每批新学 20 词（`miniprogram/config/index.js` 的 `batchSize`），阶段 2 不开放设置
- 日期基准：后端用 `LocalDate.now(ZoneId.of("Asia/Shanghai"))` 生成学习日期；客户端按设备本地日期展示
- 词库来源 **ECDICT（MIT）**；ECDICT 没有例句，`example_sentence` / `example_translation` 为 `null`，UI 显示“暂无例句”
- 1.0 默认隐藏自建词库和文件导入入口（阶段 4/5 用配置开关，默认 false）
- 阶段 1 只做后端骨架；新学/复习接口在阶段 2 实现（记录时机为「翻过才记」）

## 6. 常用命令

```bat
:: 启动/检查 MySQL
scripts\setup\windows\start-mysql.cmd
scripts\setup\windows\status-mysql.cmd

:: 跑测试（本机用本地 Maven；CI 用 mvnw）
cd backend
E:\Develop\Maven\apache-maven-3.10.0\bin\mvn.cmd -B test

:: 启动后端
E:\Develop\Maven\apache-maven-3.10.0\bin\mvn.cmd -B spring-boot:run

:: 一键脚本（等价，双击即可；8080 占用会提示）
scripts\setup\windows\start-backend.cmd
scripts\setup\windows\stop-backend.cmd

:: 健康检查
curl http://127.0.0.1:8080/api/health

:: 查词库数量
mysql --defaults-extra-file="E:\Develop\MySQL\.secrets\client-root.cnf" -D wordapp -e "SELECT wordbook_id, COUNT(*) FROM word GROUP BY wordbook_id;"
```

## 7. GitHub 当前状态

```text
仓库:            https://github.com/Sunflowers6666/WordRecall
可见性:          public
默认分支:        main
阶段 1 Issue:    #1 CLOSED
阶段 1 PR:       #2 MERGED → develop
阶段 2 Issue:    #3 CLOSED
阶段 2 PR:       #4 MERGED → develop（12 个提交）
CI:              backend-test + miniprogram-check；PR 与 develop push 均 SUCCESS
Secret scanning: enabled
Push protection: enabled
Dependabot:      disabled（可选，启用前需用户确认）
```

`gh auth status` 应显示已登录 `Sunflowers6666`。PAT 已导入后删除本地 token 文件，凭据在 Windows Credential Manager；新窗口通常不需要重新登录。

如凭据丢失或 `gh auth status` 显示未登录：重新创建一个 Classic PAT（`repo`、`workflow`、`read:org`），保存到 `E:\Develop\_downloads\gh-token.txt`，执行 `gh auth login --with-token < "E:\Develop\_downloads\gh-token.txt"`，然后立即删除文件。**不要把 PAT 发到聊天。**

## 8. 下一步（真机验证 → 阶段 4）

1. **真机验证**（当前重点）：
   - 电脑 IP `172.20.242.224`（WLAN「SanXiaU」），已写入 `miniprogram/config/index.js`；IP 变了要改
   - 后端监听 `0.0.0.0:8080`；Windows 防火墙对 java 有 Public 入站放行规则
   - 手机与电脑连同一 WiFi → 开发者工具「预览」或「真机调试」扫码 → 手机右上角「…」→ 打开调试（跳过域名校验）
   - 先测：首页词库名 → 开始学习 → 卡片例句 → 点 🔊 美 → 我的页统计
2. 英音（可选）：Windows 装 English (UK) 语音包 → `python scripts\dict\generate_audio.py --accent uk` → 卡片加「🔊 英」
3. 阶段 4 规划（待确认）：自建词库 + 文件导入（1.0 开关默认关闭）
4. 云服务器采购、备案、微信后台 request 合法域名（阶段 6 前）

## 9. 新窗口启动提示（直接复制）

```text
读取 E:\Develop\Projects\WordRecall\docs\HANDOFF.md，
继续 WordRecall（单词日历）项目。先检查 git status、gh auth status、
backend 测试是否通过，然后按 HANDOFF 的“下一步”继续。
不要让我在聊天里发送 PAT、AppSecret、数据库密码或 JWT 密钥。
```

## 10. 安全红线

- 不提交 `application-secret.yml`、`*.key`、`*.pem`、`.env`、PAT、SSH 私钥。
- 不把 AppSecret、数据库密码、JWT 密钥发到聊天、Issue、PR 或 Actions 日志。
- 本地密钥目录：`E:\Develop\MySQL\.secrets\`（不在仓库内）。
- 如果发现密钥进入 Git 历史：先轮换，再清理历史，不能只删除文件。
- **2026-10-20 事件**：曾把「小程序代码上传密钥」（RSA 私钥）误粘进 `backend/application-secret.yml`。已清理（文件恢复 4 个配置项），并已验证：该文件未被 Git 跟踪、历史中也无该私钥。**这把上传密钥应视为已泄露，需要在微信公众平台重置**（开发管理 → 开发设置 → 小程序代码上传）。
- 填 AppSecret 用 `scripts/setup/windows/set-wx-secret.py`：隐藏输入、校验 32 位字母数字、自动清掉误粘的多行内容；不要用记事本直接粘贴。
- 区分两个东西：**AppSecret**（32 位字母数字）与**上传密钥**（`-----BEGIN RSA PRIVATE KEY-----` 开头的私钥），后者只用于阶段 6 的 CI 上传。
- **2026-10-20 secret scanning 告警 #1**：GitHub 把 `miniprogram/project.config.json` 里的 **AppID** 标成 `tencent_wechat_api_app_id`。AppID 本就是公开信息（每个小程序客户端里都有），**不是密钥**；已在 GitHub 上标记为 `false_positive` 解决，当前 open 告警为 0。真正保密的 AppSecret 从未进入仓库。

## 11. 阶段 4：自建词库（2026-10-20，待真机验证）

### 已完成（分支 `feature/stage-4-wordbook`，Issue #7）

- 数据库：`wordbook.owner_id` + `idx_wordbook_owner` + 外键（**已应用到本机 wordapp 库**）；迁移脚本 `db/migration-v4-wordbook-owner.sql`；`schema.sql` 已加 `owner_id` 列与索引
- 后端（**60 个单测 + 19 项接口冒烟全过**）：
  - `domain/Wordbook.ownerId`；`WordbookDto` 增加 `editable`、`source`
  - 新增 `WordbookCreateRequest`、`WordbookUpdateRequest`、`WordItemRequest`、`WordbookWordsDto`
  - `WordbookService`：`listForUser` / `listBuiltin` / `create` / `update` / `delete` / `requireOwned`
  - `WordService`：`requireById` / `listByWordbook` / `countInWordbook` / `add` / `update` / `delete` / `nextSortOrder`
  - `WordbookController`：`GET/POST /api/wordbooks`、`PATCH/DELETE /api/wordbooks/{id}`、`GET/POST /api/wordbooks/{id}/words`
  - `WordController`：`GET/PUT/DELETE /api/words/{id}`
- 权限：预置词库只读（改/删一律 404）；非本人词库 404；重名 409；重复单词 409；删当前词库由外键置空 `current_wordbook_id`
- 小程序（页面逻辑冒烟 16 项通过）：
  - 词库页：预置 / 我的 分组，`+ 新建`（`enableUserWordbook` 开关控制）
  - 新增 `pages/wordbook-detail`：词条列表、设为当前词库、改名、删除词库、添加入口
  - 新增 `pages/word-edit`：新增/编辑词条表单（单词、音标、释义、例句、翻译），编辑时可删除
  - `config/feature.js` 的 `enableUserWordbook` 本地已设为 `true`

### 待做

1. **真机验证**：新建词库 → 加 5 个词 → 设为当前 → 开始学习能取到这些词
2. 走 PR → 合并 develop → 关 Issue #7
3. 发布前决定 `enableUserWordbook` 默认值（当前 true）；阶段 5 再做 CSV/Excel 导入

### 环境

```bat
scripts\setup\windows\start-mysql.cmd
scripts\setup\windows\start-backend.cmd     :: 8080，真机用 172.20.242.224:8080
```

注意：其它环境（服务器/新机器）建库时需要执行 `migration-v4-wordbook-owner.sql`。
