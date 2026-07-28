package com.lcl.myaiagent.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class InterviewSessionVO {

    private String id;
    private String topic;
    private String status;
    private Integer questionLimit;
    private Date createTime;
    private List<Turn> turns;

    @Data
    public static class Turn {
        private Integer turnNumber;
        private String question;
        private String questionContext;
        private String answer;
        private String feedback;
        private Integer score;
        private String turnStatus;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> practiceTasks;
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
