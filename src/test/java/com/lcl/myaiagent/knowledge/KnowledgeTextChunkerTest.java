package com.lcl.myaiagent.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTextChunkerTest {

    @Test
    void shouldKeepShortTextAsOneChunk() {
        List<String> chunks = KnowledgeTextChunker.chunk("Spring Boot transactions need clear propagation rules.");

        assertEquals(1, chunks.size());
        assertEquals("Spring Boot transactions need clear propagation rules.", chunks.getFirst());
    }

    @Test
    void shouldSplitLongTextWithBoundedChunks() {
        String content = "Java interview topic. ".repeat(300);

        List<String> chunks = KnowledgeTextChunker.chunk(content);

        assertTrue(chunks.size() > 1);
        assertFalse(chunks.stream().anyMatch(String::isBlank));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 800));
    }

    @Test
    void shouldPreserveHeadingContextAndRemoveMarkdownMarkup() {
        String markdown = """
                # Redis notes

                ## Cache penetration
                **Cache penetration** queries data that does not exist. Use a Bloom filter to prevent it.

                ## Distributed locking
                Use setnx with an expiry and a unique token to prevent accidental unlocks.
                """;

        List<String> chunks = KnowledgeTextChunker.chunk(markdown);

        assertEquals(2, chunks.size());
        assertTrue(chunks.getFirst().startsWith("Cache penetration"));
        assertTrue(chunks.getFirst().contains("Bloom filter"));
        assertTrue(chunks.get(1).startsWith("Distributed locking"));
        assertTrue(chunks.stream().noneMatch(chunk -> chunk.contains("#") || chunk.contains("**")));
    }
}
