# 单词日历 · 小程序端（阶段 2）

微信小程序原生代码（WXML / WXSS / JS），不使用 TypeScript、Uni-app、Taro、npm。

## 打开方式

1. 微信开发者工具 → 导入项目 → 目录选 `E:\Develop\Projects\WordRecall\miniprogram`
2. AppID 已写入 `project.config.json`（`wx1a3282cb951d51b8`）
3. 详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」
4. 先启动后端：`cd backend && mvnw.cmd spring-boot:run`（或 `E:\Develop\Maven\apache-maven-3.10.0\bin\mvn.cmd -B spring-boot:run`）

开发环境的接口地址在 `config/index.js`：默认 `http://127.0.0.1:8080`。
真机调试时改成电脑的局域网 IP（例如 `http://192.168.1.10:8080`），手机和电脑要在同一网络。

## 目录

```text
miniprogram/
├─ app.js / app.json / app.wxss   全局入口、页面与 tabBar、设计令牌
├─ project.config.json            AppID 与编译设置
├─ sitemap.json
├─ config/index.js                接口地址、每批词数、运行环境
├─ config/feature.js              1.0 功能开关（自建词库/文件导入，默认 false）
├─ utils/request.js               统一请求：Bearer、超时、错误分类、401 重登一次
├─ utils/auth.js                  wx.login、token 与用户缓存、ensureLogin
├─ utils/date.js                  月历网格、日期格式化
├─ utils/feedback.js              toast 与错误类型
├─ components/word-card/          单词卡（学习/复习共用）
├─ components/state-view/         加载 / 空 / 错误三态
├─ pages/index/                   日历首页（tab）
├─ pages/learn/                   新学
├─ pages/review/                  按日期复习
├─ pages/result/                  复习结果
├─ pages/profile/                 我的（tab）
├─ pages/wordbook/                选择词库
└─ scripts/
   ├─ check.py                    静态检查（JSON/JS/WXML 配对/文件齐全）
   └─ logic-test.js               页面逻辑冒烟（假 wx 运行时）
```

## 自检命令

```bat
cd miniprogram
python scripts\check.py
node scripts\logic-test.js
```

两个脚本都不需要微信开发者工具，改完代码可以先跑一遍再进 IDE。

## 约定

- 页面不直接调用 `wx.request`，一律走 `utils/request.js`。
- 样式只用 `app.wxss` 里的 CSS 变量，不写死颜色和字号。
- 颜色、字号、间距、圆角、三态规范见 `docs/stage-2-ui-spec.md`。
- 不提交 `project.private.config.json`（开发者工具本地设置，已 gitignore）。
