package com.lcl.myaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.myaiagent.ai.LocalStructuredAiClient;
import com.lcl.myaiagent.common.ErrorCode;
import com.lcl.myaiagent.exception.BusinessException;
import com.lcl.myaiagent.interview.AiInterviewResponse;
import com.lcl.myaiagent.mapper.InterviewSessionMapper;
import com.lcl.myaiagent.mapper.InterviewTurnMapper;
import com.lcl.myaiagent.model.po.InterviewSession;
import com.lcl.myaiagent.model.po.InterviewTurn;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.InterviewSessionVO;
import com.lcl.myaiagent.model.vo.KnowledgeSearchResultVO;
import com.lcl.myaiagent.service.InterviewService;
import com.lcl.myaiagent.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionMapper sessions;
    private final InterviewTurnMapper turns;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final LocalStructuredAiClient structuredAiClient;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewSessionVO start(String topic, Integer limit, User user) {
        InterviewSession session = new InterviewSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setTopic(topic == null || topic.isBlank() ? "Java 后端" : topic.trim());
        session.setQuestionLimit(Math.min(Math.max(limit == null ? 5 : limit, 1), 5));
        session.setStatus("IN_PROGRESS");

        List<KnowledgeSearchResultVO> evidence = retrieveEvidence(session.getTopic(), user);
        AiInterviewResponse response = createOpeningQuestion(session, evidence);
        sessions.insert(session);
        turns.insert(questionTurn(session, 1, response.getQuestion(), response.getQuestionContext(), evidence));
        return view(session);
    }

    @Override
    public InterviewSessionVO answer(String sessionId, String answer, User user) {
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先提交回答");
        }
        InterviewSession session = session(sessionId, user);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前面试不是可答题状态");
        }
        InterviewTurn turn = currentAskedTurn(sessionId);
        if (turn == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前没有待回答的问题，可能正在生成反馈");
        }

        turn.setTurnStatus("EVALUATING");
        turns.updateById(turn);
        boolean needsNextQuestion = turn.getTurnNumber() < session.getQuestionLimit();
        try {
            List<KnowledgeSearchResultVO> evidence = retrieveEvidence(session.getTopic() + " " + turn.getQuestion(), user);
            AiInterviewResponse response = evaluateAnswer(session, turn, answer.trim(), evidence, needsNextQuestion);
            persistEvaluation(session, turn, answer.trim(), response, evidence, needsNextQuestion);
            return view(session);
        } catch (RuntimeException exception) {
            turn.setTurnStatus("ASKED");
            turns.updateById(turn);
            throw exception;
        }
    }

    @Override
    public InterviewSessionVO pause(String sessionId, User user) {
        InterviewSession session = session(sessionId, user);
        Long evaluatingCount = turns.selectCount(new LambdaQueryWrapper<InterviewTurn>()
                .eq(InterviewTurn::getSessionId, sessionId)
                .eq(InterviewTurn::getTurnStatus, "EVALUATING"));
        if (evaluatingCount != null && evaluatingCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "正在生成本题反馈，请稍候再暂停");
        }
        if ("IN_PROGRESS".equals(session.getStatus())) {
            session.setStatus("PAUSED");
            sessions.updateById(session);
        }
        return view(session);
    }

    @Override
    public InterviewSessionVO resume(String sessionId, User user) {
        InterviewSession session = session(sessionId, user);
        if ("PAUSED".equals(session.getStatus())) {
            session.setStatus("IN_PROGRESS");
            sessions.updateById(session);
        }
        return view(session);
    }

    @Override
    public InterviewSessionVO get(String sessionId, User user) {
        return view(session(sessionId, user));
    }

    @Override
    public List<InterviewSessionVO> list(User user) {
        return sessions.selectList(new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, user.getId())
                        .orderByDesc(InterviewSession::getCreateTime))
                .stream().map(this::view).toList();
    }

    private AiInterviewResponse createOpeningQuestion(InterviewSession session, List<KnowledgeSearchResultVO> evidence) {
        return structuredAiClient.generate(
                "你是严谨的 Java 后端实习面试官。根据主题与候选人资料提出一题可回答的面试题，不要给答案。",
                questionPrompt(session.getTopic(), evidence),
                AiInterviewResponse.class,
                response -> hasText(response.getQuestion()) && hasText(response.getQuestionContext()));
    }

    private AiInterviewResponse evaluateAnswer(InterviewSession session, InterviewTurn turn, String answer,
                                               List<KnowledgeSearchResultVO> evidence, boolean needsNextQuestion) {
        return structuredAiClient.generate(
                "你是严谨但鼓励性的 Java 后端实习面试官。评分必须基于候选人的本次回答；不得编造候选人的项目经历。",
                evaluationPrompt(session, turn, answer, evidence, needsNextQuestion),
                AiInterviewResponse.class,
                response -> validEvaluation(response, needsNextQuestion));
    }

    private void persistEvaluation(InterviewSession session, InterviewTurn turn, String answer,
                                   AiInterviewResponse response, List<KnowledgeSearchResultVO> evidence,
                                   boolean needsNextQuestion) {
        turn.setAnswer(answer);
        turn.setScore(response.getScore());
        turn.setFeedback(response.getFeedback());
        turn.setFeedbackDetail(writeJson(response));
        turn.setEvidenceDetail(writeJson(toEvidence(evidence)));
        turn.setTurnStatus("ANSWERED");
        turns.updateById(turn);
        if (needsNextQuestion) {
            turns.insert(questionTurn(session, turn.getTurnNumber() + 1, response.getNextQuestion(),
                    response.getNextQuestionContext(), evidence));
        } else {
            session.setStatus("COMPLETED");
            sessions.updateById(session);
        }
    }

    private InterviewTurn questionTurn(InterviewSession session, int number, String question,
                                       String context, List<KnowledgeSearchResultVO> evidence) {
        InterviewTurn turn = new InterviewTurn();
        turn.setId(UUID.randomUUID().toString());
        turn.setSessionId(session.getId());
        turn.setTurnNumber(number);
        turn.setQuestion(question);
        turn.setQuestionContext(context);
        turn.setEvidenceDetail(writeJson(toEvidence(evidence)));
        turn.setTurnStatus("ASKED");
        return turn;
    }

    private InterviewSession session(String sessionId, User user) {
        InterviewSession session = sessions.selectOne(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getId, sessionId)
                .eq(InterviewSession::getUserId, user.getId()));
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "面试不存在或无权访问");
        }
        return session;
    }

    private InterviewTurn currentAskedTurn(String sessionId) {
        return turns.selectOne(new LambdaQueryWrapper<InterviewTurn>()
                .eq(InterviewTurn::getSessionId, sessionId)
                .eq(InterviewTurn::getTurnStatus, "ASKED")
                .orderByDesc(InterviewTurn::getTurnNumber)
                .last("limit 1"));
    }

    private List<KnowledgeSearchResultVO> retrieveEvidence(String query, User user) {
        return knowledgeDocumentService.search(query, 3, user);
    }

    private String questionPrompt(String topic, List<KnowledgeSearchResultVO> evidence) {
        return "面试主题：" + topic + "\n候选人资料：\n" + evidenceText(evidence)
                + "\n只返回 JSON：{\"question\":\"...\",\"questionContext\":\"本题考察点\"}。";
    }

    private String evaluationPrompt(InterviewSession session, InterviewTurn turn, String answer,
                                    List<KnowledgeSearchResultVO> evidence, boolean needsNextQuestion) {
        return "面试主题：" + session.getTopic() + "\n当前问题：" + turn.getQuestion()
                + "\n候选人回答：" + answer + "\n可参考的候选人资料：\n" + evidenceText(evidence)
                + "\n只返回 JSON：{\"score\":0-100,\"feedback\":\"总体点评\",\"strengths\":[\"优点\"],\"weaknesses\":[\"缺失要点\"],\"practiceTasks\":[\"补弱任务\"],\"nextQuestion\":\"下一题或空字符串\",\"nextQuestionContext\":\"下一题考察点或空字符串\"}。"
                + (needsNextQuestion ? "本轮未结束，nextQuestion 与 nextQuestionContext 必须非空并可追问当前弱点。" : "这是最后一题，nextQuestion 与 nextQuestionContext 必须为空字符串。");
    }

    private String evidenceText(List<KnowledgeSearchResultVO> evidence) {
        if (evidence.isEmpty()) {
            return "无可用资料；不要推断候选人经历。";
        }
        return evidence.stream().map(item -> item.getDocumentName() + " 分片 " + (item.getChunkIndex() + 1) + "：" + item.getContent())
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private boolean validEvaluation(AiInterviewResponse response, boolean needsNextQuestion) {
        if (response == null || response.getScore() == null || response.getScore() < 0 || response.getScore() > 100
                || !hasText(response.getFeedback()) || response.getStrengths() == null || response.getWeaknesses() == null
                || response.getPracticeTasks() == null) {
            return false;
        }
        return needsNextQuestion
                ? hasText(response.getNextQuestion()) && hasText(response.getNextQuestionContext())
                : !hasText(response.getNextQuestion()) && !hasText(response.getNextQuestionContext());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "面试反馈无法保存");
        }
    }

    private InterviewSessionVO view(InterviewSession session) {
        InterviewSessionVO value = new InterviewSessionVO();
        value.setId(session.getId());
        value.setTopic(session.getTopic());
        value.setStatus(session.getStatus());
        value.setQuestionLimit(session.getQuestionLimit());
        value.setCreateTime(session.getCreateTime());
        value.setTurns(turns.selectList(new LambdaQueryWrapper<InterviewTurn>()
                        .eq(InterviewTurn::getSessionId, session.getId())
                        .orderByAsc(InterviewTurn::getTurnNumber))
                .stream().map(this::toTurn).toList());
        return value;
    }

    private InterviewSessionVO.Turn toTurn(InterviewTurn turn) {
        InterviewSessionVO.Turn value = new InterviewSessionVO.Turn();
        value.setTurnNumber(turn.getTurnNumber());
        value.setQuestion(turn.getQuestion());
        value.setQuestionContext(turn.getQuestionContext());
        value.setAnswer(turn.getAnswer());
        value.setFeedback(turn.getFeedback());
        value.setScore(turn.getScore());
        value.setTurnStatus(turn.getTurnStatus());
        if (turn.getFeedbackDetail() != null) {
            try {
                AiInterviewResponse feedback = objectMapper.readValue(turn.getFeedbackDetail(), AiInterviewResponse.class);
                value.setStrengths(feedback.getStrengths());
                value.setWeaknesses(feedback.getWeaknesses());
                value.setPracticeTasks(feedback.getPracticeTasks());
            } catch (JsonProcessingException ignored) {
                value.setStrengths(List.of());
                value.setWeaknesses(List.of());
                value.setPracticeTasks(List.of());
            }
        }
        value.setEvidence(readEvidence(turn.getEvidenceDetail()));
        return value;
    }

    private List<InterviewSessionVO.Evidence> toEvidence(List<KnowledgeSearchResultVO> results) {
        List<InterviewSessionVO.Evidence> evidence = new ArrayList<>();
        for (KnowledgeSearchResultVO result : results) {
            InterviewSessionVO.Evidence item = new InterviewSessionVO.Evidence();
            item.setDocumentId(result.getDocumentId());
            item.setDocumentName(result.getDocumentName());
            item.setChunkIndex(result.getChunkIndex());
            item.setContent(result.getContent());
            evidence.add(item);
        }
        return evidence;
    }

    private List<InterviewSessionVO.Evidence> readEvidence(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }
}
