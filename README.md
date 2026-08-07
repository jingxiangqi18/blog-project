# Blog Project

这是一个前后端分离的单仓库项目：

- `frontend/`：Vue 3、Vite、Vue Router、Axios，使用 npm。
- `backend/`：Java 17、Spring Boot 4.0.6、Maven Wrapper、JdbcTemplate、MySQL Connector/J。
- `backend/database/schema.sql`：MySQL/TiDB Cloud 的一次性初始化脚本。

推荐的免费部署结构是 GitHub Pages（前端）+ Render Docker Web Service（后端）+ TiDB Cloud Starter（数据库）。

## 本地开发

### 后端

默认连接 `localhost:3306/blog_api`，默认端口为 `8080`。第一次使用时，先创建数据库并执行：

```bash
mysql -u root -p blog_api < backend/database/schema.sql
```

然后启动后端：

```bash
cd backend
./mvnw spring-boot:run
```

如果本地数据库不是无密码的 `root` 用户，请在当前终端设置环境变量，不要修改并提交密码：

```bash
export DB_URL='jdbc:mysql://localhost:3306/blog_api?serverTimezone=Asia/Shanghai&characterEncoding=utf8'
export DB_USERNAME='你的本地数据库用户名'
export DB_PASSWORD='你的本地数据库密码'
export JWT_SECRET="$(openssl rand -base64 32)"
./mvnw spring-boot:run
```

健康检查地址为 `http://localhost:8080/api/health`。

### 前端

`frontend/.env.development` 已把本地 API 指向 `http://localhost:8080`，无需修改业务代码：

```bash
cd frontend
npm ci
npm run dev
```

当前前端开发端口为 `5174`。路由使用 Hash 模式，因此线上地址类似 `/#/articles`，刷新不会要求 GitHub Pages 提供 SPA 服务端回退。

## 环境变量

| 名称 | 使用位置 | 是否敏感 | 说明 |
| --- | --- | --- | --- |
| `DB_URL` | 后端 | 是 | 完整 JDBC URL；TiDB Cloud 必须使用 TLS |
| `DB_USERNAME` | 后端 | 是 | 云数据库用户名 |
| `DB_PASSWORD` | 后端 | 是 | 云数据库密码 |
| `JWT_SECRET` | 后端 | 是 | Base64 编码且至少 32 字节的随机密钥 |
| `CORS_ALLOWED_ORIGIN` | 后端 | 否 | 允许的前端来源，多个来源用逗号分隔且不要带路径末尾 `/` |
| `JWT_EXPIRATION_MS` | 后端 | 否 | 可选，默认 `86400000`（一天） |
| `PORT` | 后端 | 否 | Render 自动提供，本地默认 `8080` |
| `VITE_API_BASE_URL` | 前端构建 | 否 | Render 服务地址，不带 `/api` 和末尾 `/` |
| `VITE_BASE_PATH` | 前端构建 | 否 | 可选；项目站点是 `/仓库名/`，自定义域名是 `/` |

仓库中的 JWT 默认值明确只用于本地开发，不是生产密钥。Render 部署时必须设置独立的 `JWT_SECRET`。

## Docker 本地验证

在仓库根目录构建：

```bash
docker build -f backend/Dockerfile -t blog-api ./backend
```

Linux 上让容器连接宿主机 MySQL 的示例：

```bash
docker run --rm -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e DB_URL='jdbc:mysql://host.docker.internal:3306/blog_api?serverTimezone=Asia/Shanghai&characterEncoding=utf8' \
  -e DB_USERNAME='你的本地数据库用户名' \
  -e DB_PASSWORD='你的本地数据库密码' \
  -e JWT_SECRET="$(openssl rand -base64 32)" \
  -e CORS_ALLOWED_ORIGIN='http://localhost:5174' \
  blog-api
```

不要把上述真实值写进 Dockerfile、`render.yaml` 或 Git 仓库。

## 在线部署

部署存在前后端地址互相依赖，推荐顺序如下：

1. 创建 TiDB Cloud Starter 集群并初始化数据库。
2. 在 Render 创建后端服务，先用本地前端来源配置 CORS，得到 `onrender.com` 地址。
3. 把 Render 地址配置为 GitHub Repository Variable，部署 GitHub Pages。
4. 得到 Pages 正式地址后，把它更新到 Render 的 `CORS_ALLOWED_ORIGIN`。
5. 重新部署或重启 Render 服务并完成验证。

### 1. TiDB Cloud Starter

