# 微信云托管部署指南（体验版）

> 适用：不买云服务器、用微信云托管跑后端的方案。
> 栈：Spring Boot 3.5 + MySQL 8 + 本地 mp3 音频（14296 个，约 138MB）。

## 0. 先看清三个硬限制（2026-10-20 按官方文档核对）

| 限制 | 影响 |
|---|---|
| 「手动上传代码包」**≤ 2 MiB**（[官方部署文档](https://developers.weixin.qq.com/miniprogram/dev/wxcloudservice/wxcloudrun/src/guide/service/online.html)） | 我们的 jar 就 71MB，**代码包上传不可用**；只能走「绑定 GitHub 构建」或「拉取镜像」 |
| 镜像构建需要 Docker | 本机未安装 Docker；走 GitHub 构建则不需要本地 Docker |
| 音频 138MB 在 `backend/data/` 且被 gitignore | GitHub 构建的镜像里**不会**有音频 → 发音全部 404 |

### 已采用：GitHub 构建 + 音频随仓库

- `backend/data/audio/**` 已提交（commit `7ce99e4`，14296 个 mp3）
- `backend/Dockerfile` 为**多阶段构建**：云端用 Maven 编译，再打运行镜像；不依赖本地 `target/`
- 云托管绑定 GitHub 仓库后按 `backend/Dockerfile` 构建即可，无需本地 Docker

备选（若以后仓库太大想瘦身）：音频移到对象存储 + `AudioService` 跳转；或改用轻量服务器。

## 1. 其他成本与限制

| 项目 | 说明 |
|---|---|
| 冷启动 | Spring Boot 启动约 10–20 秒。**最小实例设 0** 最省钱，代价是闲置后第一次请求要等这么久 |
| 常驻费用 | 实例缩到 0、MySQL 自动暂停时，闲置几乎不产生算力费，只剩少量存储费 |
| 域名 | 默认域名形如 `https://xxxx.run.tcloudbase.com`，腾讯已备案，可直接配为 request 合法域名 |

## 2. 创建云托管环境

1. 打开 <https://cloud.weixin.qq.com>（或微信开发者工具「云托管」面板）
2. 小程序管理员微信登录 → 创建环境（按量付费）→ 绑定 AppID `wx1a3282cb951d51b8`
3. 记下环境 ID 与默认域名

## 3. 开通云托管 MySQL 并导入数据

> 云托管的 MySQL **不在「免费快速部署」模板列表里**，它是独立资源：
> 控制台左侧菜单 **数据库 → MySQL** → 首次进入弹「开通」→ 设 root 密码 → 选 **MySQL 8.0** → 开通（约几分钟）。

- **必须选 8.0**：建表与导出的 dump 使用 `utf8mb4_0900_ai_ci`，5.7 不支持
- 大小写敏感：8.0 开通后不可改，建议选**不敏感**
- 开通后记下**内网地址**（同一环境内的服务可直连）

建库与导入：

```sql
CREATE DATABASE wordapp DEFAULT CHARACTER SET utf8mb4;
```

导入方式（建议，数据约 2.7MB）：

1. 数据库页面临时**开启外网地址**
2. 本机执行：

```bat
mysql -h <外网地址> -P <端口> -u root -p wordapp < E:\Develop\_downloads\wordapp.sql
```

3. 导入完成后**关闭外网地址**

> 注意：MySQL Serverless 默认「自动暂停」（10 分钟无连接暂停，暂停时只收存储费）。
> 恢复瞬间可能返回 `CynosDB serverless instance is resuming` 错误，导致第一个请求失败。
> 体验版建议**关闭自动暂停**（费用略高但稳定）；不关闭的话需要在后端加一次重试。

## 4. 部署服务（路线 1：GitHub 构建）

1. 云托管 → 服务 → 新建服务（名称 `wordrecall`）
2. 首次发布 → 选择代码 → **绑定 GitHub 仓库** `Sunflowers6666/WordRecall`
3. 构建设置：
   - 分支：`develop`
   - 目标目录：`backend`
   - Dockerfile：`backend/Dockerfile`
4. 端口：`8080`
5. 最小实例：**0**（省钱；闲置后首次请求等 10–20 秒）；规格 `0.5 核 / 1G`
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

> 后端已内置数据库预热重试（`DatabaseWarmup`）：启动时执行 `SELECT 1`，失败会重试最多 20 次（约 30 秒），
> 避免 MySQL「自动暂停」恢复瞬间返回 `CynosDB serverless instance is resuming` 导致第一个用户 500。
> 因此 **MySQL 可以放心开着自动暂停**。

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
- [ ] 闲置 10 分钟后再打开小程序：第一次会等 10–20 秒（冷启动 + MySQL 唤醒），属正常；之后恢复秒开
