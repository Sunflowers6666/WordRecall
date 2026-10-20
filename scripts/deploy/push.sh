#!/usr/bin/env bash
# 本机执行：构建 jar 并把 jar + 音频 + SQL 传到服务器
# 用法：SERVER_IP=1.2.3.4 bash scripts/deploy/push.sh
set -euo pipefail

: "${SERVER_IP:?请设置 SERVER_IP，例如 SERVER_IP=1.2.3.4 bash scripts/deploy/push.sh}"
SSH_USER=${SSH_USER:-root}
REMOTE=/opt/wordrecall

cd "$(dirname "$0")/../.."

echo "==> 构建 jar"
cd backend
/e/Develop/Maven/apache-maven-3.10.0/bin/mvn.cmd -B -q package -DskipTests
cd ..

JAR=$(ls backend/target/wordrecall-backend-*.jar | head -1)
echo "==> 上传 $JAR"
scp "$JAR" "$SSH_USER@$SERVER_IP:$REMOTE/app/backend.jar"

echo "==> 上传音频（14296 个，约 138MB，首次较慢）"
rsync -az --info=progress2 backend/data/audio/ "$SSH_USER@$SERVER_IP:$REMOTE/data/audio/"

echo "==> 上传数据库 dump"
scp /e/Develop/_downloads/wordapp.sql "$SSH_USER@$SERVER_IP:/root/wordapp.sql"

echo "==> 远程修正权限并重启服务"
ssh "$SSH_USER@$SERVER_IP" "chown -R wordrecall:wordrecall $REMOTE && systemctl restart wordrecall && sleep 3 && curl -s -o /dev/null -w 'health=%{http_code}\n' http://127.0.0.1:8080/api/health"
