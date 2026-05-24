package com.lcl.yupiai.service.impl;

import com.lcl.yupiai.domain.po.ChatMessage;
import com.lcl.yupiai.mapper.ChatMessageMapper;
import com.lcl.yupiai.service.ChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 聊天消息表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-05-18
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

}
