package com.lcl.myaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.common.Role;
import com.lcl.myaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 智能体基类，提供基础的AI Agent执行框架
 * <p>
 * 该抽象类定义了智能体的核心运行逻辑，包括状态管理、步骤控制、记忆系统等功能。
 * 子类需要实现step()方法定义单步执行逻辑，以及cleanUp()方法进行资源清理。
 * </p>
 */
@Slf4j
@Data
public abstract class BaseAgent {
    // 智能体名称
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 智能体状态
    private AgentState state = AgentState.IDLE;// 默认为空闲状态

    // 智能体执行控制
    private int currentStep = 0;
    private int maxSteps = 10;

    // LLM模型
    private ChatClient chatClient;

    // 记忆系统(Memory 自主维护)
    private List<Message> messageList = new ArrayList<>();

    // 添加重复内容阈值
    private int duplicateThreshold = 2;

    // 陷入循环计数器
    private int stuckCount = 0;
    private static final int MAX_STUCK_COUNT = 3;

    /**
     * 运行智能体，处理用户输入并返回执行结果
     * <p>
     * 该方法执行以下流程：
     * 1. 校验当前状态和用户输入
     * 2. 设置运行状态并记录用户消息
     * 3. 循环执行智能体步骤直到完成或达到最大步数
     * 4. 处理异常并清理资源
     * </p>
     *
     * @param userPrompt 用户输入的提示信息，不能为空
     * @return 执行结果字符串，包含各步骤的执行结果或错误信息
     * @throws RuntimeException 当智能体状态不为IDLE或用户输入为空时抛出
     */
    public String run(String userPrompt) {
        // 校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Can not run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("User prompt can not be empty.");
        }
        // 更改状态
        this.state = AgentState.RUNNING;
        // 重置循环计数器
        this.stuckCount = 0;
        // 记录上下文
        this.messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            int stepNumber = 0;
            // 执行
            while (this.currentStep < this.maxSteps && this.state != AgentState.FINISHED) {
                stepNumber++;
                this.currentStep = stepNumber;
                log.info("Executing step: {}/{}", stepNumber, this.maxSteps);
                // 单步执行
                String stepResult = this.step();
                // 检查是否陷入循环
                if (isStuck()) {
                    handleStuckState();
                    if (this.state == AgentState.FINISHED) {
                        results.add("Terminated: Agent stuck in a loop");
                        break;
                    }
                }
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (this.currentStep >= this.maxSteps) {
                this.state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + this.maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            this.state = AgentState.ERROR;
            log.error("Error executing agent: ", e);
            return "执行错误，Error: " + e.getMessage();
        } finally {
            // 清理资源
            this.cleanUp();
        }
    }

