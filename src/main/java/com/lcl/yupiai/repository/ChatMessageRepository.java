package com.lcl.yupiai.repository;

import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.lcl.yupiai.domain.po.ChatMessage;
import com.lcl.yupiai.mapper.ChatMessageMapper;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageRepository extends CrudRepository<ChatMessageMapper, ChatMessage> {
}