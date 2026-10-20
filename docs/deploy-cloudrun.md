# 微信云托管部署指南（体验版）

> 适用：不买云服务器、用微信云托管跑后端的方案。
> 栈：Spring Boot 3.5 + MySQL 8 + 本地 mp3 音频（14296 个，约 138MB）。

## 0. 先看清三个硬限制（2026-10-20 按官方文档核对）

| 限制 | 影响 |
|---|---|
| 「手动上传代码包」**≤ 2 MiB**（[官方部署文档](https://developers.weixin.qq.com/miniprogram/dev/wxcloudservice/wxcloudrun/src/guide/service/online.html)） | 我们的 jar 就 71MB，**代码包上传不可用**；只能走「绑定 GitHub 构建」或「拉取镜像」 |
| 镜像构建需要 Docker | 本机未安装 Docker；走 GitHub 构建则不需要本地 Docker |
| 音频 138MB 在 `backend/data/` 且被 gitignore | GitHub 构建的镜像里**不会**有音频 → 发音全部 404 |

所以云托管有两条可行路线：

### 路线 1：GitHub 构建 + 音频提交进仓库（改动最小）
- 把 `backend/data/audio/**` 提交到 Git（仓库增加约 138MB）
- 云托管绑定 GitHub 仓库，按 `backend/Dockerfile` 构建（`COPY data/audio` 能拿到文件）
- 不需要本地 Docker、不需要对象存储、不改后端代码

### 路线 2：GitHub 构建 + 音频放对象存储（仓库干净，改动大）
- 音频上传到云托管对象存储 / COS
- `AudioService` 改成 302 跳转或代理
- 需要额外配置存储桶与访问凭据

### 路线 3（不在云托管）：轻量云服务器
- 不用 Docker、不改仓库、音频直接放服务器
- 约 ¥60–100/年，比云托管常驻便宜

## 1. 其他成本与限制

| 项目 | 说明 |
|---|---|
| 冷启动 | Spring Boot 启动约 8–15 秒；最小实例必须设 **1**，否则首个请求很慢 |
| 常驻费用 | 0.5 核 / 1G 常驻约每月几十元 |
| 域名 | 默认域名形如 `https://xxxx.run.tcloudbase.com`，腾讯已备案，可直接配为 request 合法域名 |

## 2. 创建云托管环境

1. 打开 <https://cloud.weixin.qq.com>（或微信开发者工具「云托管」面板）
2. 小程序管理员微信登录 → 创建环境（按量付费）→ 绑定 AppID `wx1a3282cb951d51b8`
3. 记下环境 ID 与默认域名

## 3. 创建 MySQL 并导入数据

1. 云托管控制台 → 数据库 → MySQL → 新建（记下内网地址、用户名、密码、库名）
2. 本地导出：

```bat
mysqldump --defaults-extra-file="E:\Develop\MySQL\.secrets\client-root.cnf" ^
  --single-transaction --routines --triggers wordapp > E:\Develop\_downloads\wordapp.sql
```

3. 导入到云托管 MySQL（控制台导入或本机 mysql 客户端连公网地址）：

```bat
mysql -h <云托管MySQL地址> -P <端口> -u <用户> -p < E:\Develop\_downloads\wordapp.sql
```

## 4. 部署服务（路线 1：GitHub 构建）

1. 云托管 → 服务 → 新建服务（名称 `wordrecall`）
2. 首次发布 → 选择代码 → **绑定 GitHub 仓库** `Sunflowers6666/WordRecall`
3. 构建设置：
   - 分支：`develop`
   - 目标目录：`backend`
   - Dockerfile：`backend/Dockerfile`
4. 端口：`8080`
5. 最小实例：`1`；规格 `0.5 核 / 1G`
6. 环境变量（面板里填，不要提交仓库）：

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<内网地址>:3306/<库名>?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=<用户>
DB_PASSWORD=<密码>
JWT_SECRET=<至少 32 字节随机串>
WX_MINIAPP_APPID=wx1a3282cb951d51b8
WX_MINIAPP_SECRET=<AppSecret>
APP_AUDIO_DIR=/app/data/audio
```

7. 健康检查路径 `/api/health`，发布。

## 5. 域名与小程序

1. 云托管 → 服务 → 域名：拿默认 HTTPS 域名
2. 微信公众平台 → 开发管理 → 开发设置 → 服务器域名 → request 合法域名填该域名
3. `miniprogram/config/index.js`：

```js
trial: 'https://xxxx.run.tcloudbase.com',
release: 'https://xxxx.run.tcloudbase.com'
```

4. 重新编译，真机不需要「打开调试」也能请求

## 6. 上传体验版

1. 公众平台 → 开发管理 → 开发设置 → 小程序代码上传 → **生成新的上传密钥**
2. 开发者工具 → 上传（版本号 `1.0.0`）
3. 公众平台 → 版本管理 → 设为体验版
4. 成员管理 → 体验成员 → 添加朋友微信号，发体验版二维码

## 7. 验证清单

- [ ] `GET https://<域名>/api/health` 返回 UP
- [ ] 真机（不开调试）能登录、日历、学习、发音
- [ ] 云托管日志无 ERROR
- [ ] 云托管 MySQL 自动备份已开启
