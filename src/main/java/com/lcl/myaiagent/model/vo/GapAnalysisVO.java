package com.lcl.myaiagent.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class GapAnalysisVO {

    private String id;
    private String status;
    private String summary;
    private boolean insufficientEvidence;
    private List<GapItem> gaps;
    private Date createTime;

    @Data
    public static class GapItem {
        private String skill;
        private String severity;
        private String recommendation;
        private List<Evidence> evidence;
    }

    @Data
    public static class Evidence {
        private Long documentId;
        private String documentName;
        private Integer chunkIndex;
        private String content;
    }
}
