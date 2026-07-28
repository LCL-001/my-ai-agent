package com.lcl.myaiagent.service.impl;

import com.lcl.myaiagent.exception.BusinessException;
import com.lcl.myaiagent.common.ErrorCode;
import com.lcl.myaiagent.knowledge.KnowledgeTextChunker;
import com.lcl.myaiagent.knowledge.KnowledgeTextExtractor;
import com.lcl.myaiagent.mapper.KnowledgeChunkMapper;
import com.lcl.myaiagent.mapper.KnowledgeDocumentMapper;
import com.lcl.myaiagent.model.enums.KnowledgeDocumentStatus;
import com.lcl.myaiagent.model.enums.KnowledgeDocumentType;
import com.lcl.myaiagent.model.po.KnowledgeChunk;
import com.lcl.myaiagent.model.po.KnowledgeDocument;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.KnowledgeDocumentVO;
import com.lcl.myaiagent.model.vo.KnowledgeSearchResultVO;
import com.lcl.myaiagent.service.KnowledgeDocumentService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final ObjectProvider<VectorStore> userKnowledgeVectorStoreProvider;
    private final Path storageDirectory;

    public KnowledgeDocumentServiceImpl(
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            KnowledgeChunkMapper knowledgeChunkMapper,
            @Qualifier("userKnowledgeVectorStore") ObjectProvider<VectorStore> userKnowledgeVectorStoreProvider,
            @Value("${app.knowledge.storage-dir}") String storageDirectory) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.userKnowledgeVectorStoreProvider = userKnowledgeVectorStoreProvider;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Override
    public KnowledgeDocumentVO upload(MultipartFile file, KnowledgeDocumentType documentType, User loginUser) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要上传的资料");
        }
        String safeFileName = safeFileName(file.getOriginalFilename());
        String content;
        try {
            content = KnowledgeTextExtractor.extract(file, safeFileName);
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资料解析失败：" + exception.getMessage());
        }
        List<String> textChunks = KnowledgeTextChunker.chunk(content);
        if (textChunks.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资料中没有可检索的文本内容");
        }
        VectorStore vectorStore = userKnowledgeVectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "向量资料库未启用，请使用 Docker 环境启动服务");
        }

        KnowledgeDocument document = new KnowledgeDocument();
        document.setUserId(loginUser.getId());
        document.setName(safeFileName);
        document.setDocumentType(documentType.name());
        document.setStoragePath("pending");
        document.setContentText(content);
        document.setStatus(KnowledgeDocumentStatus.PROCESSING.name());
        document.setChunkCount(0);
        knowledgeDocumentMapper.insert(document);

        try {
            Path storedFile = copyToStorage(file, loginUser.getId(), document.getId(), safeFileName);
            List<KnowledgeChunk> chunks = createChunks(document, textChunks);
            List<Document> vectorDocuments = chunks.stream()
                    .map(chunk -> toVectorDocument(chunk, document))
                    .toList();
            vectorStore.add(vectorDocuments);
            chunks.forEach(knowledgeChunkMapper::insert);
            document.setStoragePath(storedFile.toString());
            document.setStatus(KnowledgeDocumentStatus.READY.name());
            document.setChunkCount(chunks.size());
            knowledgeDocumentMapper.updateById(document);
            return toDocumentVO(document);
        } catch (Exception exception) {
            document.setStatus(KnowledgeDocumentStatus.FAILED.name());
            document.setErrorMessage(limitErrorMessage(exception.getMessage()));
            knowledgeDocumentMapper.updateById(document);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "资料入库失败，请稍后重试");
        }
    }

    @Override
    public List<KnowledgeDocumentVO> list(User loginUser) {
        return knowledgeDocumentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getUserId, loginUser.getId())
                        .orderByDesc(KnowledgeDocument::getCreateTime))
                .stream()
                .map(this::toDocumentVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long documentId, User loginUser) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getId, documentId)
                        .eq(KnowledgeDocument::getUserId, loginUser.getId()));
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资料不存在或无权操作");
        }
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk>()
                        .eq(KnowledgeChunk::getDocumentId, documentId)
                        .eq(KnowledgeChunk::getUserId, loginUser.getId()));
        VectorStore vectorStore = userKnowledgeVectorStoreProvider.getIfAvailable();
        if (vectorStore != null && !chunks.isEmpty()) {
            vectorStore.delete(chunks.stream().map(KnowledgeChunk::getVectorDocumentId).toList());
        }
        knowledgeChunkMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, documentId)
                .eq(KnowledgeChunk::getUserId, loginUser.getId()));
        knowledgeDocumentMapper.deleteById(documentId);
        try {
            Files.deleteIfExists(Path.of(document.getStoragePath()));
        } catch (IOException ignored) {
            // 文件删除失败不应恢复已经完成的权限隔离删除。
        }
    }

    @Override
    public List<KnowledgeSearchResultVO> search(String query, int limit, User loginUser) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入检索问题");
        }
        VectorStore vectorStore = userKnowledgeVectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return List.of();
        }
        int topK = Math.min(Math.max(limit, 1), 10);
        String filterExpression = "userId == '" + loginUser.getId().replace("'", "") + "'";
        return vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query.trim())
                        .topK(topK)
                        .similarityThreshold(0.3)
                        .filterExpression(filterExpression)
                        .build())
                .stream()
                .map(this::toSearchResultVO)
                .toList();
    }

    private Path copyToStorage(MultipartFile file, String userId, Long documentId, String fileName) throws IOException {
        Path documentDirectory = storageDirectory.resolve(userId).resolve(String.valueOf(documentId));
        Files.createDirectories(documentDirectory);
        Path destination = documentDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(documentDirectory)) {
            throw new IOException("非法文件路径");
        }
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    private List<KnowledgeChunk> createChunks(KnowledgeDocument document, List<String> textChunks) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int index = 0; index < textChunks.size(); index++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setUserId(document.getUserId());
            chunk.setChunkIndex(index);
            chunk.setContent(textChunks.get(index));
            chunk.setVectorDocumentId(UUID.randomUUID().toString());
            chunks.add(chunk);
        }
        return chunks;
    }

    private Document toVectorDocument(KnowledgeChunk chunk, KnowledgeDocument document) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", chunk.getUserId());
        metadata.put("documentId", String.valueOf(chunk.getDocumentId()));
        metadata.put("documentName", document.getName());
        metadata.put("documentType", document.getDocumentType());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        return new Document(chunk.getVectorDocumentId(), chunk.getContent(), metadata);
    }

    private KnowledgeDocumentVO toDocumentVO(KnowledgeDocument document) {
        KnowledgeDocumentVO valueObject = new KnowledgeDocumentVO();
        valueObject.setId(document.getId());
        valueObject.setName(document.getName());
        valueObject.setDocumentType(document.getDocumentType());
        valueObject.setStatus(document.getStatus());
        valueObject.setErrorMessage(document.getErrorMessage());
        valueObject.setChunkCount(document.getChunkCount());
        valueObject.setCreateTime(document.getCreateTime());
        return valueObject;
    }

    private KnowledgeSearchResultVO toSearchResultVO(Document document) {
        KnowledgeSearchResultVO valueObject = new KnowledgeSearchResultVO();
        Map<String, Object> metadata = document.getMetadata();
        valueObject.setDocumentId(Long.valueOf(String.valueOf(metadata.get("documentId"))));
        valueObject.setDocumentName(String.valueOf(metadata.get("documentName")));
        valueObject.setDocumentType(String.valueOf(metadata.get("documentType")));
        valueObject.setChunkIndex(Integer.parseInt(String.valueOf(metadata.get("chunkIndex"))));
        valueObject.setContent(document.getText());
        valueObject.setScore(document.getScore());
        return valueObject;
    }

    private String safeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        String fileName = Path.of(originalFileName).getFileName().toString();
        if (fileName.contains("..")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法文件名");
        }
        KnowledgeTextExtractor.extensionOf(fileName);
        return fileName;
    }

    private String limitErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "未知错误";
        }
        return errorMessage.substring(0, Math.min(errorMessage.length(), 500));
    }
}
