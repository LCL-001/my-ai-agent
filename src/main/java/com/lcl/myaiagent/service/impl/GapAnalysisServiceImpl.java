package com.lcl.myaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.myaiagent.ai.LocalStructuredAiClient;
import com.lcl.myaiagent.analysis.AiGapReport;
import com.lcl.myaiagent.analysis.GapKeywordMatcher;
import com.lcl.myaiagent.common.ErrorCode;
import com.lcl.myaiagent.exception.BusinessException;
import com.lcl.myaiagent.mapper.GapAnalysisMapper;
import com.lcl.myaiagent.model.po.GapAnalysis;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.GapAnalysisVO;
import com.lcl.myaiagent.model.vo.KnowledgeSearchResultVO;
import com.lcl.myaiagent.service.GapAnalysisService;
import com.lcl.myaiagent.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GapAnalysisServiceImpl implements GapAnalysisService {

    private static final Set<String> VERDICTS = Set.of("MATCHED", "GAP", "UNVERIFIED");
    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "UNKNOWN");

    private final GapAnalysisMapper gapAnalysisMapper;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final LocalStructuredAiClient structuredAiClient;
    private final ObjectMapper objectMapper;

    @Override
    public GapAnalysisVO analyze(String jobDescription, User loginUser) {
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请粘贴目标岗位的 JD");
        }
        GapAnalysisVO report = buildReport(jobDescription, loginUser);

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

    private GapAnalysisVO buildReport(String jobDescription, User loginUser) {
        GapAnalysisVO report = new GapAnalysisVO();
        report.setGaps(new ArrayList<>());
        List<String> detectedSkills = GapKeywordMatcher.requiredSkills(jobDescription);
        List<String> skills = detectedSkills.isEmpty() ? List.of("岗位核心要求") : detectedSkills;

        Map<String, List<KnowledgeSearchResultVO>> evidenceBySkill = retrieveEvidence(skills, jobDescription, loginUser);
        if (evidenceBySkill.values().stream().flatMap(Collection::stream).findAny().isEmpty()) {
            report.setInsufficientEvidence(true);
            report.setSummary("未检索到可用于验证岗位要求的私有资料，因此不会生成没有依据的能力差距结论。请先上传简历、项目材料或复习笔记。");
            skills.forEach(skill -> report.getGaps().add(unverified(skill)));
            return report;
        }

        Map<String, KnowledgeSearchResultVO> evidenceCatalog = catalog(evidenceBySkill);
        AiGapReport aiReport = structuredAiClient.generate(
                "你是严谨的技术面试教练。只根据用户资料分片判断能力，不得使用常识补全，也不得编造证据。",
                prompt(jobDescription, skills, evidenceBySkill, evidenceCatalog),
                AiGapReport.class,
                response -> valid(response, skills, evidenceBySkill, evidenceCatalog));
        Map<String, AiGapReport.Conclusion> conclusions = aiReport.getConclusions().stream()
                .collect(java.util.stream.Collectors.toMap(AiGapReport.Conclusion::getSkill, item -> item));
        for (String skill : skills) {
            AiGapReport.Conclusion conclusion = conclusions.get(skill);
            report.getGaps().add(conclusion == null ? unverified(skill) : toGapItem(conclusion, evidenceCatalog));
        }
        report.setInsufficientEvidence(report.getGaps().stream().allMatch(item -> "UNVERIFIED".equals(item.getVerdict())));
        report.setSummary(aiReport.getSummary());
        return report;
    }

    private Map<String, List<KnowledgeSearchResultVO>> retrieveEvidence(List<String> skills, String jobDescription, User loginUser) {
        Map<String, List<KnowledgeSearchResultVO>> values = new LinkedHashMap<>();
        for (String skill : skills) {
            values.put(skill, knowledgeDocumentService.search("岗位要求：" + skill + "。" + jobDescription, 4, loginUser));
        }
        return values;
    }

    private Map<String, KnowledgeSearchResultVO> catalog(Map<String, List<KnowledgeSearchResultVO>> evidenceBySkill) {
        Map<String, KnowledgeSearchResultVO> values = new LinkedHashMap<>();
        evidenceBySkill.values().stream().flatMap(Collection::stream).forEach(result ->
                values.putIfAbsent("E" + (values.size() + 1), result));
        return values;
    }

    private String prompt(String jobDescription, List<String> skills,
                          Map<String, List<KnowledgeSearchResultVO>> evidenceBySkill,
                          Map<String, KnowledgeSearchResultVO> evidenceCatalog) {
        StringBuilder text = new StringBuilder("目标岗位 JD：\n").append(jobDescription).append("\n\n待判断技能：")
                .append(String.join("、", skills)).append("\n\n可引用资料：\n");
        evidenceCatalog.forEach((id, result) -> text.append(id).append(" | ")
                .append(result.getDocumentName()).append(" | 分片 ").append(result.getChunkIndex() + 1)
                .append(" | ").append(result.getContent()).append("\n"));
        text.append("\n每个技能只可使用其检索结果：\n");
        evidenceBySkill.forEach((skill, results) -> text.append(skill).append(": ")
                .append(evidenceIdsFor(results, evidenceCatalog)).append("\n"));
        text.append("\n只返回 JSON：{\"summary\":\"...\",\"conclusions\":[{\"skill\":\"技能名\",\"verdict\":\"MATCHED|GAP|UNVERIFIED\",\"severity\":\"LOW|MEDIUM|HIGH|UNKNOWN\",\"recommendation\":\"...\",\"evidenceIds\":[\"E1\"]}]}. ")
                .append("MATCHED 和 GAP 必须至少引用一个允许的 evidenceId；没有足够依据时使用 UNVERIFIED。不得引用未给出的编号。");
        return text.toString();
    }

    private List<String> evidenceIdsFor(List<KnowledgeSearchResultVO> results, Map<String, KnowledgeSearchResultVO> catalog) {
        return catalog.entrySet().stream().filter(entry -> results.contains(entry.getValue())).map(Map.Entry::getKey).toList();
    }

    private boolean valid(AiGapReport report, List<String> skills,
                          Map<String, List<KnowledgeSearchResultVO>> evidenceBySkill,
                          Map<String, KnowledgeSearchResultVO> evidenceCatalog) {
        if (report == null || report.getSummary() == null || report.getSummary().isBlank()
                || report.getConclusions() == null || report.getConclusions().size() != skills.size()) {
            return false;
        }
        if (report.getConclusions().stream().map(AiGapReport.Conclusion::getSkill).collect(java.util.stream.Collectors.toSet()).size() != skills.size()) {
            return false;
        }
        return report.getConclusions().stream().allMatch(item -> {
            if (item == null || !skills.contains(item.getSkill()) || !VERDICTS.contains(item.getVerdict())
                    || !SEVERITIES.contains(item.getSeverity()) || item.getRecommendation() == null || item.getRecommendation().isBlank()) {
                return false;
            }
            List<String> allowedIds = evidenceIdsFor(evidenceBySkill.get(item.getSkill()), evidenceCatalog);
            List<String> citedIds = item.getEvidenceIds() == null ? List.of() : item.getEvidenceIds();
            return citedIds.stream().allMatch(allowedIds::contains)
                    && ("UNVERIFIED".equals(item.getVerdict()) || !citedIds.isEmpty());
        });
    }

    private GapAnalysisVO.GapItem toGapItem(AiGapReport.Conclusion conclusion,
                                             Map<String, KnowledgeSearchResultVO> evidenceCatalog) {
        GapAnalysisVO.GapItem item = new GapAnalysisVO.GapItem();
        item.setSkill(conclusion.getSkill());
        item.setVerdict(conclusion.getVerdict());
        item.setSeverity(conclusion.getSeverity());
        item.setRecommendation(conclusion.getRecommendation());
        List<String> evidenceIds = conclusion.getEvidenceIds() == null ? List.of() : conclusion.getEvidenceIds();
        item.setEvidenceAvailable(!evidenceIds.isEmpty());
        item.setEvidence(evidenceIds.stream().map(evidenceCatalog::get).map(this::toEvidence).toList());
        return item;
    }

    private GapAnalysisVO.GapItem unverified(String skill) {
        GapAnalysisVO.GapItem item = new GapAnalysisVO.GapItem();
        item.setSkill(skill);
        item.setVerdict("UNVERIFIED");
        item.setSeverity("UNKNOWN");
        item.setRecommendation("当前私有资料没有检索到足够证据，请补充该技能的项目实践或复习笔记后重新分析。");
        item.setEvidenceAvailable(false);
        item.setEvidence(List.of());
        return item;
    }

    private GapAnalysisVO.Evidence toEvidence(KnowledgeSearchResultVO result) {
        GapAnalysisVO.Evidence evidence = new GapAnalysisVO.Evidence();
        evidence.setDocumentId(result.getDocumentId());
        evidence.setDocumentName(result.getDocumentName());
        evidence.setChunkIndex(result.getChunkIndex());
        evidence.setContent(result.getContent());
        return evidence;
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
}
