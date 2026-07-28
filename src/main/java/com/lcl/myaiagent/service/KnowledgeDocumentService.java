package com.lcl.myaiagent.service;

import com.lcl.myaiagent.model.enums.KnowledgeDocumentType;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.KnowledgeDocumentVO;
import com.lcl.myaiagent.model.vo.KnowledgeChunkVO;
import com.lcl.myaiagent.model.vo.KnowledgeSearchResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeDocumentService {

    KnowledgeDocumentVO upload(MultipartFile file, KnowledgeDocumentType documentType, User loginUser);

    List<KnowledgeDocumentVO> list(User loginUser);

    List<KnowledgeChunkVO> listChunks(long documentId, User loginUser);

    KnowledgeDocumentVO reindex(long documentId, User loginUser);

    void delete(long documentId, User loginUser);

    List<KnowledgeSearchResultVO> search(String query, int limit, User loginUser);
}
