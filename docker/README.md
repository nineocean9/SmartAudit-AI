# RuoYi-Vue Docker 部署指南

## 服务器要求

| 项目 | 最低配置 | 推荐配置 |
|------|---------|----------|
| 操作系统 | Ubuntu 22.04 / CentOS 8 | Ubuntu 24.04 |
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 20 GB | 40 GB+ |
| Docker | 24.0+ | 27.0+ |
| Docker Compose | v2.20+ | v2.30+ |

## 快速部署

### 1. 服务器初始化

将 `server-setup.sh` 上传到服务器并执行（需要 root 权限）：

```bash
scp docker/server-setup.sh root@<服务器IP>:/root/
ssh root@<服务器IP>
bash server-setup.sh
```

此脚本会自动完成：
- 系统更新
- 安装 Docker Engine + Docker Compose v2
- 配置 UFW 防火墙（开放 22, 80, 443 端口）
- 设置时区为 Asia/Shanghai
- 配置 Docker 日志轮转（单文件最大 10MB，保留 3 个）

### 2. 拉取代码

```bash
git clone https://github.com/nineocean9/SmartAudit-AI.git /opt/ruoyi-audit
cd /opt/ruoyi-audit/docker
```

### 3. 配置环境变量

```bash
cp .env.example .env
chmod 600 .env
vim .env
```

需要配置的变量：

| 变量 | 说明 |
|------|------|
| `POSTGRES_PASSWORD` | 数据库密码（≥32字符强随机字符串） |
| `REDIS_PASSWORD` | Redis 密码（不同于数据库密码） |
| `TOKEN_SECRET` | JWT Token 密钥（≥64字符随机字符串） |
| `AI_MODEL_API_KEY` | AI 模型 API Key（如使用 Mimo） |
| `AI_EMBEDDING_API_KEY` | 向量嵌入 API Key（如使用 DashScope） |
| `HTTP_PORT` | HTTP 端口，默认 80 |

> **安全提示**：用 `openssl rand -hex 32` 生成强随机密码。

### 4. 启动服务

```bash
bash deploy.sh up
```

首次启动会自动：
1. 构建前后端 Docker 镜像
2. 启动 PostgreSQL（自动初始化 26 个 SQL 文件）
3. 启动 Redis
4. 启动 Spring Boot 后端
5. 启动 Nginx 前端

### 5. 验证部署

```bash
# 查看服务状态
docker compose ps

# 查看后端日志
docker compose logs -f backend

# 测试 HTTP 访问
curl http://localhost/
```

浏览器访问 `http://<服务器IP>/`，默认管理员账号：`admin / admin123`

---

## 常用运维命令

```bash
# 查看所有服务状态
bash deploy.sh status

# 查看日志
bash deploy.sh logs              # 所有服务
bash deploy.sh logs backend      # 仅后端
bash deploy.sh logs frontend     # 仅前端

# 重启服务
bash deploy.sh restart

# 停止服务（保留数据）
bash deploy.sh down

# 停止并删除所有数据（危险！）
bash deploy.sh clean
```

---

## 数据备份

### 备份数据库

```bash
docker compose exec -T postgres pg_dump -U postgres -d ry-vue | gzip > backup_$(date +%Y%m%d).sql.gz
```

### 恢复数据库

```bash
gunzip -c backup_20260717.sql.gz | docker compose exec -T postgres psql -U postgres -d ry-vue
```

### 备份上传文件

```bash
docker run --rm -v ruoyi-vue-v392_upload_data:/data -v $(pwd):/backup alpine tar czf /backup/upload_backup.tar.gz -C /data .
```

---

## 安全规范

### 文件上传安全

- Nginx 层限制：`client_max_body_size 50m`
- 应用层限制：单文件 10MB，总上传 20MB（`application.yml`）
- 上传文件通过后端 API 中转，不直接暴露存储路径
- 建议在生产环境增加文件类型白名单校验

### 网络安全

- 仅开放 22 (SSH)、80 (HTTP)、443 (HTTPS) 端口
- PostgreSQL (5432) 和 Redis (6379) 仅 Docker 内网访问，不对外暴露
- Nginx 已配置安全响应头（X-Content-Type-Options, X-Frame-Options, X-XSS-Protection）

### 密钥安全

- `.env` 文件已排除在版本控制之外（`.gitignore`）
- 服务器上 `.env` 权限设为 `600`（仅所有者可读写）
- 所有密码使用强随机字符串
- 生产环境建议关闭 Druid 监控面板或设置 IP 白名单

### HTTPS 配置（可选）

建议后续通过以下方式启用 HTTPS：
1. 云服务商负载均衡器 + SSL 证书（推荐）
2. Let's Encrypt + Certbot
3. 在 `compose.yaml` 中添加 Traefik/Caddy 反向代理容器

---

## 故障排查

### 数据库初始化失败

```bash
docker compose logs postgres | grep ERROR
```

如果数据库已存在旧数据导致初始化跳过，需要删除数据卷重新初始化：
```bash
docker compose down -v
docker compose up -d
```

### 后端无法连接数据库

```bash
docker compose logs backend | grep -i error
```

常见原因：
- `.env` 中 `POSTGRES_PASSWORD` 与 postgres 容器使用的密码不一致
- postgres 健康检查未通过

### 前端页面 404

```bash
docker compose logs frontend
```

检查 Nginx 是否正确代理 API 请求到后端。

### 端口被占用

```bash
# 查看端口占用
ss -tlnp | grep :80

# 修改 HTTP_PORT 为其他端口
echo "HTTP_PORT=8081" >> .env
bash deploy.sh up
```
