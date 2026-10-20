# WordRecall · 单词日历

一个面向个人和熟人小范围使用的微信小程序单词学习应用。核心不是“今天背了多少”，而是**按学习批次回看**：选择任意历史学习日期，只复习你在这个日期首次学习的单词。

- 小程序名称：**单词日历**
- 代码仓库：**WordRecall**
- 当前阶段：阶段 2（小程序 UI 规范、7 个业务接口、6 个页面已完成；待真机联调）

## 核心功能

- 预置词库：CET-4、CET-6、IELTS
- 新学：从当前词库按顺序抽取未学单词，写入当天学习记录
- 按日期复习：选择任意历史日期，返回该用户当天首次学习的单词
- 复习提交：标记错词，供后续统计使用
- 1.0 只开放预置词库；用户自建词库和文件导入入口默认隐藏

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.5.16、Spring Security 6.5.x、MyBatis-Plus 3.5.17 |
| 数据库 | MySQL 8.0、utf8mb4 |
| 认证 | WxJava 4.8.0 + JWT（JJWT 0.12.7） |
| 小程序 | 微信小程序原生框架（JavaScript，阶段 2 开始） |
| CI/CD | GitHub Actions + miniprogram-ci（阶段 6） |

不使用 Redis、消息队列、微服务、JPA。

## 目录结构

```text
E:\Develop\Projects\WordRecall\
├─ backend\                 Spring Boot 后端
│  ├─ src\main\java\com\wordrecall\
│  ├─ src\main\resources\db\schema.sql
│  ├─ src\main\resources\dict\   CET-4/CET-6/IELTS JSON
│  └─ src\test\java\
├─ miniprogram\             小程序（阶段 2：6 个页面 + 2 个组件）
├─ scripts\                 环境、词库生成等脚本
├─ docs\                    环境与设计记录
└─ .github\                 workflows、Issue/PR 模板
```

## 后端运行要求

- JDK 17+（本机：`E:\Develop\JDK`）
- MySQL 8.0（本机：`E:\Develop\MySQL\mysql-8.0.46-winx64`）
- 项目自带 `mvnw`，不需要系统 Maven
- 微信小程序 AppID；AppSecret 只放在本地/服务器环境变量

### 1. 启动 MySQL（本机方式 B）

```bat
scripts\setup\windows\start-mysql.cmd
scripts\setup\windows\status-mysql.cmd
```

### 2. 初始化数据库

```bat
mysql --defaults-extra-file="E:\Develop\MySQL\.secrets\client-root.cnf" -D wordapp < backend\src\main\resources\db\schema.sql
```

### 3. 本地密钥文件

复制 `backend\src\main\resources\application-secret.example.yml` 为 `backend\application-secret.yml`，填入本机数据库密码、JWT 密钥、AppID/AppSecret。

> 仓库里**只有示例文件** `application-secret.example.yml`（值全是 `CHANGE_ME`）；真实的 `application-secret.yml` 从未提交，已被 `.gitignore` 排除。

填 AppSecret 建议用脚本（隐藏输入、校验格式、自动清掉误粘的多行内容）：

```bat
python scripts\setup\windows\set-wx-secret.py
```

- 微信 **AppSecret** 是 32 位字母数字；「小程序代码上传密钥」是 RSA 私钥，两者不要混。私钥只用于阶段 6 的 CI 上传，不要填进这个文件。
- `backend/application-secret.yml` 已被 `.gitignore` 排除，**不要提交**。
- AppSecret 绝不写入小程序端，也不要发到聊天或 Issue。
- 生产环境不读取文件，全部使用环境变量：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`WX_MINIAPP_APPID`、`WX_MINIAPP_SECRET`。

### 4. 启动后端

```bat
cd backend
mvnw.cmd spring-boot:run
```

或者直接双击（推荐，省得记命令）：

- `scripts\setup\windows\start-backend.cmd`：启动后端，窗口保持打开就是运行中；若 8080 已被占用会提示
- `scripts\setup\windows\stop-backend.cmd`：停掉占用 8080 的后端进程

健康检查：

```text
GET http://127.0.0.1:8080/api/health
```

### 5. 测试

```bat
cd backend
mvnw.cmd test
```

阶段 1 覆盖 JWT、微信登录服务（mock WxJava）、词库导入服务；阶段 2 覆盖日历聚合、新学批次、按日期复习与错词提交、词库切换、用户昵称；CSV RFC 4180 七类边界测试在阶段 5。

小程序端自检（不需要微信开发者工具）：

```bat
cd miniprogram
python scripts\check.py       :: JSON/JS/WXML 静态检查
node scripts\logic-test.js    :: 页面逻辑冒烟
```

### 阶段 2 接口（已实现）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/wordbooks` | 词库列表（可匿名，带 token 时返回 `selected`） |
| PATCH | `/api/users/me` | 修改昵称 |
| PUT | `/api/users/me/wordbook` | 切换当前词库 |
| GET | `/api/calendar?year=&month=` | 月份日历：每天学了多少词、错了几个 |
| POST | `/api/study/sessions` | 预览一批未学单词（不写记录） |
| POST | `/api/study/records` | 上报翻过的单词，写入当天学习记录 |
| GET | `/api/words/{id}/audio?accent=us` | 单词发音（离线预生成的 mp3，公开访问、缓存 7 天） |
| GET | `/api/review?date=` | 某天首次学过的单词 |
| POST | `/api/review/submit` | 提交某天错词，回写 `is_wrong` |

详细契约（请求/响应/错误码）见 `docs/stage-2-ui-spec.md` §7。

## 小程序端配置

