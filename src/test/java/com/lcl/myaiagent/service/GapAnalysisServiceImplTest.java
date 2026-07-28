package com.lcl.myaiagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.myaiagent.ai.LocalStructuredAiClient;
import com.lcl.myaiagent.analysis.AiGapReport;
import com.lcl.myaiagent.mapper.GapAnalysisMapper;
import com.lcl.myaiagent.model.po.GapAnalysis;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.GapAnalysisVO;
import com.lcl.myaiagent.model.vo.KnowledgeSearchResultVO;
import com.lcl.myaiagent.service.impl.GapAnalysisServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GapAnalysisServiceImplTest {

    @Test
    void returnsUnverifiedResultWithoutPrivateEvidence() {
        GapAnalysisMapper mapper = mock(GapAnalysisMapper.class);
        KnowledgeDocumentService knowledge = mock(KnowledgeDocumentService.class);
        LocalStructuredAiClient aiClient = mock(LocalStructuredAiClient.class);
        when(knowledge.search(any(), eq(4), any())).thenReturn(List.of());
        GapAnalysisServiceImpl service = new GapAnalysisServiceImpl(mapper, knowledge, aiClient, new ObjectMapper());

        GapAnalysisVO report = service.analyze("需要熟悉 Java 和 Redis", user());

        assertTrue(report.isInsufficientEvidence());
        assertEquals("UNVERIFIED", report.getGaps().getFirst().getVerdict());
        assertFalse(report.getGaps().getFirst().isEvidenceAvailable());
        verifyNoInteractions(aiClient);
        verify(mapper).insert(any(GapAnalysis.class));
    }

    @Test
    void persistsOnlyEvidenceBackedAiConclusion() {
        GapAnalysisMapper mapper = mock(GapAnalysisMapper.class);
        KnowledgeDocumentService knowledge = mock(KnowledgeDocumentService.class);
        LocalStructuredAiClient aiClient = mock(LocalStructuredAiClient.class);
        KnowledgeSearchResultVO evidence = evidence();
        when(knowledge.search(any(), eq(4), any())).thenReturn(List.of(evidence));
        when(aiClient.generate(any(), any(), eq(AiGapReport.class), any())).thenReturn(aiReport());
        GapAnalysisServiceImpl service = new GapAnalysisServiceImpl(mapper, knowledge, aiClient, new ObjectMapper());

        GapAnalysisVO report = service.analyze("需要熟悉 Java", user());

        assertEquals("MATCHED", report.getGaps().getFirst().getVerdict());
        assertTrue(report.getGaps().getFirst().isEvidenceAvailable());
        assertEquals(7L, report.getGaps().getFirst().getEvidence().getFirst().getDocumentId());
        verify(mapper).insert(any(GapAnalysis.class));
    }

    private User user() {
        User user = new User();
        user.setId("user-a");
        return user;
    }

    private KnowledgeSearchResultVO evidence() {
        KnowledgeSearchResultVO result = new KnowledgeSearchResultVO();
        result.setDocumentId(7L);
        result.setDocumentName("resume.md");
        result.setChunkIndex(2);
        result.setContent("负责 Java Spring Boot 服务开发");
        return result;
    }

    private AiGapReport aiReport() {
        AiGapReport.Conclusion conclusion = new AiGapReport.Conclusion();
        conclusion.setSkill("Java");
        conclusion.setVerdict("MATCHED");
        conclusion.setSeverity("LOW");
        conclusion.setRecommendation("准备项目细节和边界条件。");
        conclusion.setEvidenceIds(List.of("E1"));
        AiGapReport report = new AiGapReport();
        report.setSummary("简历中有 Java 项目证据。");
        report.setConclusions(List.of(conclusion));
        return report;
    }
}
