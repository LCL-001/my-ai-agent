package com.lcl.myaiagent.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudyTaskUpdateRequest {
    private String title;
    private String description;
    private String taskStatus;
    private LocalDate scheduledDate;
}
