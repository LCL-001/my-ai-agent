package com.lcl.myaiagent.knowledge;

import com.baomidou.mybatisplus.annotation.TableField;
import com.lcl.myaiagent.model.po.KnowledgeChunk;
import com.lcl.myaiagent.model.po.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KnowledgeEntityMappingTest {

    @Test
    void mapsKnowledgeDocumentCamelCasePropertiesToDatabaseColumns() {
        assertColumn(KnowledgeDocument.class, "userId", "user_id");
        assertColumn(KnowledgeDocument.class, "documentType", "document_type");
        assertColumn(KnowledgeDocument.class, "storagePath", "storage_path");
        assertColumn(KnowledgeDocument.class, "contentText", "content_text");
        assertColumn(KnowledgeDocument.class, "errorMessage", "error_message");
        assertColumn(KnowledgeDocument.class, "chunkCount", "chunk_count");
    }

    @Test
    void mapsKnowledgeChunkCamelCasePropertiesToDatabaseColumns() {
        assertColumn(KnowledgeChunk.class, "documentId", "document_id");
        assertColumn(KnowledgeChunk.class, "userId", "user_id");
        assertColumn(KnowledgeChunk.class, "chunkIndex", "chunk_index");
        assertColumn(KnowledgeChunk.class, "vectorDocumentId", "vector_document_id");
    }

    private void assertColumn(Class<?> entityType, String property, String column) {
        try {
            TableField tableField = entityType.getDeclaredField(property).getAnnotation(TableField.class);
            assertNotNull(tableField, () -> property + " must declare its database column");
            assertEquals(column, tableField.value());
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing property: " + property, exception);
        }
    }
}
