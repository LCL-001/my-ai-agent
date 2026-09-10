# My AI Agent

基于 **Spring Boot 3.5 / Spring AI Alibaba / Vue 3** 构建的双智能体对话应用：

- **MyManus**：通用任务智能体，ReAct / 工具调用循环，多轮会话 + SSE 流式推送执行过程，最大 20 步。
- **AI 恋爱大师**：单一领域的情感场景对话应用，同步、Flux SSE、`ServerSentEvent`、`SseEmitter` 四种输出形态并存。

两个智能体共用一套会话体系（登录、会话列表、历史消息、AI 自动生成标题），前端只保留新对话、会话历史、登录和两个智能体入口。

> 这个仓库经历了一次明显的**产品收敛**：私有知识库、JD 差距分析、学习计划、模拟面试四块功能做完后整体移除了（见提交 `bab874c`，75 个文件、3349 行），并用一个测试断言这些路由不再存在，防止后续被误复活。原因与数据兼容性的取舍写在 `doc/18-dual-agent-product-scope.md`。

## 会话记忆：水位线增量摘要压缩

多步 Agent 每一步都要读一次会话历史。这里有两个问题叠加：

1. 官方的 `MessageWindowChatMemory` **按消息条数**裁剪，不感知 token。审查类任务里单条工具结果可能几千字，按条数裁挡不住上下文溢出；
2. 如果每次读取都把超限的历史**整段重新摘要**，一次 20 步的任务就会产生最多 20 次摘要模型调用，成本和延迟被步数放大。

`chatmemory/FlowWindowBasedChatMemory.java` 的做法是把「写」和「读」拆开：

| 层 | 行为 | 原则 |
|----|------|------|
| 写入 | 全量落库，绝不裁剪 | 摘要压缩才有原料，策略可随时调整 |
| 读取 | 按 token 预算（默认 4096）组装 | 只影响"送进模型的量" |

超预算时不是整段重压，而是**增量压缩**：摘要表（`chat_summary`）里存着一条 `last_message_id` 水位线，标记摘要已覆盖到哪条消息。读取时先做本地判断——

- 水位线之后没有新消息 → 直接复用摘要，**零模型调用**；
- 摘要 + 新消息仍在预算内 → 直接复用，**零模型调用**；
- 超预算且新消息足够多 → 把水位线后除最近 10 条之外的部分**增量合并**进已有摘要；
- 新消息不足保留条数却超预算（单条巨大）→ 降级为按 token 硬裁剪。

摘要的覆盖边界用数据库自增 id 标记而不是创建时间，因为秒级精度下同一秒多条消息的顺序会抖动，边界可能切错。

三层可靠性兜底：任何异常都降级为硬裁剪；模型返回空摘要视为失败并抛异常，**绝不把空摘要落库**（否则水位线会指向一段没有信息的摘要，之后所有读取都拿到空上下文）；以及上面提到的「新消息不足」分支。

单测锁住了这条路径——`compress_reuseSummary` 用 `verify(chatModel, never()).call(...)` 断言复用时模型一次都不被调用。

需要说明边界：**「零模型调用」的收益目前只有单元测试证据**。运行日志里能确认的是 6 次真实增量压缩（摘要从 386 字增长到 814 字），但没有记录到任何一次"复用命中"——要么那次会话每步都恰好超预算，要么是读取次数太少。复用分支的运行期命中率还没有埋点统计，这是当前最缺的一块证据。

## Agent 架构

四层继承链（`BaseAgent → ReActAgent → ToolCallAgent → MyManus`），自建状态机（`IDLE / RUNNING / FINISHED / ERROR`）驱动"思考 → 调用工具 → 观察结果 → 终止"循环：

- 关闭模型的内置工具执行（`internalToolExecutionEnabled=false`），改为手动调用 `ToolCallingManager` 并回写对话历史。只有掌握循环，才能做到每一步都推送事件，才能在模型开始重复自己时注入新策略提示。
- 终止路径共 7 条：无工具调用 / `doTerminate` / `askHuman` / 达到最大步数 / 循环检测命中 3 次 / 用户断开连接 / 异常置 ERROR。
- 循环检测：最后一条助手文本重复出现 2 次判定卡住，累计 3 次强制终止。定位消息用**引用比较**而不是 `equals`，避免内容相同但语义不同的消息被误判。
- `MyManus` 刻意**不注册成 Spring 单例**——它是有状态的（消息列表随步数增长），按请求新建实例避免跨请求串味。

