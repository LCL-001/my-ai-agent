package com.lcl.myaiagent.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KnowledgeTextChunker {

    private static final int CHUNK_SIZE = 800;
    private static final int OVERLAP_SIZE = 120;
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^#{1,6}\\s+(.+?)\\s*#*$");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^]]+)]\\([^)]*\\)");

    private KnowledgeTextChunker() {
    }

    public static List<String> chunk(String text) {
        String normalizedText = normalizeLineEndings(text);
        if (normalizedText.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        String currentHeading = "";
        StringBuilder paragraph = new StringBuilder();
        for (String rawLine : normalizedText.split("\\n", -1)) {
            String line = cleanMarkdown(rawLine);
            Matcher headingMatcher = MARKDOWN_HEADING.matcher(line);
            if (headingMatcher.matches()) {
                appendParagraph(chunks, currentHeading, paragraph);
                currentHeading = cleanInlineMarkup(headingMatcher.group(1));
            } else if (line.isBlank()) {
                appendParagraph(chunks, currentHeading, paragraph);
            } else {
                if (!paragraph.isEmpty()) {
                    paragraph.append(' ');
                }
                paragraph.append(line);
            }
        }
        appendParagraph(chunks, currentHeading, paragraph);
        return chunks;
    }

    private static String normalizeLineEndings(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String cleanMarkdown(String line) {
        String normalizedLine = line == null ? "" : line.trim();
        if (normalizedLine.isBlank()) {
            return "";
        }
        return cleanInlineMarkup(normalizedLine)
                .replaceFirst("^(?:[-*+]\\s+|\\d+\\.\\s+)", "")
                .trim();
    }

    private static String cleanInlineMarkup(String value) {
        return MARKDOWN_LINK.matcher(value)
                .replaceAll("$1")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .trim();
    }

    private static void appendParagraph(List<String> chunks, String heading, StringBuilder paragraph) {
        String content = paragraph.toString().trim();
        paragraph.setLength(0);
        if (!content.isBlank()) {
            appendChunks(chunks, heading, content);
        }
    }

    private static void appendChunks(List<String> chunks, String heading, String content) {
        String prefix = heading.isBlank() ? "" : heading + "\n";
        int bodyChunkSize = Math.max(CHUNK_SIZE - prefix.length(), CHUNK_SIZE / 2);
        int startIndex = 0;
        while (startIndex < content.length()) {
            int endIndex = Math.min(startIndex + bodyChunkSize, content.length());
            if (endIndex < content.length()) {
                int boundary = findBoundary(content, startIndex, endIndex);
                if (boundary > startIndex + bodyChunkSize / 2) {
                    endIndex = boundary + 1;
                }
            }
            chunks.add((prefix + content.substring(startIndex, endIndex).trim()).trim());
            if (endIndex == content.length()) {
                break;
            }
            startIndex = Math.max(endIndex - OVERLAP_SIZE, startIndex + 1);
        }
    }

    private static int findBoundary(String content, int startIndex, int endIndex) {
        for (int index = endIndex - 1; index > startIndex; index--) {
            char character = content.charAt(index);
            if (character == '。' || character == '！' || character == '？'
                    || character == '.' || character == '!' || character == '?'
                    || character == '；' || character == ';' || character == ' ') {
                return index;
            }
        }
        return -1;
    }
}
