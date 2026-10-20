# 云服务器部署指南（腾讯云轻量 · Ubuntu 22.04）

> 适用于：备案在腾讯云、购买腾讯云轻量应用服务器。
> 后端：Spring Boot 3.5 + MySQL 8 + Nginx + HTTPS。音频随仓库提交，目录 `backend/data/audio`（14296 个，138MB）。

## 0. 买什么

| 项 | 建议 |
|---|---|
| 产品 | **轻量应用服务器**（不是 CVM/ECS，轻量更便宜、防火墙简单） |
| 配置 | 2 核 4G（约 ¥200–300/年）或 2 核 2G（约 ¥70–120/年，JVM 限 512M 也够） |
| 带宽 | 3–5 Mbps，月流量 200G+ |
| 系统盘 | 40–60G SSD |
| 镜像 | **Ubuntu 22.04 LTS**（纯净系统，不要宝塔面板） |
| 地域 | 上海 / 广州 / 北京 任选，离你近即可 |

不要买：1 核 1G、突发性能 t6、GPU、高防。

活动页：<https://cloud.tencent.com/act/pro/lighthouse>

## 1. 购买后立即做

1. 控制台 → 轻量 → 服务器 → **防火墙**：放行 `22`、`80`、`443`；**不要**放行 8080（只走 Nginx）
2. 记下公网 IP；设置 root 密码或绑定 SSH 密钥
3. 等 1–2 分钟开机完成

## 2. 初始化服务器（一次性）

本机把脚本传上去（或在服务器上用 nano 粘贴）：

```bat
scp scripts\deploy\server-init.sh root@<公网IP>:/root/
ssh root@<公网IP> "bash /root/server-init.sh"
```

脚本会：装 JDK 17 / MySQL 8 / Nginx / certbot、建库与 `wordapp` 账号（只给读写权限）、生成 `/etc/wordrecall.env`、装 systemd 服务、配置 80 端口反代、2G 内存机器自动加 swap。

过程中会让你输入：数据库密码（自己定）、小程序 AppID / AppSecret（AppSecret 不会回显）。

## 3. 上传程序与数据

本机执行：

```bat
set SERVER_IP=<公网IP>
bash scripts/deploy/push.sh
```

会做：本地打包 jar → 上传 jar → rsync 音频（138MB，首次较慢）→ 上传 `wordapp.sql` → 远程改权限并重启服务。

然后导入数据库（在服务器上）：

```bash
mysql wordapp < /root/wordapp.sql
mysql -e "SELECT COUNT(*) FROM wordapp.word; SELECT COUNT(*) FROM wordapp.wordbook;"
systemctl restart wordrecall
curl -s http://127.0.0.1:8080/api/health
```

> 注意：必须先建表结构再导数据 —— `wordapp.sql` 是 mysqldump 导出，已包含建表语句，直接导入即可。

## 4. 域名与 HTTPS（备案通过后）

1. 腾讯云 DNS → 域名解析 → 添加 `A` 记录 `api`（或 `@`）指向公网 IP
2. 等解析生效（`ping api.你的域名.com` 能看到 IP）
3. 服务器上：

```bash
certbot --nginx -d api.你的域名.com
```

4. 微信公众平台 → 开发管理 → 开发设置 → **服务器域名** → request 合法域名填 `https://api.你的域名.com`
5. 小程序 `miniprogram/config/index.js` 的 `trial`/`release` 改成该域名

> 备案没下来之前：可以用 `http://<IP>` 直接测接口，但**体验版必须 HTTPS + 备案域名**，朋友打不开 IP。

## 5. 上传体验版

1. 公众平台 → 开发管理 → 开发设置 → 小程序代码上传 → **生成新的上传密钥**
2. 开发者工具 → 上传（版本 `1.0.0`）
3. 公众平台 → 版本管理 → 设为体验版
4. 成员管理 → 体验成员 → 添加朋友微信号

## 6. 日常运维

```bash
systemctl status wordrecall      # 运行状态
journalctl -u wordrecall -n 100  # 最近日志
tail -f /opt/wordrecall/logs/app.log
systemctl restart wordrecall
```

更新版本：本机 `bash scripts/deploy/push.sh` 即可（会重启）。

备份（建议加个每日 cron）：

```bash
mysqldump wordapp > /root/backup/wordapp-$(date +%F).sql
```

## 7. 常见问题

| 现象 | 处理 |
|---|---|
| 502 Bad Gateway | 后端没起来：`journalctl -u wordrecall -n 50` |
| 小程序报「不在以下 request 合法域名列表中」 | 公众平台没配域名，或配的是 http |
| 音频 404 | 音频没传全：`ls /opt/wordrecall/data/audio/us | wc -l` 应为 14296 |
| 端口被墙/访问不了 | 轻量防火墙没放行 80/443 |
| MySQL 连不上 | 确认 `systemctl status mysql`，账号是 `wordapp@127.0.0.1` |
