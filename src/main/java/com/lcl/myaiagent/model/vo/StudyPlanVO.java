package com.lcl.myaiagent.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
public class StudyPlanVO {
    private String id;
    private String gapAnalysisId;
    private String title;
    private String status;
    private String draftNote;
    private Date createTime;
    private List<Task> tasks;

    @Data
    public static class Task {
        private String id;
        private Integer dayNumber;
        private String title;
        private String description;
        private String taskStatus;
        private LocalDate scheduledDate;
    }
}
