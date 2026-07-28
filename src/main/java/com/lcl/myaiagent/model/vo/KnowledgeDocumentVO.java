package com.lcl.myaiagent.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeDocumentVO {
    private Long id;
    private String name;
    private String documentType;
    private String status;
    private String errorMessage;
    private Integer chunkCount;
    private Date createTime;
}
