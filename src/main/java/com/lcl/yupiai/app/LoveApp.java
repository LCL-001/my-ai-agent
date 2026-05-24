package com.lcl.yupiai.app;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.lcl.yupiai.advisors.MyLoggerAdvisor;
import com.lcl.yupiai.advisors.MyReReadingAdvisor;
import com.lcl.yupiai.chatmemory.DataBaseChatMemory;
import com.lcl.yupiai.chatmemory.FileBasedChatMemory;
import com.lcl.yupiai.rag.LoveAppRagCustomAdvisorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 恋爱应用核心类
 * 提供基于AI的恋爱咨询服务，支持普通对话、报告生成、本地知识库检索和云端知识库检索等多种交互模式
 */
@Component
@Slf4j
public class LoveApp {


    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT =
            "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
                    "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
                    "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
                    "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    /**
     * 构造恋爱应用实例
     * 初始化ChatClient，配置系统提示词、对话记忆和默认Advisor
     *
     * @param dashscopeChatModel DashScope聊天模型，用于生成AI响应
     * @param dataBaseChatMemory 基于数据库的对话记忆管理器，用于持久化聊天记录
     */
    public LoveApp(ChatModel dashscopeChatModel, DataBaseChatMemory dataBaseChatMemory) {
        // 初始化基于内存的对话记忆
        // 打印实际使用的模型配置
        log.info("DashScope ChatModel class: {}", dashscopeChatModel.getClass().getName());
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
//        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);


//        DashScopeChatOptions options = DashScopeChatOptions.builder()
//                .withModel("qwen-plus")           // 模型名称
//                .withTemperature(0.7)              // Temperature 参数
//                .withMaxToken(2000)                // 最大令牌数
//                .withTopP(0.9)                     // Top-P 采样
//                .build();
//
//        DashScopeApi dashScopeApi = DashScopeApi.builder()
//                .apiKey()
//                .build();
//        ChatModel chatModel = DashScopeChatModel.builder()
//                .dashScopeApi(dashScopeApi)
//                .defaultOptions(options)
//                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),// 添加基于内存的对话记忆
                        // 添加自定义的 Logger Advisor，开启日志，便于观察效果
                        new MyLoggerAdvisor()
//                        // 添加自定义的推理增强 Advisor
//                        ,new MyReReadingAdvisor()
                )
//                .defaultOptions(ChatOptions.builder().model("qwen-plus").build())
                .build();
    }

    /**
     * 执行流式聊天，返回响应式消息流
     * <p>
     * 该方法使用流式处理的方式与AI进行对话，支持实时返回响应内容。
     * 通过chatId维护会话上下文，实现多轮对话功能。
     * </p>
     *
     * @param message 用户发送的消息内容
     * @param chatId 会话ID，用于标识和追踪对话上下文
     * @return Flux<String> 响应式消息流，包含AI的流式响应内容
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 执行普通聊天对话
     * 根据用户消息和会话ID进行对话，自动管理对话上下文记忆
     *
     * @param message        用户输入的消息内容
     * @param conversationId 会话唯一标识，用于区分不同用户的对话上下文
     * @return String AI生成的回复内容
     */
    public String doChat(String message, String conversationId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 恋爱报告数据记录
     * 包含报告标题和内容，用于结构化返回恋爱建议报告
     */
    record LoveReport(String title, String content) {
    }

    /**
     * 执行带报告生成的聊天对话
     * 在普通对话基础上，每次交互后生成结构化的恋爱报告，包含标题和建议内容
     *
     * @param message        用户输入的消息内容
     * @param conversationId 会话唯一标识，用于区分不同用户的对话上下文
     * @return LoveReport 包含标题和内容的恋爱报告对象
     */
    public LoveReport doChatWithReport(String message, String conversationId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }


    @Resource
    private VectorStore loveAppVectorStore;

    /**
     * 执行基于本地知识库的RAG（检索增强生成）聊天对话
     * 结合本地向量知识库进行智能检索，提供更专业的恋爱建议
     *
     * @param message        用户输入的消息内容
     * @param conversationId 会话唯一标识，用于区分不同用户的对话上下文
     * @return String AI结合知识库生成的回复内容
     */
    // 启用本地知识库服务
    public String doChatWithRag(String message, String conversationId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 开启日志，便于观察效果
//                .advisors(new MyLoggerAdvisor())
                // 应用知识库问答
                .advisors(QuestionAnswerAdvisor.builder(loveAppVectorStore).build())// 添加基于本地知识库的问答
                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                        loveAppVectorStore, "已婚")
                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    @Resource
    private Advisor loveAppRagCloudAdvisor;

    /**
     * 执行基于云端知识库的RAG（检索增强生成）聊天对话
     * 使用云端增强检索服务获取更丰富的知识库资源，提供专业化的恋爱咨询
     *
     * @param message        用户输入的消息内容
     * @param conversationId 会话唯一标识，用于区分不同用户的对话上下文
     * @return String AI结合云端知识库生成的回复内容
     */
    // 启用云知识库服务
    public String doChatWithCloudRag(String message, String conversationId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用增强检索服务（云知识库服务）
                .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String conversationId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
//                .tools(allTools)
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String conversationId) {
        log.info("mcp name={}",toolCallbackProvider.getToolCallbacks().toString());
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}


//package com.lcl.yupiai.app;
//
//import com.alibaba.cloud.ai.graph.RunnableConfig;
//import com.alibaba.cloud.ai.graph.agent.ReactAgent;
//import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
//import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.messages.AssistantMessage;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.stereotype.Component;
//
//
//@Component
//@Slf4j
//public class LoveApp {
//    private final ReactAgent agent;
//    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
//            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
//            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
//            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";
//
//
//    public LoveApp(ChatModel dashScopeChatModel) {
//        // 创建 agent
//        agent = ReactAgent.builder()
//                .name("Love_agent")
//                .model(dashScopeChatModel)
//                .systemPrompt(SYSTEM_PROMPT)
//                .saver(new MemorySaver())
//                .build();
//    }
//    public String doChat(String message, RunnableConfig runnableConfig) throws GraphRunnerException {
//        AssistantMessage response = agent.call(message, runnableConfig);
//        String content = response.getText();
//        log.info("content: {}", content);
//        return content;
//    }
//
//}
