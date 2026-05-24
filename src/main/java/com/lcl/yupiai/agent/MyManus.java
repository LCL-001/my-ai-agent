package com.lcl.yupiai.agent;

import com.lcl.yupiai.advisors.MyLoggerAdvisor;
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
                You are MyManus, an all-capable AI assistant, aimed at solving any task presented by the user.  
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.  
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
                Based on user needs, proactively select the most appropriate tool or combination of tools.  
                For complex tasks, you can break down the problem and use different tools step by step to solve it.  
                After using each tool, clearly explain the execution results and suggest the next steps.  
                If you want to stop the interaction at any point, use the `terminate` tool/function call.  
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
