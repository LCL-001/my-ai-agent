package com.lcl.myaiagent.knowledge;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class KnowledgeTextExtractor {

    private KnowledgeTextExtractor() {
    }

    public static String extract(MultipartFile file, String fileName) throws IOException {
        String extension = extensionOf(fileName);
        return switch (extension) {
            case "txt", "md", "markdown" -> new String(file.getBytes(), StandardCharsets.UTF_8);
            case "pdf" -> extractPdf(file);
            case "docx" -> extractDocx(file);
            default -> throw new IllegalArgumentException("仅支持 PDF、DOCX、Markdown 和 TXT 文件");
        };
    }

    public static String extensionOf(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("文件缺少扩展名");
        }
        return fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String extractPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            return document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .filter(text -> !text.isBlank())
                    .reduce("", (current, text) -> current + "\n" + text);
        }
    }
}
