package com.lcl.myaiagent.chatmemory;

import com.lcl.myaiagent.utils.MessageConverter;
import com.lcl.myaiagent.domain.po.ChatMessage;
import com.lcl.myaiagent.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于数据库持久化的对话记忆实现
 * 将聊天消息存储到数据库中，支持按会话ID进行增删查操作
 */
@Component
@RequiredArgsConstructor
public class DataBaseChatMemory implements ChatMemory {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * 添加消息到指定会话的聊天记录中
     * 将Spring AI的Message对象转换为内部ChatMessage实体并批量保存
     *
     * @param conversationId 会话唯一标识
     * @param messages       需要保存的消息列表
     */
    @Override
    public void add(@NotNull String conversationId, List<Message> messages) {
        List<ChatMessage> chatMessageList = messages.stream()
                .map(message -> MessageConverter.toChatMessage(message, conversationId))
                .toList();
        chatMessageRepository.saveBatch(chatMessageList, chatMessageList.size());
    }

    /**
     * 获取指定会话的历史聊天记录
     * 从数据库中查询该会话的所有消息，按创建时间降序排列后转换为Spring AI的Message对象
     *
     * @param conversationId 会话唯一标识
     * @return List<Message> 该会话的历史消息列表，按时间倒序返回
     */
    @NotNull
    @Override
    public List<Message> get(@NotNull String conversationId) {
        List<ChatMessage> messageList = chatMessageRepository.lambdaQuery()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreateTime)
                .list();
        return messageList.stream().map(MessageConverter::toMessage).toList();
    }

    /**
     * 清空指定会话的所有聊天记录
     * 从数据库中删除该会话关联的所有消息
     *
     * @param conversationId 会话唯一标识
     */
    @Override
    public void clear(@NotNull String conversationId) {
        chatMessageRepository.lambdaUpdate()
                .eq(ChatMessage::getConversationId, conversationId)
                .remove();
    }
}
