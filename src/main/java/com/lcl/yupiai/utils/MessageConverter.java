package com.lcl.yupiai.utils;

import com.lcl.yupiai.domain.po.ChatMessage;
import org.springframework.ai.chat.messages.*;

import java.util.List;
import java.util.Map;

/**
 * 消息转换器工具类
 * 用于在Spring AI的Message对象和内部ChatMessage实体之间进行双向转换
 */
public class MessageConverter {

    /**
     * 将Spring AI的Message对象转换为内部ChatMessage实体
     * 提取消息类型、内容、元数据等信息，并关联到指定会话
     *
     * @param message        Spring AI的消息对象，包含消息内容和元数据
     * @param conversationId 会话唯一标识，用于关联聊天记录
     * @return ChatMessage 转换后的内部聊天消息实体
     */
    public static ChatMessage toChatMessage(Message message, String conversationId) {
        return ChatMessage.builder()
                .conversationId(conversationId)
                .messageType(message.getMessageType())
                .content(message.getText())
                .metadata(message.getMetadata())
                .build();
    }

    /**
     * 将内部ChatMessage实体转换为Spring AI的Message对象
     * 根据消息类型（用户/助手/系统/工具）创建对应的Spring AI消息实例
     *
     * @param chatMessage 内部聊天消息实体，包含消息类型、内容和元数据
     * @return Message 转换后的Spring AI消息对象，具体类型取决于消息类型
     */
    public static Message toMessage(ChatMessage chatMessage) {
        MessageType messageType = chatMessage.getMessageType();
        String text = chatMessage.getContent();
        Map<String, Object> metadata = chatMessage.getMetadata();
        return switch (messageType) {
            case USER -> new UserMessage(text);
            case ASSISTANT -> AssistantMessage.builder().content(text).properties(metadata).build();
            case SYSTEM -> new SystemMessage(text);
            case TOOL -> ToolResponseMessage.builder().metadata(metadata).responses(List.of()).build();
        };
    }

}
