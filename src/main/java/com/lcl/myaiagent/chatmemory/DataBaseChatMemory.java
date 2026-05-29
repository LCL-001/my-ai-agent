package com.lcl.myaiagent.chatmemory;

import com.lcl.myaiagent.model.po.ChatMessage;
import com.lcl.myaiagent.repository.ChatMessageRepository;
import com.lcl.myaiagent.utils.MessageConverter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于数据库持久化的对话记忆实现
 */
@Component
@RequiredArgsConstructor
public class DataBaseChatMemory implements ChatMemory {

    private final ChatMessageRepository chatMessageRepository;

    @Override
    public void add(@NotNull String conversationId, List<Message> messages) {
        List<ChatMessage> chatMessageList = messages.stream()
                .map(message -> MessageConverter.toChatMessage(message, conversationId))
                .toList();
        chatMessageRepository.saveBatch(chatMessageList, chatMessageList.size());
    }

    @NotNull
    @Override
    public List<Message> get(@NotNull String conversationId) {
        List<ChatMessage> messageList = chatMessageRepository.lambdaQuery()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreateTime)
                .list();
        return messageList.stream().map(MessageConverter::toMessage).toList();
    }

    @Override
    public void clear(@NotNull String conversationId) {
        chatMessageRepository.lambdaUpdate()
                .eq(ChatMessage::getConversationId, conversationId)
                .remove();
    }
}