> 关于"手动编排"的准确说法：关掉的是框架的**内置自动执行**开关，`ToolCallingManager` 仍是被手动调用的，并不是完全弃用它。

**踩过的一个框架级坑**：工具方法返回纯文本，我靠一个固定前缀判断"模型是否在请求人工介入"，但实际运行时这个判断永远不成立。排查后发现框架会把工具返回的字符串再按 JSON 序列化一次，文本前后加了引号、内部引号被转义。解决方法是使用工具结果前先按 JSON 解析取回原始字符串。收敛在一个方法里（`ToolCallAgent.normalizeToolResponseData`），后续新增工具不会再踩。

### 工具清单

全仓库声明 9 个 `@Tool` 方法，实际注册给 Agent 的是 **8 个**（见 `config/ToolRegistration`）：

| 工具 | 方法 | 文件 |
| --- | --- | --- |
| 读文件 | `readFile` | `tools/FileOperationTool.java` |
| 写文件 | `writeFile` | `tools/FileOperationTool.java` |
| 网页搜索 | `searchWeb` | `tools/WebSearchTool.java` |
| 网页抓取 | `scrapeWebPage` | `tools/WebScrapingTool.java` |
| 资源下载 | `downloadResource` | `tools/ResourceDownloadTool.java` |
| PDF 生成 | `generatePDF` | `tools/PDFGenerationTool.java` |
| 询问人类 | `askHuman` | `tools/AskHumanTool.java` |
| 主动终止 | `doTerminate` | `tools/TerminateTool.java` |

`TerminalOperationTool`（终端命令）也声明了 `@Tool`，但**没有注册**，Agent 不可达。README 早期版本宣传过这个工具，与实际不符——以此为准。

### SSE 事件

三种 JSON 事件 + 一个哨兵：

- `event=step`，`kind=think` —— 模型调用工具前的自然语言推理
- `event=step`，`kind=tool` —— 工具名与执行结果
- `event=answer` —— 面向用户的最终回答
- 裸字符串 `[DONE]` 表示流结束

前端（`components/ChatRoom.vue`）收到第一个 `step` 就自动展开折叠条，收到 `answer` 后折叠并显示"已执行 N 步（用时 X 秒）"。历史消息同样还原成这个结构：`ChatHistoryAssembler` 把落库的原始消息按轮次重组，把内部提示与工具步骤归组，有 7 个单元测试覆盖。

### 会话与用户

- Session 登录（Redis 共享，多实例无需改代码），密码 BCrypt 优先并兼容旧 MD5 加盐格式
- 查询历史消息有登录 + 归属双重校验（`ConversationController#getMessages`）
- 首条消息后异步生成不超过 18 字的会话标题，只在标题仍是默认值"新对话"时更新，不覆盖手动改名
- 文件下载接口做了双重防路径穿越：先取纯文件名，再校验规范化路径前缀

## 快速开始

### 1. 准备依赖

```powershell
# MySQL 8 必需；Redis 用于 Session 共享（可选）
docker compose up -d mysql redis
```

数据库表有两个来源：`create_sql/init.sql`（Docker 首启自动执行）和 `src/main/resources/db/migration`（Flyway）。**注意**：`chat_summary` 表目前只在 `init.sql` 里、不在 Flyway 迁移里，如果用 Flyway 建库需要手动补建，否则水位线功能会静默降级为硬裁剪。

### 2. 配置密钥

```powershell
$env:AI_DASHSCOPE_API_KEY = "你的 DashScope API Key"
# 网页搜索工具需要
$env:SEARCH_API_KEY = "你的 SearchAPI Key"
```

**不要把真实 Key 写进配置文件的默认值**——`application-ollama.yaml` 和 `application.yaml` 里这些默认值必须留空。

### 3. 启动后端

```powershell
.\mvnw.cmd spring-boot:run
```

默认配置指向本地 Ollama（`application.yaml` 的 `spring.profiles.active: ollama`），可通过环境变量换模型：

```powershell
$env:OLLAMA_CHAT_MODEL = "qwen3:4b"   # 需支持工具调用
```

要切回云端 DashScope，把 `application.yaml:5` 的默认 profile 改掉即可。

健康检查：`http://localhost:8123/api/health`；接口文档：`http://localhost:8123/api/doc.html`

### 4. 启动前端

```powershell
cd my-ai-agent-frontend
npm install
npm run dev
```

