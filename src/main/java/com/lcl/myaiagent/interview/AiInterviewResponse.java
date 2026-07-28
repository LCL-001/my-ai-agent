package com.lcl.myaiagent.interview;

import lombok.Data;

import java.util.List;

@Data
public class AiInterviewResponse {

    private String question;
    private String questionContext;
    private Integer score;
    private String feedback;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> practiceTasks;
    private String nextQuestion;
    private String nextQuestionContext;
}
