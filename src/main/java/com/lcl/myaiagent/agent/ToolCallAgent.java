package com.lcl.myaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.lcl.myaiagent.agent.model.AgentState;
import com.lcl.myaiagent.tools.AskHumanTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具调用智能体，继承自ReActAgent
 * <p>
 * 该类实现了基于LLM的工具调用功能，能够根据上下文自动选择合适的工具执行任务。
 * 通过集成ToolCallingManager和ToolCallback机制，支持动态工具管理和执行。
 * </p>
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
public class ToolCallAgent extends ReActAgent {

    /**
     * 可用的工具回调数组，包含所有可供智能体调用的工具
     */
    private ToolCallback[] availableTools;

    /**
     * 存储最近一次LLM的响应结果，包含工具调用信息
     */
    private ChatResponse toolCallChatResponse;

    /**
     * 工具调用管理器，负责工具的注册、解析和执行
     */
    private ToolCallingManager toolCallingManager;

    /**
     * 禁用内置的工具调用机制，自己维护上下文
     */
    private final ChatOptions chatOptions;


    /**
     * 构造函数，初始化可用工具和配置
     *
     * @param availableTools 可用的工具回调数组
     */
    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 思考过程，分析当前状态并决定是否调用工具
     * <p>
     * 该方法执行以下逻辑：
     * 1. 如果有下一步提示，将其添加到消息列表
     * 2. 构建Prompt并调用LLM获取响应
     * 3. 解析响应中的工具调用信息
     * 4. 根据是否有工具调用来决定是否需要行动
     * </p>
     *
     * @return true表示需要调用工具，false表示无需调用工具
     */
    @Override
    public boolean think() {
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);
        try {
            // 获取带工具选项的响应
            ChatResponse chatResponse = getChatClient()
                    .prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于 Act
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 输出提示信息
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
//            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了" + toolCallList.size() + "个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s, 工具参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("/n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才记录助手信息
                getMessageList().add(assistantMessage);
//                setState(AgentState.FINISHED);
                return false;
            } else {
                // 需要调用工具时，无需记录助手信息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并返回结果
     * <p>
     * 该方法执行以下逻辑：
     * 1. 检查是否有工具调用
     * 2. 使用ToolCallingManager执行工具调用
     * 3. 更新对话历史，添加工具执行结果
     * 4. 提取并返回工具执行的响应信息
     * </p>
     *
     * @return 工具执行结果的描述字符串，包含各工具的完成情况和返回数据
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
//            return "没有工具调用";
            // 返回上一步的输出
            return getMessageList().getLast().getText();
        }
        AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
        String assistantText = assistantMessage.getText();
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        Message message = getMessageList().getLast();
        // 获取当前工具调用的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(toolResponse -> "工具 " + toolResponse.name() + " 完成了它的任务！结果：" + toolResponse.responseData())
                .collect(Collectors.joining("\n"));
        String askHumanQuestion = toolResponseMessage.getResponses().stream()
                .filter(toolResponse -> "askHuman".equals(toolResponse.name()))
                .map(ToolResponseMessage.ToolResponse::responseData)
                .filter(responseData -> responseData.startsWith(AskHumanTool.ASK_HUMAN_PREFIX))
                .map(responseData -> responseData.substring(AskHumanTool.ASK_HUMAN_PREFIX.length()))
                .findFirst()
                .orElse(null);
        if (askHumanQuestion != null) {
            setState(AgentState.FINISHED);
            log.info("{} needs user clarification: {}", getName(), askHumanQuestion);
            return "需要用户补充信息：" + askHumanQuestion;
        }
        // 判断是否调用了终止工具
        if (toolResponseMessage.getResponses().stream()
                .anyMatch(toolResponse -> "doTerminate".equals(toolResponse.name()))) {
            setState(AgentState.FINISHED);
            if (StrUtil.isNotBlank(assistantText)) {
                return assistantText;
            }
            return "任务结束";
        }
        log.info(getName() + "的输出：" + results);
        return results;
    }

    /**
     * 清理资源方法
     * <p>
     * 在run()或runStream()方法执行完成后清理运行时状态。
     * 该方法可能被多次调用（正常完成、超时、完成回调），因此实现具有幂等性。
     * 保留消息历史以支持多轮对话，仅重置执行控制相关的临时状态。
     * </p>
     */
    @Override
    protected void cleanUp() {
        // 重置步骤计数器，为下次运行做准备
        setCurrentStep(0);

        // 清理工具调用响应缓存（这是临时数据，可以清空）
        this.toolCallChatResponse = null;

        // 重置状态为空闲，允许再次运行
        // 注意：如果已经是ERROR状态，不要覆盖
        if (getState() != AgentState.ERROR) {
            setState(AgentState.IDLE);
        }

        // 重要：不清空messageList！
        // 原因：
        // 1. messageList保存了当前会话的完整对话历史
        // 2. runStream()和run()都依赖这个列表维持上下文
        // 3. 外部ChatMemory负责持久化，但内存中的历史需要保持

        log.debug("{} 资源清理完成，当前消息数: {}", getName(), getMessageList().size());
    }


}
