package com.lcl.myaiagent.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GapKeywordMatcherTest {

    @Test
    void shouldExtractRequiredSkillsAndMatchCaseInsensitively() {
        String jd = "招聘熟悉 Java、Spring Boot 和 MySQL 的后端实习生";

        assertTrue(GapKeywordMatcher.requiredSkills(jd).contains("Spring Boot"));
        assertTrue(GapKeywordMatcher.mentions("我完成了 spring boot 项目", "Spring Boot"));
        assertFalse(GapKeywordMatcher.mentions("只会 HTML", "Redis"));
    }
}
