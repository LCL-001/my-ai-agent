package com.lcl.myaiagent.analysis;

import java.util.List;
import java.util.Locale;

public final class GapKeywordMatcher {

    private static final List<String> SKILLS = List.of(
            "Java", "Spring Boot", "MySQL", "Redis", "Kafka", "Docker", "Kubernetes",
            "RabbitMQ", "JVM", "MyBatis", "Linux", "Git", "Vue", "Vue.js", "Elasticsearch");

    private GapKeywordMatcher() {
    }

    public static List<String> requiredSkills(String jobDescription) {
        String normalized = normalize(jobDescription);
        return SKILLS.stream().filter(skill -> normalized.contains(normalize(skill))).toList();
    }

    public static boolean mentions(String content, String skill) {
        return normalize(content).contains(normalize(skill));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(".js", "").replaceAll("\\s+", " ");
    }
}