访问 `http://localhost:5173/chat`。`/love` 与 `/manus` 两个独立入口是早期遗留，功能上与会话内嵌智能体重复。

## 测试

19 个测试类、61 个活跃 `@Test`。按是否依赖外部环境分两类：

- **纯单元测试（约 47 个）**：Agent 状态机与循环检测、工具调用编排、记忆压缩四条分支、历史消息重组、会话标题生成。这些用 Mockito 桩住模型与仓储，**不需要任何外部环境**。
- **`@SpringBootTest`（约 14 个）**：依赖真实的 MySQL / Redis / Ollama / DashScope Key，无这些环境会失败。其中 `MyManusTest` 注入了一个刻意不注册成 Bean 的类，**当前是确定失败的坏测试**。

```powershell
.\mvnw.cmd test   # 注意：全量跑不完，只有纯单元测试部分可靠
```

## 已知短板

写在这里而不是等被问出来：

- **测试不自洽**：`@SpringBootTest` 一族依赖真实环境，没有 CI，也没有全量测试跑绿的可复现记录；还有一个确定性失败的坏测试。
- **水位线压缩的收益缺运行期度量**：复用分支只有单元测试覆盖，运行日志里没有命中记录（见上文会话记忆一节末尾）。
- **`chat_summary` 表不在 Flyway 迁移里**，只在 `create_sql/init.sql`。用迁移工具建库的新环境会缺表，水位线功能会**静默降级**为硬裁剪——功能看起来"没坏"，但其实没生效。
- **两个配置开关失效**：`VECTOR_ENABLED` 与 `LEGACY_LOVE_ENABLED` 在文档里被描述为可开关，但相关的 `@Configuration` 类已被注释掉，设置了环境变量也不会生效，恢复需要改代码。文档与实现不一致。
- **RAG 能力当前完全停用**：pgvector 相关依赖、PostgreSQL 驱动与三个向量配置类都被注释（本地化改造 `5130f1b`），不是"配置一下就能用"。
- **README 与代码曾有约十处不一致**，本版已修正；若发现新的不一致，以代码为准并提 issue。
- **文件读写工具没有做路径规范化校验**，相对路径可以穿越；相比之下文件下载接口做了双重防穿越，说明防护意识没有横向铺开。
- **死代码**：两个只有注解没有任何方法的空 Controller、一个没有任何调用点的文件版记忆实现、大段被注释的旧实现。
- **没有 CI**、没有链路追踪、没有模型调用的 token 与成本统计。
- **Docker 编排的库名与后端连接的库名不一致**：`create_sql/init.sql` 建的是 `my-ai-agent`，而 `application.yaml` 连接的是 `yu-ai-agent`，容器起来后表建在另一个库里，后端一查询就报错。
- **图片搜索 MCP**（`image-search-mcp-server` 子项目）是可独立运行的 MCP Server，但主应用侧的客户端配置整段是注释，没有接入主应用——所以准确的说法是"提供了一个可独立运行的 MCP Server 示例"，而不是"支持 MCP"。
- 数据库默认口令仍是弱口令（可用环境变量覆盖）。

## 项目结构

```text
.
├── src/main/java/com/lcl/myaiagent
│   ├── advisors/          # ChatClient Advisor
│   ├── agent/             # 四层继承链 + 状态机 + MyManus
│   ├── app/               # 恋爱大师核心逻辑（LoveApp）
│   ├── chatmemory/        # 对话记忆：全量落库 + 水位线增量压缩
│   ├── config/            # 工具注册、CORS、模型选项装配
│   ├── controller/        # REST / SSE 接口
│   ├── mapper/            # MyBatis-Plus Mapper
│   ├── model/             # po / vo / dto
│   ├── rag/               # 向量库与 RAG 配置（当前已停用）
│   ├── repository/        # CrudRepository
│   ├── service/           # 会话 / 消息 / 摘要 / 标题 / 用户
│   ├── tools/             # AI 可调用工具
│   └── utils/             # 历史消息重组、消息转换等
├── src/main/resources
│   ├── db/migration/      # Flyway 迁移
│   ├── application.yaml   # 主配置（默认 ollama profile）
│   └── application-ollama.yaml
├── create_sql/init.sql    # Docker 首启建表
├── my-ai-agent-frontend   # Vue 3 前端
├── doc/                   # 设计与运行说明（部分文档）
└── image-search-mcp-server # 图片搜索 MCP Server 子项目
```

## 免责声明

本项目为技术学习与演示项目，「恋爱大师」的输出不构成任何现实建议。
