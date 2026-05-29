package com.lcl.myaiagent.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lcl.myaiagent.agent.MyManus;
import com.lcl.myaiagent.app.LoveApp;
import com.lcl.myaiagent.chatmemory.DataBaseChatMemory;
import com.lcl.myaiagent.constant.UserConstant;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.service.ConversationService;
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
    private ConversationService conversationService;

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
        MyManus manus = new MyManus(allTools, dashscopeChatModel);

        String userId = getLoginUserId(request);

        // 加载历史消息
        if (StrUtil.isNotBlank(chatId)) {
            List<Message> history = dataBaseChatMemory.get(chatId);
            if (CollUtil.isNotEmpty(history)) {
                manus.getMessageList().addAll(history);
                log.info("Loaded {} history messages for chatId: {}", history.size(), chatId);
            }
        }

        SseEmitter emitter = manus.runStream(message);

        // 对话结束后持久化
        if (StrUtil.isNotBlank(chatId)) {
            emitter.onCompletion(() -> {
                List<Message> messages = manus.getMessageList();
                dataBaseChatMemory.clear(chatId);
                dataBaseChatMemory.add(chatId, messages);
                // 自动创建/更新会话，绑定登录用户
                conversationService.getOrCreate(chatId, userId, "manus");
                log.info("Saved {} messages to DB for chatId: {}", messages.size(), chatId);
            });
        }

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
