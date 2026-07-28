package com.lcl.myaiagent.model.enums;

import java.util.Arrays;

public enum KnowledgeDocumentType {
    RESUME,
    JOB_DESCRIPTION,
    NOTE,
    QUESTION_SET;

    public static KnowledgeDocumentType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的资料类型"));
    }
}
