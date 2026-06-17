# VoyageAI

VoyageAI 是一个前后端分离的智能旅行规划系统。用户可以用自然语言生成旅行草稿，在地图上查看单日路线，并把确认后的行程保存到个人列表中。

## 功能特性

- 对话式生成旅行计划，支持基于当前草稿继续修改
- 景点知识库、热门景点和城市分布统计
- 行程草稿保存、详情查看和删除
- 高德地图 JS API 前端打点展示
- 高德 Web Service 后端补全 POI 信息和路线规划
- 大模型接口兼容 DeepSeek/OpenAI 风格的 `/chat/completions`

## 技术栈

- 前端：Vue 3、Vite、Pinia、Vue Router、Element Plus、Axios、高德地图 JSAPI
- 后端：Spring Boot 3、MyBatis-Plus、MySQL、Lombok、Maven
- 数据库：MySQL 8+

## 目录结构

```text
VoyageAI/
├── backend/              # Spring Boot 后端
├── frontend/             # Vue 3 + Vite 前端
├── mysql/init/           # 数据库建表与演示数据
├── .env.example          # 环境变量示例
└── README.md
```

## 环境变量

项目不会在仓库中保存真实数据库密码或 API Key。请复制 `.env.example` 或 `frontend/.env.example` 后在本地填写真实值。

后端常用变量：

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=travel_assistant
DB_USERNAME=root
DB_PASSWORD=your-db-password
AI_ENABLED=false
AI_BASE_URL=https://api.deepseek.com
AI_API_KEY=your-ai-api-key
AI_MODEL=deepseek-chat
AMAP_WEB_SERVICE_KEY=your-amap-web-service-key
```

前端常用变量：

```env
VITE_API_BASE_URL=/api
VITE_AMAP_JS_KEY=your-amap-js-key
```

## 本地运行

1. 初始化数据库

```bash
mysql -u root -p < mysql/init/01_schema.sql
mysql -u root -p travel_assistant < mysql/init/02_seed.sql
```

2. 启动后端

PowerShell 示例：

```powershell
cd backend
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="travel_assistant"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-db-password"
$env:AI_ENABLED="false"
$env:AI_API_KEY=""
$env:AMAP_WEB_SERVICE_KEY="your-amap-web-service-key"
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`。

3. 启动前端

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

前端默认监听 `http://localhost:5173`，开发代理会把 `/api` 转发到后端 `8080` 端口。

## Docker 构建

后端：

```bash
docker build -t voyageai-backend ./backend
docker run --rm -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=travel_assistant \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your-db-password \
  -e AI_ENABLED=false \
  -e AI_API_KEY= \
  -e AMAP_WEB_SERVICE_KEY=your-amap-web-service-key \
  voyageai-backend
```

前端：

```bash
docker build \
  --build-arg VITE_API_BASE_URL=/api \
  --build-arg VITE_AMAP_JS_KEY=your-amap-js-key \
  -t voyageai-frontend ./frontend
docker run --rm -p 80:80 voyageai-frontend
```

## 上传 GitHub 前检查

- 不要提交 `.env`、日志、`target/`、`dist/`、`node_modules/` 或打包产物
- 真实密钥只放在本地环境变量、服务器环境变量或 CI/CD Secret 中
- 如果密钥曾经进入过 Git 提交历史，请先吊销并重新生成，再发布仓库
- 演示 SQL 里的账号仅用于本地体验，请勿作为生产账号使用
