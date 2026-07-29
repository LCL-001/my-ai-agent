# YuLin AI

YuLin AI 是一个基于 Spring Boot、Spring AI 和 Vue 3 的双智能体应用：

- **MyManus**：支持会话历史、SSE 流式回复与原有工具调用链的通用助手。
- **恋爱大师**：面向情感沟通、聊天回复和关系场景分析的对话助手。

会话会按用户保存，首次消息可由模型自动生成标题。前端支持新建时选择智能体、查看历史会话、修改标题与删除会话。

## 技术栈

- Java 21、Spring Boot 3.5、Spring AI / Spring AI Alibaba
- MyBatis-Plus、MySQL、Redis
- Vue 3、Vite、Pinia、Vue Router
- 可选：Ollama 本地模型、PostgreSQL + pgvector

## 本地模型切换

默认配置使用 DashScope。若要使用本地 Ollama，请先确保模型已下载并启动 Ollama 服务：

```powershell
ollama serve
ollama list
```

然后以 `ollama` Profile 启动后端：

```powershell
$env:SPRING_PROFILES_ACTIVE = "ollama"
$env:OLLAMA_CHAT_MODEL = "qwen3:4b"
.\mvnw.cmd spring-boot:run
```

可以仅修改 `OLLAMA_CHAT_MODEL` 来切换模型，例如：

```powershell
$env:OLLAMA_CHAT_MODEL = "deepseek-r1:7b"
```

更多参数见 `src/main/resources/application-ollama.yaml`，完整说明见 `doc/15-local-ollama-model-switching.md`。

## 数据库与 PgVector

MySQL 保存用户、会话和聊天消息。默认本地连接参数可以通过以下环境变量覆盖：

```powershell
$env:MYSQL_URL = "jdbc:mysql://localhost:3306/yu-ai-agent"
$env:MYSQL_USERNAME = "root"
$env:MYSQL_PASSWORD = "123456"
```

`docker-compose.yml` 预留了 pgvector 服务。启动基础设施：

```powershell
docker compose up -d mysql redis pgvector
```

需要让应用初始化通用 `VectorStore` Bean 时，设置：

```powershell
$env:VECTOR_ENABLED = "true"
$env:VECTOR_DB_URL = "jdbc:postgresql://localhost:5433/yu_ai_agent_vector"
```

这轮产品不提供资料上传或检索界面；PgVector 仅作为后续 RAG 扩展的基础设施。恋爱大师本地/云端 RAG 继续由现有 `LoveAppVectorStoreConfig` 与 `LEGACY_LOVE_ENABLED` 配置控制，不会被强制启用。

## 启动

后端：

```powershell
.\mvnw.cmd spring-boot:run
```

前端：

```powershell
cd my-ai-agent-frontend
npm install
npm run dev
```

服务默认地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8123/api`
- 健康检查：`http://localhost:8123/api/health`

## 主要接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/ai/manus/chat` | MyManus SSE 对话 |
| GET | `/ai/love_app/chat/sse` | 恋爱大师 SSE 对话 |
| GET | `/conversations` | 当前用户会话历史 |
| POST | `/conversations` | 新建会话 |
| PUT | `/conversations/{conversationId}/title` | 修改会话标题 |

## 验证

```powershell
$env:SPRING_PROFILES_ACTIVE = "ollama"
.\mvnw.cmd -Dtest=MyAiAgentApplicationTests,ProductScopeTest,ConversationTitleServiceTest test

cd my-ai-agent-frontend
npm run build
```

历史上的面试准备数据表和 Flyway 迁移被保留，以兼容已有数据库；设计取舍说明见 `doc/18-dual-agent-product-scope.md`。
