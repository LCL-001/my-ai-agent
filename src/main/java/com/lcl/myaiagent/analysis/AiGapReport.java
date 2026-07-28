package com.lcl.myaiagent.analysis;

import lombok.Data;

import java.util.List;

@Data
public class AiGapReport {

    private String summary;
    private List<Conclusion> conclusions;

    @Data
    public static class Conclusion {
        private String skill;
        private String verdict;
        private String severity;
        private String recommendation;
        private List<String> evidenceIds;
    }
}
