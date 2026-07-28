package com.lcl.myaiagent.model.vo;

import lombok.Data;

@Data
public class KnowledgeSearchResultVO {
    private Long documentId;
    private String documentName;
    private String documentType;
    private Integer chunkIndex;
    private String content;
    private Double score;
}