- AppID `wx1a3282cb951d51b8` 已写入 `miniprogram/project.config.json`。
- 打开方式、目录说明与自检命令见 [`miniprogram/README.md`](miniprogram/README.md)。
- 后端地址在 `miniprogram/config/index.js`，按运行环境区分：
  - 开发（路径 B 过渡）：`http://127.0.0.1:8080/`（真机调试改成电脑局域网 IP）
  - 生产：`https://<已备案域名>/`
- 开发阶段在微信开发者工具中关闭“校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”。
- 体验版/正式版在真机上必须使用已备案的 HTTPS 域名。
- 请求统一封装在 `miniprogram/utils/request.js`，自动携带 `Authorization: Bearer <token>`；页面不直接散落 `wx.request`。
- 登录时机：首页、词库列表不强制登录；“开始学习”“提交复习”等写操作才触发 `wx.login()`。
- 1.0 配置开关（`miniprogram/config/feature.js`，默认 `false`，阶段 4/5 再启用）：

```js
module.exports = {
  enableUserWordbook: false,  // 自建词库入口
  enableFileImport: false     // 文件导入入口
}
```

审核和用户默认不可见；待主体升级或审核策略明确后再开启。

## 词库来源与许可

预置词库由 **ECDICT** 生成：

- 仓库：<https://github.com/skywind3000/ECDICT>
- 许可证：MIT（Copyright (c) 2025 Linwei）
- ECDICT 当前不含例句，因此 `example_sentence` / `example_translation` 为 `null`，小程序端显示“暂无例句”。
- 生成脚本：`scripts/dict/build_wordbooks.py`

> 未使用 `KyleBing/dict`：该仓库当前没有 LICENSE 文件，GitHub API 的 license 为 null，不能按 MIT/Apache 2.0 合规使用。

## GitHub Actions

### 后端测试

`.github/workflows/backend-test.yml`：PR/push 到 `develop` 时运行 `./mvnw -B test`。

### 小程序上传（阶段 6）

- 微信公众平台 → 开发 → 开发设置 → 小程序代码上传 → 生成上传密钥。
- 密钥文件 Base64 后存入 GitHub Repository Secret `PRIVATE_KEY`。
- workflow 运行中解码到临时文件，上传后立即删除；不打印密钥。
- `miniprogram-ci` 只上传开发版；提交审核、体验成员、正式发布仍在微信后台手动完成。

## Issues / Pull Requests

- 每个阶段创建 Issue：`[阶段N] 标题`，正文写目标、验收标准、关联阶段。
- 分支：`main`（稳定）、`develop`（集成）、`feature/<功能>`。
- 功能分支通过 PR 合回 `develop`；PR 描述包含变更、测试结果、关联 Issue、是否需手动操作。
- 提交信息使用 Conventional Commits，例如 `feat(auth): 微信登录签发 JWT (#12)`。
- 标签：`bug`、`enhancement`、`documentation`、`infra`；不存在时由维护者创建。

## 安全说明

- 不提交：AppSecret、数据库密码、JWT 密钥、miniprogram-ci 上传私钥、SSH 私钥、云厂商 AccessKey、`.env`、`application-secret.yml`。
- 本机密钥保存在 `E:\Develop\MySQL\.secrets\`，权限仅当前用户。
- 提交前检查 `git diff --cached`；CI 中可加 secret scanning。
- 如发现密钥进入历史：先轮换密钥，再用 `git filter-repo`/BFG 清理，不能只做“后续删除”。
- 安全问题报告：请在仓库 Issues 中开一个不包含敏感信息的 issue，或联系维护者。

## 许可证

本项目使用 [MIT License](LICENSE)。MIT 允许使用、修改、分发和二次创作，但要求保留版权声明和许可声明。

## 署名与二次创作

欢迎 fork、修改和扩展。二次创作时请保留原作者署名，并在项目说明中注明：

> 基于 WordRecall（原作者：Sunflowers6666，仓库：<https://github.com/Sunflowers6666/WordRecall>）

说明：MIT 在法律上只强制保留版权声明和许可声明，不强制在显著位置展示作者名称；上面的署名是社区约定和请求，不是法律强制。

## 致谢

- [ECDICT](https://github.com/skywind3000/ECDICT)：MIT，预置词库数据来源。
- [Spring Boot](https://spring.io/projects/spring-boot)、[MyBatis-Plus](https://baomidou.com/)、[WxJava](https://github.com/Wechat-Group/WxJava)、[JJWT](https://github.com/jwtk/jjwt)：各自许可证见其仓库。
- 微信开发者工具、微信小程序平台。

## 版本记录

| 版本 | 日期 | 内容 |
|---|---|---|
| 阶段 1 | 2026-10 | 后端骨架、四张表、ECDICT 词库导入、WxJava + JWT 登录、单元测试 |
| 阶段 2（进行中） | 2026-10 | 小程序 UI 规范（`docs/stage-2-ui-spec.md`）、7 个业务接口、6 个页面 + 2 个组件、27 个单元测试、34 项接口冒烟、11 项页面逻辑冒烟 |

## 上线前检查项（阶段 6 使用）

- [ ] 小程序备案完成
- [ ] HTTPS 域名可用，证书有效
- [ ] 微信后台 request 合法域名已配置
- [ ] AppSecret 未泄露，生产环境变量已配置
- [ ] 上传密钥未入库，`PRIVATE_KEY` 仅存在 GitHub Secret
- [ ] 基础库最低版本已设置（≥ 2.20.0）
- [ ] 用户隐私保护指引状态已确认
- [ ] 生产 profile 无明文密钥
- [ ] 备份任务已运行至少一次并可恢复
- [ ] GitHub Secret scanning / Push protection 已开启
