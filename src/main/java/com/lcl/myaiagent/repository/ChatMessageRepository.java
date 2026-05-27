package com.lcl.myaiagent.repository;

import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.lcl.myaiagent.domain.po.ChatMessage;
import com.lcl.myaiagent.mapper.ChatMessageMapper;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageRepository extends CrudRepository<ChatMessageMapper, ChatMessage> {
}