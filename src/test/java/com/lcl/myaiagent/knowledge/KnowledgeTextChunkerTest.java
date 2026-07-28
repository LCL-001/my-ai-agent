package com.lcl.myaiagent.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTextChunkerTest {

    @Test
    void shouldKeepShortTextAsOneChunk() {
        List<String> chunks = KnowledgeTextChunker.chunk("Spring Boot 中使用事务要关注传播行为。");

        assertEquals(1, chunks.size());
        assertEquals("Spring Boot 中使用事务要关注传播行为。", chunks.getFirst());
    }

    @Test
    void shouldSplitLongTextWithBoundedChunks() {
        String content = "Java 面试知识点。".repeat(300);

        List<String> chunks = KnowledgeTextChunker.chunk(content);

        assertTrue(chunks.size() > 1);
        assertFalse(chunks.stream().anyMatch(String::isBlank));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 800));
    }
}
