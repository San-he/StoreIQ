# StoreIQ 部署指南（阿里云学生机 + Docker Compose）

> 目标：一台云服务器跑通 **MySQL + Spring Boot 后端 + Nginx 前端**，任何时间公网可访问。
> 前端与后端**同源部署**（nginx 反代），无需处理跨域、无 HTTPS 混合内容问题。

```
浏览器 ──HTTP:80──▶ Nginx(web容器)
                     ├── /            → 前端静态页面（Vue 构建产物）
                     └── /stores/**   → 反代 → 后端(Spring Boot:8002) → MySQL
```

## 一、领取学生服务器（阿里云）

1. 阿里云「高校计划 / 学生专区」完成**学信网认证**，领取免费 ECS；
2. **推荐规格**：经济型 e（2 核 2 GiB）· 系统盘 40G · Ubuntu 22.04；
3. ⚠️ **到期提醒**：免费/优惠额度通常 12 个月，到期转原价续费——领到后立刻设日历提醒；
4. **带宽建议**：公网带宽选「**按使用流量计费**」（带宽上限可到 100Mbps）。演示流量很小，费用约几毛钱/月，但首屏加载速度天差地别（固定 1Mbps 时前端包要加载 30~60 秒）；
5. **安全组**：控制台放行 **22**（SSH）和 **80**（HTTP）。**3306（MySQL）、8002（后端）不需要放行**——它们只在 Docker 内部网络通信。

## 二、服务器初始化

SSH 登录后执行：

```bash
# 1. 安装 Docker + Compose（官方一键脚本）
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER   # 重新登录后免 sudo 用 docker

# 2. 配置阿里云镜像加速器（关键：否则 1M 带宽拉镜像要 1 小时+）
#    阿里云控制台 → 容器镜像服务 ACR → 镜像工具 → 镜像加速器 → 复制你的专属地址
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": ["https://<你的加速器地址>"]
}
EOF
sudo systemctl daemon-reload && sudo systemctl restart docker

# 3. 拉取代码（GitHub 与 Gitee 双平台同步，任选其一）
git clone https://github.com/San-he/StoreIQ.git && cd StoreIQ
```

## 三、一键部署

```bash
# 1. 环境变量（.env 已被 gitignore，不会进仓库）
cp .env.example .env && vim .env
#   DB_PASSWORD=你的MySQL强密码
#   JWT_SECRET=64位随机字符串（生成：openssl rand -hex 32）

# 2. 构建并启动（首次构建约 10~20 分钟：Maven/npm 依赖下载 + 镜像构建，之后更新秒级）
docker compose up -d --build

# 3. 检查状态
docker compose ps            # 三个容器均为 running/healthy
docker compose logs backend  # 看到 "Started StoreIQApplication" 即成功
```

浏览器访问 `http://服务器IP` 即打开系统。**上线后第一件事：登录 superadmin 修改默认密码！**

## 四、更新部署

```bash
git pull
docker compose up -d --build   # 只重建有变更的服务，数据库数据不受影响
```

## 五、HTTPS（可选，需域名 + ICP 备案）

国内服务器绑域名需**备案（约 1~3 周）**。备案通过后：

```bash
# 1. 域名 A 记录 → 服务器 IP
# 2. certbot 签发证书（Let's Encrypt 免费）
sudo apt install -y certbot
sudo certbot certonly --standalone -d your-domain.com

# 3. docker-compose.yml 的 web 服务增加：
#    volumes:
#      - /etc/letsencrypt:/etc/letsencrypt:ro
#      - ./nginx-https.conf:/etc/nginx/conf.d/default.conf:ro
#    ports: - "80:80" - "443:443"
#    （nginx-https.conf = nginx.conf 增加 443 server + 证书路径 + 80 跳转 443）
# 4. 安全组放行 443
```

## 六、本地验证（可选）

开发机装了 Docker 的话，也可以先本地跑一遍（端口注意）：

```bash
cp .env.example .env   # 填任意测试值
# 若本机 80 被占用：把 compose 中 web 的 "80:80" 改为 "8080:80"
docker compose up -d --build
# 访问 http://localhost:8080
```

## 七、常见问题

- **内存不足（OOM）**：compose 已内置调优（JVM 512m / MySQL 关 performance_schema）。若仍 OOM，把 `-Xmx512m` 降到 384m；
- **首次构建太慢**：Maven 依赖建议启用阿里云镜像源（在 backend Dockerfile 构建阶段加 `settings.xml`）；npm 可加 `--registry=https://registry.npmmirror.com`；
- **磁盘占用增长**：`docker system prune -f` 清理构建缓存；
- **改环境变量不生效**：改 `.env` 后需 `docker compose up -d` 重建容器。