1. 在 TiDB Cloud 创建免费的 Starter 集群和数据库。
2. 在集群的 Connect 页面生成数据库用户/密码，并按页面提示允许 Render 发起公网连接。若平台只支持 IP Access List，按 TiDB 和 Render 当前控制台提示配置，不要关闭 TLS 校验。
3. 使用 TiDB Cloud SQL Editor 或 MySQL 客户端，对目标数据库手动执行 `backend/database/schema.sql`。脚本只使用 MySQL/TiDB 兼容的 `BIGINT AUTO_INCREMENT`、`DATETIME`、索引、外键和 `utf8mb4`，不会删除已有表。
4. 在 Render 中配置连接信息。JDBC URL 示例：

```text
jdbc:mysql://你的TiDB主机:4000/你的数据库名?sslMode=VERIFY_IDENTITY&serverTimezone=UTC&characterEncoding=utf8
```

TiDB Cloud Starter 要求安全连接。不要把 `sslMode` 改为 `DISABLED`，也不要在仓库中保存真实主机、用户名或密码。

### 2. Render 后端

仓库根目录的 `render.yaml` 已定义 Docker Runtime、`backend/Dockerfile`、`backend` 构建上下文、自动部署和 `/api/health` 健康检查。

Blueprint 方式：

1. 登录 Render，选择 **New → Blueprint**。
2. 连接此 GitHub 仓库，Render 会读取根目录的 `render.yaml`。
3. 在首次创建时填写所有标记为 `sync: false` 的变量。
4. 确认服务 Plan 为 Free、Runtime 为 Docker、Health Check Path 为 `/api/health`。

也可以手动创建 **Web Service**，设置如下：

- Runtime：Docker
- Branch：仓库默认分支
- Dockerfile Path：`./backend/Dockerfile`
- Docker Build Context Directory：`./backend`
- Health Check Path：`/api/health`
- Auto Deploy：开启

Render 环境变量：

```text
DB_URL=jdbc:mysql://你的TiDB主机:4000/你的数据库名?sslMode=VERIFY_IDENTITY&serverTimezone=UTC&characterEncoding=utf8
DB_USERNAME=占位：TiDB用户名
DB_PASSWORD=占位：TiDB密码
JWT_SECRET=占位：openssl rand -base64 32 的输出
CORS_ALLOWED_ORIGIN=http://localhost:5174
JWT_EXPIRATION_MS=86400000
```

`PORT` 由 Render 自动注入，不需要手工创建。服务上线并得到 Pages 地址后，将 CORS 更新为：

```text
CORS_ALLOWED_ORIGIN=http://localhost:5174,https://你的GitHub用户名.github.io
```

Origin 只包含协议、域名和可选端口；即使 Pages 部署在 `/仓库名/`，这里也不能填写该路径。

### 3. GitHub Pages 前端

1. 打开 **GitHub Repository → Settings → Pages**。
2. 在 **Build and deployment → Source** 选择 **GitHub Actions**。
3. 打开 **Settings → Secrets and variables → Actions → Variables**，创建：

```text
VITE_API_BASE_URL=https://你的Render服务名.onrender.com
```

`VITE_API_BASE_URL` 没有设置时，工作流会明确失败，防止发布一个仍指向本机的站点。

`VITE_BASE_PATH` 可以不创建，工作流会根据 `${owner}/${repository}` 自动使用 `/仓库名/`。只有以下情况需要显式创建：

- 普通项目 Pages：`VITE_BASE_PATH=/仓库名/`
- 用户主页仓库或自定义域名：`VITE_BASE_PATH=/`

工作流 `.github/workflows/deploy-frontend.yml` 会在 `main` 或 `master` 分支的前端文件变化时运行，也支持在 Actions 页面手动运行。它使用 npm lock 文件安装依赖，并通过 GitHub 官方 Pages Actions 发布 `frontend/dist`。

## 部署验证清单

- GitHub Pages 首页能打开，URL 中可进入 `/#/articles`。
- favicon、CSS、JavaScript 等静态资源没有 404。
- `https://你的后端.onrender.com/api/health` 返回 `{"status":"ok"}`。
- 前端可以注册、登录，后续请求携带 Bearer JWT。
- 文章和评论的新增、查询、修改、删除正常。
- 在文章详情等路由刷新页面不会出现 GitHub Pages 404。
- 浏览器控制台没有 CORS 错误。
- Render 日志没有数据库连接或 TLS 错误。
- Render 重启或重新部署后，TiDB 中的数据仍然存在。

## 安全说明

- `.env`、`.env.local`、`.env.*.local` 和生产专用 Spring 配置已被忽略；示例配置可提交。
- `application.properties` 只包含无秘密的本地默认值和环境变量占位符。
- 不要把 Render 或 TiDB 控制台中的真实凭据复制到 README、Issue、提交记录或前端变量中。
- 若你确认某个敏感文件过去曾被 Git 跟踪，先轮换对应密码/密钥，再使用 `git filter-repo` 清理历史；本次改造不会自动执行历史重写、commit 或 push。
