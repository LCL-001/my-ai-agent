package com.lcl.myaiagent.agent;

import com.lcl.myaiagent.advisors.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * MyManus智能体，继承自ToolCallAgent
 * <p>
 * 这是一个全能的AI助手组件，能够解决用户提出的任何任务。
 * 通过集成多种工具，可以高效地完成复杂的请求。
 * </p>
 */
@Component
public class MyManus extends ToolCallAgent {

    /**
     * 构造函数，初始化MyManus智能体的配置
     * <p>
     * 该方法执行以下初始化操作：
     * 1. 调用父类构造函数传入可用工具
     * 2. 设置智能体名称、系统提示词和下一步提示词
     * 3. 配置最大执行步数为20
     * 4. 创建并配置ChatClient，添加日志记录器
     * </p>
     *
     * @param allTools 所有可用的工具回调数组
     * @param dashscopeChatModel DashScope聊天模型实例
     */
    public MyManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("MyManus");
        String SYSTEM_PROMPT = """
                You are MyManus, an all-capable AI assistant aimed at solving complex tasks for the user.
                You can call tools when needed for searching, file operations, resource downloads, PDF generation, and other tasks.
                Always answer the user in Chinese unless the user explicitly requests another language.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on the user's needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, break down the problem first, then solve it step by step with tools when useful.
                After using each tool, clearly explain the execution result in Chinese and suggest the next step.
                If key information is missing and you cannot continue safely or accurately, use the `askHuman` tool/function call to ask one clear question.
                When the task is complete, or when no tool call is needed, provide the final answer in Chinese and end this run.
                If you need to actively end the interaction, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(10);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
