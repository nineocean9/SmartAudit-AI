#!/usr/bin/env bash
# =============================================================================
# RuoYi-Vue Docker 部署脚本
# 用法: bash deploy.sh {up|down|restart|logs|status|build}
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC}  $(date '+%H:%M:%S') $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $(date '+%H:%M:%S') $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $(date '+%H:%M:%S') $*"; exit 1; }

check_docker() {
  if ! command -v docker &>/dev/null; then
    err "Docker 未安装，请先运行 server-setup.sh 或手动安装 Docker"
  fi
  if ! docker compose version &>/dev/null; then
    err "Docker Compose v2 未安装，请先安装 Docker Compose"
  fi
}

check_env() {
  if [ ! -f ".env" ]; then
    warn ".env 文件不存在，正在从 .env.example 复制..."
    cp .env.example .env
    warn "请编辑 .env 文件，填入真实的密码和密钥后重新运行！"
    exit 1
  fi
}

wait_healthy() {
  local service="$1"
  local max_wait="${2:-120}"
  local waited=0
  log "等待 ${service} 健康检查通过 (最多 ${max_wait}s)..."
  while [ $waited -lt $max_wait ]; do
    local status
    status=$(docker compose ps --format json 2>/dev/null | grep "\"Name\":\"ruoyi-${service}\"" | grep -o '"Health":"[^"]*"' | cut -d'"' -f4 || true)
    if [ "$status" = "healthy" ]; then
      log "${service} 已就绪 ✓"
      return 0
    fi
    sleep 2
    waited=$((waited + 2))
  done
  warn "${service} 健康检查超时，请检查日志: docker compose logs ${service}"
  return 1
}

# ---- 主逻辑 ----

check_docker

case "${1:-up}" in
  up|start)
    check_env
    log "正在构建镜像..."
    docker compose build --no-cache
    log "正在启动所有服务..."
    docker compose up -d
    log "等待服务就绪..."
    sleep 5
    wait_healthy "postgres" 120
    wait_healthy "redis" 60
    log "所有服务已启动:"
    docker compose ps
    echo ""
    log "访问地址: http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo '服务器IP')"
    ;;

  down|stop)
    log "正在停止所有服务..."
    docker compose down
    log "服务已停止"
    ;;

  restart)
    check_env
    log "正在重启所有服务..."
    docker compose restart
    log "服务已重启"
    ;;

  logs)
    shift
    docker compose logs -f "${@:---tail=100}"
    ;;

  status|ps)
    docker compose ps
    ;;

  build)
    log "仅构建镜像（不启动）..."
    docker compose build --no-cache
    log "镜像构建完成"
    ;;

  clean)
    warn "此操作将删除所有数据卷（数据库、Redis、上传文件）！"
    read -rp "确认删除？输入 yes 继续: " confirm
    if [ "$confirm" = "yes" ]; then
      docker compose down -v
      log "所有服务和数据卷已删除"
    else
      log "已取消"
    fi
    ;;

  *)
    echo "用法: bash deploy.sh {up|down|restart|logs|status|build|clean}"
    echo ""
    echo "  up/start  - 构建镜像并启动所有服务"
    echo "  down/stop - 停止所有服务"
    echo "  restart   - 重启所有服务"
    echo "  logs      - 查看日志 (可指定服务名: bash deploy.sh logs backend)"
    echo "  status    - 查看服务状态"
    echo "  build     - 仅构建镜像"
    echo "  clean     - 停止服务并删除所有数据 (危险!)"
    exit 1
    ;;
esac
