#!/usr/bin/env bash
# =============================================================================
# 云服务器初始化脚本
# 用途: 在全新云服务器上安装 Docker、配置防火墙、创建项目目录
# 用法: SSH 到服务器后执行 bash server-setup.sh
# =============================================================================
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ---- 检查 root 权限 ----
if [ "$(id -u)" -ne 0 ]; then
  err "请使用 root 用户或 sudo 运行此脚本"
fi

# =============================================================================
# 1. 系统更新
# =============================================================================
log "更新系统包..."
if command -v apt-get &>/dev/null; then
  apt-get update && apt-get upgrade -y
  PKG_MGR="apt"
elif command -v yum &>/dev/null; then
  yum update -y
  PKG_MGR="yum"
else
  err "不支持的系统，仅支持 Debian/Ubuntu (apt) 或 CentOS/RHEL (yum)"
fi

# =============================================================================
# 2. 安装 Docker Engine
# =============================================================================
log "安装 Docker..."
if command -v docker &>/dev/null; then
  log "Docker 已安装: $(docker --version)"
else
  curl -fsSL https://get.docker.com | bash
  log "Docker 安装完成: $(docker --version)"
fi

# 启动 Docker 并设置开机自启
systemctl enable docker
systemctl start docker

# =============================================================================
# 3. 安装 Docker Compose v2 (插件方式)
# =============================================================================
log "检查 Docker Compose..."
if docker compose version &>/dev/null; then
  log "Docker Compose 已安装: $(docker compose version)"
else
  log "安装 Docker Compose v2..."
  COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
  if [ "$(uname -m)" = "aarch64" ]; then
    COMPOSE_ARCH="aarch64"
  else
    COMPOSE_ARCH="x86_64"
  fi
  mkdir -p /usr/local/lib/docker/cli-plugins
  curl -SL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-${COMPOSE_ARCH}" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
  log "Docker Compose 安装完成: $(docker compose version)"
fi

# =============================================================================
# 4. 配置时区
# =============================================================================
log "设置时区为 Asia/Shanghai..."
if command -v timedatectl &>/dev/null; then
  timedatectl set-timezone Asia/Shanghai
fi
log "当前时间: $(date)"

# =============================================================================
# 5. 配置防火墙 (UFW / firewalld)
# =============================================================================
log "配置防火墙..."
if command -v ufw &>/dev/null; then
  # Ubuntu / Debian
  ufw --force reset
  ufw default deny incoming
  ufw default allow outgoing
  ufw allow 22/tcp comment 'SSH'
  ufw allow 80/tcp comment 'HTTP'
  ufw allow 443/tcp comment 'HTTPS'
  ufw --force enable
  log "UFW 防火墙已配置并启用"
  ufw status verbose
elif command -v firewall-cmd &>/dev/null; then
  # CentOS / RHEL
  systemctl start firewalld
  systemctl enable firewalld
  firewall-cmd --permanent --add-service=ssh
  firewall-cmd --permanent --add-service=http
  firewall-cmd --permanent --add-service=https
  firewall-cmd --reload
  log "firewalld 防火墙已配置并启用"
  firewall-cmd --list-all
else
  warn "未检测到防火墙工具，请手动配置安全组规则：开放 22, 80, 443 端口"
fi

# =============================================================================
# 6. 创建项目目录
# =============================================================================
APP_DIR="/opt/ruoyi-audit"
log "创建项目目录: ${APP_DIR}"
mkdir -p "${APP_DIR}"

# =============================================================================
# 7. Docker 优化配置
# =============================================================================
log "配置 Docker 日志轮转..."
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF
systemctl restart docker

# =============================================================================
# 完成
# =============================================================================
echo ""
echo "============================================="
log "服务器初始化完成!"
echo "============================================="
log "Docker 版本:    $(docker --version)"
log "Compose 版本:   $(docker compose version 2>/dev/null || echo '未安装')"
log "时区:           $(timedatectl 2>/dev/null | grep 'Time zone' | awk '{print $3}' || date +%Z)"
log "项目目录:       ${APP_DIR}"
echo ""
log "下一步:"
log "  1. git clone https://github.com/nineocean9/SmartAudit-AI.git ${APP_DIR}"
log "  2. cd ${APP_DIR}/docker"
log "  3. cp .env.example .env && chmod 600 .env"
log "  4. vim .env  # 编辑密码和密钥"
log "  5. bash deploy.sh up"
echo "============================================="