    /**
     * 以流式方式运行智能体，通过SSE（Server-Sent Events）实时返回执行结果
     * <p>
     * 该方法使用异步执行和SSE技术，将智能体的每一步执行结果实时推送给客户端。
     * 主要流程包括：
     * 1. 创建SseEmitter对象并设置5分钟超时
     * 2. 在异步线程中校验状态和输入参数
     * 3. 循环执行智能体步骤，每步完成后立即发送结果
     * 4. 处理异常、超时和完成事件，确保资源正确清理
     * </p>
     *
     * @param userPrompt 用户输入的提示信息，不能为空
     * @return SseEmitter SSE发射器对象，用于向客户端推送流式响应
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建 SseEmitter 对象，超时设置为 5 分钟
        SseEmitter emitter = new SseEmitter(300000L);
        // 校验
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误：无法从该状态运行代理：" + this.state);
                    emitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    emitter.send("错误：用户提示不能为空。");
                    emitter.complete();
                    return;
                }
                // 更改状态
                this.state = AgentState.RUNNING;
                // 重置循环计数器
                this.stuckCount = 0;
                // 记录上下文
                this.messageList.add(new UserMessage(userPrompt));

                try {
                    int stepNumber = 0;
                    // 执行
                    while (this.currentStep < this.maxSteps && this.state != AgentState.FINISHED) {
                        stepNumber++;
                        this.currentStep = stepNumber;
                        log.info("Executing step: {}/{}", stepNumber, this.maxSteps);
                        // 单步执行
                        String stepResult = this.step();
                        // 每一步 step 执行完都要检查是否陷入循环
                        if (isStuck()) {
                            handleStuckState();
                            if (this.state == AgentState.FINISHED) {
                                emitter.send("检测到循环，智能体已终止");
                                break;
                            }
                        }
//                        String result = "Step " + this.currentStep + ": " + stepResult;
//                        // 发送每一步的结果
//                        emitter.send(result);
                        // 发送每一步的结果
                        emitter.send(stepResult);
                    }
                    // 检查是否超出步骤限制
                    if (this.currentStep >= this.maxSteps) {
                        this.state = AgentState.FINISHED;
                        emitter.send("执行结束：达到最大步骤 (" + this.maxSteps + ")");
                    }
                    // 正常完成
                    emitter.complete();
                } catch (Exception e) {
                    this.state = AgentState.ERROR;
                    log.error("Error executing agent: ", e);
                    try {
                        emitter.send("执行错误，Error: " + e.getMessage());
                        emitter.complete();
                    } catch (IOException ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanUp();
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });
        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanUp();
            log.warn("SSE connection timed out.");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanUp();
            log.info("SSE connection completed.");
        });
        return emitter;
    }

    /**
     * 执行智能体的单步操作
     * <p>
     * 子类必须实现此方法来定义具体的单步执行逻辑。
     * 该方法会在run()方法的循环中被调用，每次执行代表智能体的一个推理或行动步骤。
     * </p>
     *
     * @return 当前步骤的执行结果描述
     */
    public abstract String step();

    /**
     * 清理智能体占用的资源
     * <p>
     * 在run()方法执行完成后（无论正常结束还是异常）都会被调用，
     * 子类应在此方法中实现必要的资源清理逻辑，如关闭连接、释放内存等。
     * </p>
     */
    protected abstract void cleanUp();

    /**
     * 处理陷入循环的状态
     */
    protected void handleStuckState() {
        stuckCount++;
        if (stuckCount >= MAX_STUCK_COUNT) {
            log.warn("Agent stuck {} times, forcing termination", stuckCount);
            this.state = AgentState.FINISHED;
            return;
        }
        String stuckPrompt = "观察到重复响应。考虑新策略，避免重复已尝试过的无效路径。";
        this.nextStepPrompt = stuckPrompt + "\n" + (this.nextStepPrompt != null ? this.nextStepPrompt : "");
        log.warn("Agent detected stuck state ({} / {}). Added prompt: {}", stuckCount, MAX_STUCK_COUNT, stuckPrompt);
    }

    /**
     * 检查代理是否陷入循环
     * 查找最后一条 ASSISTANT 消息，检测是否与历史 ASSISTANT 消息重复
     *
     * @return 是否陷入循环
     */
    protected boolean isStuck() {
        List<Message> messages = getMessageList();
        if (messages.size() < 2) {
            return false;
        }

        // 从末尾查找最后一条 ASSISTANT 消息（跳过 ToolResponseMessage 等）
        Message lastAssistantMsg = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.getMessageType() == MessageType.ASSISTANT) {
                lastAssistantMsg = msg;
                break;
            }
        }

        if (lastAssistantMsg == null
                || lastAssistantMsg.getText() == null
                || lastAssistantMsg.getText().isEmpty()) {
            return false;
        }

        // 通过引用查找最后一条 ASSISTANT 消息的位置（不能用 indexOf，因为 equals 是按内容比较）
        int lastIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) == lastAssistantMsg) {
                lastIndex = i;
                break;
            }
        }
        // 计算该 ASSISTANT 消息在历史 ASSISTANT 消息中的重复次数
        int duplicateCount = 0;
        for (int i = lastIndex - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.getMessageType() == MessageType.ASSISTANT
                    && lastAssistantMsg.getText().equals(msg.getText())) {
                duplicateCount++;
            }
        }

        return duplicateCount >= this.duplicateThreshold;
    }

}
