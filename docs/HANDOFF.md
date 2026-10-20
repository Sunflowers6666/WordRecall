# WordRecall 交接文档

> 更新时间：2026-10-20 15:15（Asia/Shanghai）
> 新窗口请先读本文件，再读 `README.md` 和 `git log`。本文件不包含任何密码、PAT、AppSecret、JWT 密钥。

## 1. 项目基本信息

| 项目 | 值 |
|---|---|
| 小程序名称 | 单词日历 |
| 代码仓库 | WordRecall |
| 本地路径 | `E:\Develop\Projects\WordRecall` |
| GitHub 账号 | `Sunflowers6666` |
| GitHub 仓库 | `https://github.com/Sunflowers6666/WordRecall`（尚未创建/推送） |
| 当前阶段 | 阶段 1 本地完成，等待 GitHub PAT 授权后推送 |

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

### 实测数据

```text
total_words = 14296
CET-4 = 3849
CET-6 = 5407
IELTS = 5040
users = 0
```

### 未完成 / 待用户操作

- `gh` 尚未登录，GitHub 仓库未创建，代码未推送
- 阶段 1 Issue / PR / Actions 未创建
- Secret scanning / Push protection 未检查
- 真实微信 `code2Session` 未执行（AppID/AppSecret 仍是 `CHANGE_ME`）
- 云服务器未采购
- 阶段 2 小程序页面未开始

## 3. Git 状态

```text
main:                       e9b79cb chore: 记录微信开发者工具缓存迁移
develop:                    e9b79cb
feature/stage-1-backend:    2463fc6 feat(backend): 阶段 1 后端骨架、词库导入与微信登录
                            + 交接文档提交
工作区:                     clean
远程:                       origin https://github.com/Sunflowers6666/WordRecall.git
已推送:                     否
```

分支策略：`main` 稳定、`develop` 集成、`feature/*` 功能分支；阶段 1 完成后 PR 到 `develop`。

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
| GitHub CLI | `E:\Develop\gh\bin\gh.exe`，2.102.0，未登录 |
| 微信开发者工具 | `E:\Develop\wxDevelop\微信web开发者工具`，缓存已 junction 到 E |
| 本地 Maven 问题 | `mvnw.cmd` 首次下载 Maven 3.10.0 卡住；本机用上面的本地 Maven 构建 |

`backend/application-secret.yml` 是本地文件，已被 `.gitignore` 排除；里面是数据库密码、JWT 密钥和 `CHANGE_ME` 的微信配置。**不要提交、不要打印。**

## 5. 关键设计决定

- Spring Boot **3.5.16**（≥ 3.5.14），Spring Security 6.5.11 由 BOM 管理
- MyBatis-Plus **3.5.17**，使用 `mybatis-plus-spring-boot3-starter`，不引 JPA
- WxJava 实际 artifact 是 `wx-java-miniapp-spring-boot-starter:4.8.0`；用户最初写的 `weixin-java-...` 在 Maven Central 不存在
- JWT HS256，有效期 7 天，不做 refresh token
- 首次登录默认词库 CET-4
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

:: 健康检查
curl http://127.0.0.1:8080/api/health

:: 查词库数量
mysql --defaults-extra-file="E:\Develop\MySQL\.secrets\client-root.cnf" -D wordapp -e "SELECT wordbook_id, COUNT(*) FROM word GROUP BY wordbook_id;"
```

## 7. GitHub 待办（PAT 方式）

1. 打开 <https://github.com/settings/tokens/new>（Classic）。
2. Note：`WordRecall local gh`；Expiration：30 days。
3. Scopes：`repo`、`workflow`、`read:org`。
4. 生成后保存到 `E:\Develop\_downloads\gh-token.txt`，**不要发到聊天**。
5. 执行：

```powershell
& "E:\Develop\gh\bin\gh.exe" auth login --with-token < "E:\Develop\_downloads\gh-token.txt"
Remove-Item "E:\Develop\_downloads\gh-token.txt" -Force
& "E:\Develop\gh\bin\gh.exe" auth setup-git
& "E:\Develop\gh\bin\gh.exe" auth status
```

6. 创建仓库并推送：

```powershell
& "E:\Develop\gh\bin\gh.exe" repo create Sunflowers6666/WordRecall --public --description "WordRecall - review words by first-learned date"
cd E:\Develop\Projects\WordRecall
git push -u origin main
git push -u origin develop
git push -u origin feature/stage-1-backend
```

7. 创建阶段 1 Issue 和 feature → develop 的 PR；PR 会触发 `backend-test.yml`。
8. 在 GitHub Settings → Code security 检查 Secret scanning / Push protection。

## 8. 下一步

1. 完成 GitHub 推送、Issue/PR、CI 绿色。
2. 用户提供 AppID，并在本地 `backend/application-secret.yml` 填入真实 AppSecret；从微信开发者工具取 code，验证真实 `code2Session`。
3. 阶段 2：先给小程序 UI 规范（配色、字号、间距、圆角、按钮/空/加载/错误状态），确认后写页面。
4. 阶段 1 末尾的云服务器采购清单已在对话中给出；采购、备案、微信后台操作由用户完成。

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
