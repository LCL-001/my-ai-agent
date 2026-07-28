package com.lcl.myaiagent.knowledge;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeTextChunker {

    private static final int CHUNK_SIZE = 800;
    private static final int OVERLAP_SIZE = 120;

    private KnowledgeTextChunker() {
    }

    public static List<String> chunk(String text) {
        String normalizedText = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalizedText.isBlank()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int startIndex = 0;
        while (startIndex < normalizedText.length()) {
            int endIndex = Math.min(startIndex + CHUNK_SIZE, normalizedText.length());
            if (endIndex < normalizedText.length()) {
                int sentenceBoundary = Math.max(
                        normalizedText.lastIndexOf('。', endIndex),
                        normalizedText.lastIndexOf(' ', endIndex));
                if (sentenceBoundary > startIndex + CHUNK_SIZE / 2) {
                    endIndex = sentenceBoundary + 1;
                }
            }
            chunks.add(normalizedText.substring(startIndex, endIndex).trim());
            if (endIndex == normalizedText.length()) {
                break;
            }
            startIndex = Math.max(endIndex - OVERLAP_SIZE, startIndex + 1);
        }
        return chunks;
    }
}
