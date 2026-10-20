#!/usr/bin/env bash
# WordRecall 后端服务器初始化（Ubuntu 22.04 / 24.04，root 执行）
# 用法：bash server-init.sh
set -euo pipefail

APP_DIR=/opt/wordrecall
ENV_FILE=/etc/wordrecall.env
DB_NAME=wordapp

echo "==> 1/7 时区与基础包"
timedatectl set-timezone Asia/Shanghai || true
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y --no-install-recommends \
  openjdk-17-jre-headless nginx mysql-server rsync curl ca-certificates

echo "==> 2/7 目录与服务用户"
id -u wordrecall >/dev/null 2>&1 || useradd --system --home "$APP_DIR" --shell /usr/sbin/nologin wordrecall
mkdir -p "$APP_DIR/app" "$APP_DIR/data/audio" "$APP_DIR/logs"
chown -R wordrecall:wordrecall "$APP_DIR"

echo "==> 3/7 MySQL：建库、建应用账号（只读写，不给 DDL）"
if [ -z "${DB_PASSWORD:-}" ]; then
  read -r -s -p "给数据库账号 wordapp 设置密码（不会回显）: " DB_PASSWORD
  echo
fi
mysql <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'wordapp'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER 'wordapp'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON ${DB_NAME}.* TO 'wordapp'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
echo "    数据库账号已创建（wordapp@127.0.0.1）"

echo "==> 4/7 生成环境变量文件"
if [ ! -f "$ENV_FILE" ]; then
  JWT_SECRET=$(head -c 48 /dev/urandom | base64 | tr -d '\n/+=' | cut -c1-48)
  read -r -p "小程序 AppID（wx 开头）: " WX_APPID
  read -r -s -p "小程序 AppSecret（不会回显）: " WX_SECRET
  echo
  cat > "$ENV_FILE" <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=wordapp
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
WX_MINIAPP_APPID=${WX_APPID}
WX_MINIAPP_SECRET=${WX_SECRET}
APP_AUDIO_DIR=${APP_DIR}/data/audio
SERVER_PORT=8080
EOF
  chmod 600 "$ENV_FILE"
  echo "    已写入 $ENV_FILE（权限 600）"
else
  echo "    $ENV_FILE 已存在，跳过（如需改请手动编辑）"
fi

echo "==> 5/7 systemd 服务"
cat > /etc/systemd/system/wordrecall.service <<'EOF'
[Unit]
Description=WordRecall backend
After=network-online.target mysql.service
Wants=network-online.target

[Service]
User=wordrecall
WorkingDirectory=/opt/wordrecall/app
EnvironmentFile=/etc/wordrecall.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -XX:MaxRAMPercentage=70 -jar /opt/wordrecall/app/backend.jar
Restart=always
RestartSec=5
SuccessExitStatus=143
StandardOutput=append:/opt/wordrecall/logs/app.log
StandardError=append:/opt/wordrecall/logs/app.log

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable wordrecall.service

echo "==> 6/7 Nginx 反代（80 端口）"
cat > /etc/nginx/sites-available/wordrecall <<'EOF'
server {
    listen 80;
    listen [::]:80;
    server_name _;

    client_max_body_size 6m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
EOF
ln -sf /etc/nginx/sites-available/wordrecall /etc/nginx/sites-enabled/wordrecall
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

echo "==> 7/7 内存小于 3G 时加 2G swap"
MEM_MB=$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)
if [ "$MEM_MB" -lt 3072 ] && [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "    已添加 2G swap"
fi

echo
echo "初始化完成。接下来："
echo "  1) 本机执行 scripts/deploy/push.sh 上传 jar 与音频"
echo "  2) 在服务器导入数据库：mysql wordapp < wordapp.sql"
echo "  3) systemctl restart wordrecall && curl http://127.0.0.1:8080/api/health"
echo "  4) 域名解析后执行：certbot --nginx -d 你的域名"
