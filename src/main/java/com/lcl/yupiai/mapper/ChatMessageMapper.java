package com.lcl.yupiai.mapper;

import com.lcl.yupiai.domain.po.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 聊天消息表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-05-18
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

}
