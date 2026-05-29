package com.lcl.myaiagent.model.vo;

import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class ChatMessageVO {
    private String id;
    private String role;
    private String content;
    private Date createTime;
}
