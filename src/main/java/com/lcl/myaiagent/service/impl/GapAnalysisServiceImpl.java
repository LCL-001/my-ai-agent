package com.lcl.myaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.myaiagent.analysis.GapKeywordMatcher;
import com.lcl.myaiagent.common.ErrorCode;
import com.lcl.myaiagent.exception.BusinessException;
import com.lcl.myaiagent.mapper.GapAnalysisMapper;
import com.lcl.myaiagent.mapper.KnowledgeDocumentMapper;
import com.lcl.myaiagent.model.po.GapAnalysis;
import com.lcl.myaiagent.model.po.KnowledgeDocument;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.GapAnalysisVO;
import com.lcl.myaiagent.service.GapAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GapAnalysisServiceImpl implements GapAnalysisService {

    private final GapAnalysisMapper gapAnalysisMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public GapAnalysisVO analyze(String jobDescription, User loginUser) {
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请粘贴目标岗位的 JD");
        }
        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getUserId, loginUser.getId())
                .eq(KnowledgeDocument::getStatus, "READY"));
        GapAnalysisVO report = buildReport(jobDescription, documents);

        GapAnalysis analysis = new GapAnalysis();
        analysis.setId(UUID.randomUUID().toString());
        analysis.setUserId(loginUser.getId());
        analysis.setStatus("READY");
        analysis.setContent(writeReport(report));
        gapAnalysisMapper.insert(analysis);
        report.setId(analysis.getId());
        report.setStatus(analysis.getStatus());
        report.setCreateTime(analysis.getCreateTime());
        return report;
    }

    @Override
    public List<GapAnalysisVO> list(User loginUser) {
        return gapAnalysisMapper.selectList(new LambdaQueryWrapper<GapAnalysis>()
                        .eq(GapAnalysis::getUserId, loginUser.getId())
                        .orderByDesc(GapAnalysis::getCreateTime))
                .stream()
                .map(this::readReport)
                .toList();
    }

    private GapAnalysisVO buildReport(String jobDescription, List<KnowledgeDocument> documents) {
        GapAnalysisVO report = new GapAnalysisVO();
        report.setGaps(new ArrayList<>());
        if (documents.isEmpty()) {
            report.setInsufficientEvidence(true);
            report.setSummary("尚未检索到你的已就绪资料，因此不会生成没有依据的能力差距结论。请先上传简历、笔记或项目材料。");
            return report;
        }
        List<String> requiredSkills = GapKeywordMatcher.requiredSkills(jobDescription);
        for (String skill : requiredSkills) {
            List<KnowledgeDocument> evidenceDocuments = documents.stream()
                    .filter(document -> GapKeywordMatcher.mentions(document.getContentText(), skill))
                    .toList();
            if (evidenceDocuments.isEmpty()) {
                report.getGaps().add(gap(skill, jobDescription));
            }
        }
        report.setInsufficientEvidence(false);
        report.setSummary(report.getGaps().isEmpty()
                ? "在已上传资料中找到了 JD 所列核心技能的直接证据；建议继续用项目细节验证深度。"
                : "识别到 " + report.getGaps().size() + " 项 JD 技能在当前资料中没有直接证据，可优先补充学习记录或项目证明。");
        return report;
    }

    private GapAnalysisVO.GapItem gap(String skill, String jobDescription) {
        GapAnalysisVO.Evidence evidence = new GapAnalysisVO.Evidence();
        evidence.setDocumentName("输入的岗位 JD");
        evidence.setChunkIndex(0);
        evidence.setContent(snippet(jobDescription, skill));
        GapAnalysisVO.GapItem item = new GapAnalysisVO.GapItem();
        item.setSkill(skill);
        item.setSeverity("HIGH");
        item.setRecommendation("补充 " + skill + " 的学习笔记或可讲解的项目实践，并在下一次分析前上传作为证据。");
        item.setEvidence(List.of(evidence));
        return item;
    }

    private String writeReport(GapAnalysisVO report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分析报告保存失败");
        }
    }

    private GapAnalysisVO readReport(GapAnalysis analysis) {
        try {
            GapAnalysisVO report = objectMapper.readValue(analysis.getContent(), GapAnalysisVO.class);
            report.setId(analysis.getId());
            report.setStatus(analysis.getStatus());
            report.setCreateTime(analysis.getCreateTime());
            return report;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "历史分析报告无法读取");
        }
    }

    private String snippet(String content, String skill) {
        int index = content.toLowerCase().indexOf(skill.toLowerCase());
        int start = Math.max(index - 60, 0);
        int end = Math.min(index + skill.length() + 100, content.length());
        return content.substring(start, end).trim();
    }
}
