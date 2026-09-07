package com.lcl.myaiagent.controller;

import cn.hutool.core.util.StrUtil;
import com.lcl.myaiagent.agent.MyManus;
import com.lcl.myaiagent.app.LoveApp;
import com.lcl.myaiagent.chatmemory.DataBaseChatMemory;
import com.lcl.myaiagent.chatmemory.FlowWindowBasedChatMemory;
import com.lcl.myaiagent.constant.UserConstant;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.service.ConversationService;
import com.lcl.myaiagent.service.ConversationTitleService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private DataBaseChatMemory dataBaseChatMemory;

    @Resource
    private FlowWindowBasedChatMemory flowWindowBasedChatMemory;

    @Resource
    private ConversationService conversationService;

    @Resource
    private ConversationTitleService conversationTitleService;

    /**
     * 获取登录用户（可能为 null）
     */
    private String getLoginUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        User user = (User) session.getAttribute(UserConstant.USER_LOGIN_STATE);
        return user != null ? user.getId() : null;
    }

    /**
     * 流式调用 Manus 超级智能体，支持多轮对话记忆和用户绑定
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, String chatId, HttpServletRequest request) {
        // 前端未传 chatId 时生成一个，避免所有匿名请求共用 advisor 的默认会话，
        // 造成不同用户的记忆串到同一个会话里
        final String convId = StrUtil.isBlank(chatId) ? UUID.randomUUID().toString() : chatId;
        // 记忆由 Agent 内部的 Memory Advisor 读取（带 conversationId），
        // 控制器不再手动加载历史，避免与 advisor 注入的记忆重复
        MyManus manus = new MyManus(allTools, dashscopeChatModel, convId, flowWindowBasedChatMemory);

        String userId = getLoginUserId(request);

        SseEmitter emitter = manus.runStream(message);

        // 消息落库由 Memory Advisor 在每步 LLM 调用后自动完成（读写双向），
        // 控制器不再追加，否则与 advisor 的写入重复。
        // 这里只做会话登记等业务簿记
        emitter.onCompletion(() -> {
            conversationService.getOrCreate(convId, userId, "manus");
            conversationTitleService.generateForFirstMessage(convId, userId, message);
            log.info("Conversation bookkeeping done for chatId: {}", convId);
        });

        return emitter;
    }

    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChat(message, chatId);
    }

    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSse(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    @GetMapping("/love_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping("/love_app/chat/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(3 * 60 * 1000L);
        loveApp.doChatByStream(message, chatId)
                .subscribe(
                        chunk -> {
                            try {
                                sseEmitter.send(chunk);
                            } catch (Exception e) {
                                sseEmitter.completeWithError(e);
                            }
                        },
                        sseEmitter::completeWithError,
                        sseEmitter::complete);
        return sseEmitter;
    }
}
