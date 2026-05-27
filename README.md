# My AI Agent

My AI Agent 是一个基于 Spring Boot、Spring AI Alibaba 和 Vue 3 构建的 AI 智能体应用。项目包含后端智能体服务、前端聊天界面，以及一个可选的图片搜索 MCP 服务，主要用于演示和实践 AI 对话、SSE 流式输出、工具调用、RAG 知识库和智能体任务执行能力。

## 功能特性

- AI 恋爱大师：面向情感沟通、聊天回复和关系场景分析的对话应用。
- AI 超级智能体：基于 ReAct / Tool Calling 思路执行复杂任务，支持流式输出执行过程。
- SSE 实时响应：前后端通过 Server-Sent Events 实现模型回复的逐步展示。
- 工具调用能力：内置文件操作、网页搜索、网页抓取、资源下载、PDF 生成、人类询问和任务终止等工具。
- RAG 知识库：内置恋爱场景知识文档，支持结合向量检索进行增强回答。
- MCP 扩展：包含 `image-search-mcp-server` 子项目，可作为 MCP Server 扩展图片搜索能力。
- API 文档：集成 Knife4j / OpenAPI，便于调试后端接口。

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5.13
- Spring AI / Spring AI Alibaba
- DashScope 通义千问模型
- MyBatis-Plus
- MySQL / PostgreSQL PGVector
- Knife4j OpenAPI
- Maven

### 前端

- Vue 3
- Vite 6
- Vue Router
- Axios
- lucide-vue-next

## 项目结构

```text
.
├── src/main/java/com/lcl/myaiagent
│   ├── advisors/          # 自定义 ChatClient Advisor
│   ├── agent/             # 智能体基类、ReAct Agent、ToolCall Agent、MyManus
│   ├── app/               # AI 应用核心逻辑，例如 LoveApp
│   ├── chatmemory/        # 对话记忆实现
│   ├── config/            # CORS、工具注册、MyBatis 配置
│   ├── controller/        # REST / SSE 接口
│   ├── domain/            # 数据库实体
│   ├── mapper/            # MyBatis Mapper
│   ├── rag/               # RAG 文档读取、向量库配置和 Advisor
│   ├── service/           # 业务服务
│   ├── tools/             # AI 可调用工具
│   └── utils/             # 通用工具类
├── src/main/resources
│   ├── document/          # 恋爱知识库 Markdown 文档
│   ├── mapper/            # MyBatis XML
│   ├── application.yaml   # 主配置
│   └── mcp-servers.json   # MCP stdio server 配置示例
├── my-ai-agent-frontend   # Vue 3 前端项目
└── image-search-mcp-server    # 图片搜索 MCP Server 子项目
```

## 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+，建议 20+
- MySQL 8+
- 可选：PostgreSQL + pgvector，用于 PGVector 向量存储
- DashScope API Key
- 可选：SearchAPI Key，用于网页搜索工具

## 后端配置

主配置文件位于 `src/main/resources/application.yaml`，默认服务地址为：

```text
http://localhost:8123/api
```

建议通过环境变量或本地配置文件提供密钥：

```powershell
$env:AI_DASHSCOPE_API_KEY="你的 DashScope API Key"
```

如果需要使用网页搜索工具，还需要配置：

```yaml
search-api:
  api-key: 你的 SearchAPI Key
```

默认数据库配置使用 MySQL：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/my-ai-agent
    username: root
    password: 123456
```

根据本地环境修改数据库地址、用户名和密码。项目当前没有独立的 SQL 初始化脚本，如需持久化聊天记录，可根据 `ChatMessage` 实体创建 `chat_message` 表。

## 启动后端

在项目根目录执行：

```powershell
.\mvnw.cmd spring-boot:run
```

启动成功后可以访问：

- 健康检查：`http://localhost:8123/api/health`
- Knife4j 文档：`http://localhost:8123/api/doc.html`
- OpenAPI JSON：`http://localhost:8123/api/v3/api-docs`

## 启动前端

进入前端目录：

```powershell
cd my-ai-agent-frontend
npm install
npm run dev
```

前端默认通过 Vite 代理访问后端：

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8123',
      changeOrigin: true,
    },
  },
}
```

常用页面：

- 首页：`http://localhost:5173/`
- AI 恋爱大师：`http://localhost:5173/love`
- AI 超级智能体：`http://localhost:5173/manus`

如果前后端分离部署，可以通过 `VITE_API_BASE_URL` 指定后端地址：

```powershell
$env:VITE_API_BASE_URL="http://localhost:8123/api"
npm run dev
```

## 主要接口

所有后端接口默认带有 `/api` 上下文路径。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/health` | 健康检查 |
| GET | `/ai/love_app/chat/sync` | AI 恋爱大师同步对话 |
| GET | `/ai/love_app/chat/sse` | AI 恋爱大师 SSE 流式对话 |
| GET | `/ai/love_app/chat/server_sent_event` | 返回 `ServerSentEvent` 格式的流式对话 |
| GET | `/ai/love_app/chat/emitter` | 基于 `SseEmitter` 的流式对话 |
| GET | `/ai/manus/chat` | MyManus 超级智能体流式对话 |

示例：

```text
GET http://localhost:8123/api/ai/love_app/chat/sync?message=你好&chatId=test-001
```

```text
GET http://localhost:8123/api/ai/manus/chat?message=帮我生成一份学习计划
```

## MCP 图片搜索服务

项目包含 `image-search-mcp-server` 子模块，可打包为 MCP Server：

```powershell
cd image-search-mcp-server
.\mvnw.cmd package
```

主项目的 `src/main/resources/mcp-servers.json` 中已经给出 stdio 启动示例：

```json
{
  "mcpServers": {
    "image-search-mcp-server": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar",
        "image-search-mcp-server/target/image-search-mcp-server-0.0.1-SNAPSHOT.jar"
      ],
      "env": {}
    }
  }
}
```

如需启用 MCP Client，可参考 `application.yaml` 中已注释的 `spring.ai.mcp.client` 配置。

## 测试

后端测试：

```powershell
.\mvnw.cmd test
```

前端构建检查：

```powershell
cd my-ai-agent-frontend
npm run build
```

## 注意事项

- 不要将真实 API Key、数据库密码等敏感信息提交到仓库。
- SSE 接口需要后端服务保持运行，前端页面如果提示连接中断，先检查 `http://localhost:8123/api/health`。
- RAG / PGVector 相关能力需要正确配置向量库依赖和数据库环境。
- 前端 `dist/`、后端 `target/`、`node_modules/` 等构建产物不建议提交。

