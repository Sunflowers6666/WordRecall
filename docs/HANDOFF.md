# WordRecall 交接文档

> 更新时间：2026-10-20 17:45（Asia/Shanghai）
> 新窗口请先读本文件，再读 `README.md` 和 `git log`。本文件不包含任何密码、PAT、AppSecret、JWT 密钥。

## 1. 项目基本信息

| 项目 | 值 |
|---|---|
| 小程序名称 | 单词日历 |
| 代码仓库 | WordRecall |
| 本地路径 | `E:\Develop\Projects\WordRecall` |
| GitHub 账号 | `Sunflowers6666` |
| GitHub 仓库 | `https://github.com/Sunflowers6666/WordRecall`（已创建，public） |
| 当前阶段 | **阶段 2 功能已完成**：规范、7 个接口、6 个页面 + 2 个组件；待开发者工具/真机联调 |

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

### 阶段 2（进行中，分支 `feature/stage-2-miniprogram`，PR #4）

- `docs/stage-2-ui-spec.md`：设计令牌（深青绿 `#2C6E5B` 主色、衬线单词字体、rpx 字号/间距/圆角）、三态规范、6 个页面布局、工程结构、接口契约、验收标准；§11 已记录确认结果
- 后端 7 个接口：`GET /api/wordbooks`、`PATCH /api/users/me`、`PUT /api/users/me/wordbook`、`GET /api/calendar`、`POST /api/study/sessions`、`GET /api/review`、`POST /api/review/submit`
- 新增 `LearningRecordService`、`StudyService`、`ReviewService`、`CalendarService` 及对应 DTO/Controller
- 单元测试 **27 个全部通过**（新增 20 个：日历 3、学习记录 6、新学 3、复习 4、用户 4）
- 真实 MySQL 冒烟 **34 项全部通过**（起真实 jar + JWT，覆盖匿名/登录、两批新学不重复、错词提交与回读、词库切换、401/400/404 边界）
- 修复：`?date=not-a-date` 这类参数格式错误原先返回 500，`GlobalExceptionHandler` 现在返回 400；同时补 405/415 处理
- 冒烟用测试用户与学习记录已清理（`users=0`、`learning_record=0`）

### 阶段 2 小程序端（同分支）

- AppID `wx1a3282cb951d51b8` 已写入 `miniprogram/project.config.json`；`backend/application-secret.yml` 的 `WX_MINIAPP_APPID` 也已填（AppSecret 仍是 `CHANGE_ME`）
- 6 个页面：日历首页（tab）、新学、复习、复习结果、我的（tab）、选择词库
- 2 个组件：`word-card`（学习/复习共用，支持遮罩释义）、`state-view`（加载/空/错误三态）
- `utils/request.js`：统一 Bearer、15s 超时、错误分 network/auth/server/biz、401 静默重登并重放一次
- `utils/auth.js`：`ensureLogin()`（并发共用一次 wx.login）；首页与词库可匿名浏览
- 设计令牌全部落在 `app.wxss` 的 CSS 变量，页面不写死颜色/字号
- 自检：`python miniprogram/scripts/check.py` 通过（11 JSON / 15 JS / 8 WXML：JSON 解析、node 语法、WXML 标签配对、页面四件套、组件路径、tabBar 引用）
- 页面逻辑冒烟：`node miniprogram/scripts/logic-test.js` **11 项通过**（月历网格、今天/未来标记、月份切换与未来月拦截、新学翻页、复习只回传错词、结果页数据、词库切换）
- 未验证：微信开发者工具真实渲染、真机 iOS/Android、真实 `code2Session`

### 实测数据

```text
total_words = 14296
CET-4 = 3849
CET-6 = 5407
IELTS = 5040
users = 0
learning_record = 0（冒烟测试数据已清理）
backend_tests = 27 passed
miniprogram_checks = check.py OK（11 JSON / 15 JS / 8 WXML）
miniprogram_logic = 11 passed
```

### 未完成 / 待用户操作

- 真实 `code2Session` **凭据已验证**（2026-10-20）：本地 AppSecret 被微信接受（假 code 返回 `40029 invalid code`），后端日志 `wx login failed, errorCode=40029`，接口返回 400/50001。**还差一次真实 wx.login 的 code 走完整登录**
- 小程序未在微信开发者工具/真机里跑过（静态检查与逻辑冒烟已过，渲染与真机行为待验证）
- 「小程序代码上传密钥」曾误粘进 `application-secret.yml`，已清理；**需在公众平台重置该上传密钥**（阶段 6 会重新生成）
- 云服务器未采购
- Dependabot 未启用（可选，需用户确认）

## 3. Git 状态

```text
main:                        阶段 1 完成，与 develop 同步（v1.0.0 时打 tag）
develop:                     97a624c docs: 记录 main 同步阶段 1
feature/stage-2-miniprogram: 阶段 2 规范 + 7 个接口 + 6 个页面（当前分支，PR #4 ready）
feature/stage-1-backend:     已合并，保留
工作区:                      clean
远程:                        origin https://github.com/Sunflowers6666/WordRecall.git
已推送:                      是（main / develop / feature/stage-2-miniprogram）
Issue:                       #1 CLOSED；#3 OPEN（阶段 2）
PR:                          #2 MERGED；#4 OPEN（阶段 2，ready for review，CI 见 PR 页面）
```

分支策略：`main` 稳定、`develop` 集成、`feature/*` 功能分支；阶段 1 已 PR 到 `develop`。`main` 按计划在阶段 6 统一更新。

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
- 阶段 1 不实现新学/复习 API；阶段 2/3 再做

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
CI:              PR run 39394653123 SUCCESS（30s）；develop push run 39394993025 SUCCESS（17s）
Secret scanning: enabled
Push protection: enabled
Dependabot:      disabled（可选，启用前需用户确认）
```

`gh auth status` 应显示已登录 `Sunflowers6666`。PAT 已导入后删除本地 token 文件，凭据在 Windows Credential Manager；新窗口通常不需要重新登录。

如凭据丢失或 `gh auth status` 显示未登录：重新创建一个 Classic PAT（`repo`、`workflow`、`read:org`），保存到 `E:\Develop\_downloads\gh-token.txt`，执行 `gh auth login --with-token < "E:\Develop\_downloads\gh-token.txt"`，然后立即删除文件。**不要把 PAT 发到聊天。**

## 8. 下一步（阶段 2 收尾）

1. 微信开发者工具导入 `miniprogram/`，起本地后端（8080），走一遍：日历 → 开始学习（触发真实 `wx.login`）→ 复习 → 结果 → 切换词库 → 改昵称。
2. 真机调试：把 `miniprogram/config/index.js` 的 develop 地址改成电脑局域网 IP，手机与电脑同一网络。
3. 重置「小程序代码上传密钥」（误粘过），阶段 6 再正式生成并存入 GitHub Secret。
4. 云服务器采购、备案、微信后台 request 合法域名配置由用户完成。
5. 阶段 2 不做用户自建词库/文件导入（阶段 4/5，入口默认关闭）。

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
