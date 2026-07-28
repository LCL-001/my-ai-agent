package com.lcl.myaiagent.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeChunkVO {
    private Long id;
    private Integer chunkIndex;
    private String content;
    private Date createTime;
}
