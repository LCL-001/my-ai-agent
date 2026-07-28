package com.lcl.myaiagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.myaiagent.ai.LocalStructuredAiClient;
import com.lcl.myaiagent.interview.AiInterviewResponse;
import com.lcl.myaiagent.mapper.InterviewSessionMapper;
import com.lcl.myaiagent.mapper.InterviewTurnMapper;
import com.lcl.myaiagent.model.po.InterviewSession;
import com.lcl.myaiagent.model.po.InterviewTurn;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.service.impl.InterviewServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewServiceImplTest {

    @Test
    void startsWithAiGeneratedQuestion() {
        InterviewSessionMapper sessions = mock(InterviewSessionMapper.class);
        InterviewTurnMapper turns = mock(InterviewTurnMapper.class);
        KnowledgeDocumentService knowledge = mock(KnowledgeDocumentService.class);
        LocalStructuredAiClient aiClient = mock(LocalStructuredAiClient.class);
        when(knowledge.search(any(), eq(3), any())).thenReturn(List.of());
        when(aiClient.generate(any(), any(), eq(AiInterviewResponse.class), any())).thenReturn(openingQuestion());
        when(turns.selectList(any())).thenReturn(List.of());
        InterviewServiceImpl service = service(sessions, turns, knowledge, aiClient);

        service.start("Java 后端", 5, user());

        verify(sessions).insert(any(InterviewSession.class));
        verify(turns).insert(any(InterviewTurn.class));
    }

    @Test
    void savesAiFeedbackAndCreatesNextQuestion() {
        InterviewSessionMapper sessions = mock(InterviewSessionMapper.class);
        InterviewTurnMapper turns = mock(InterviewTurnMapper.class);
        KnowledgeDocumentService knowledge = mock(KnowledgeDocumentService.class);
        LocalStructuredAiClient aiClient = mock(LocalStructuredAiClient.class);
        InterviewSession session = session(2);
        InterviewTurn turn = askedTurn();
        when(sessions.selectOne(any())).thenReturn(session);
        when(turns.selectOne(any())).thenReturn(turn);
        when(knowledge.search(any(), eq(3), any())).thenReturn(List.of());
        when(aiClient.generate(any(), any(), eq(AiInterviewResponse.class), any())).thenReturn(evaluation());
        when(turns.selectList(any())).thenReturn(List.of(turn));
        InterviewServiceImpl service = service(sessions, turns, knowledge, aiClient);

        service.answer(session.getId(), "我会先定位事务边界。", user());

        assertEquals("ANSWERED", turn.getTurnStatus());
        assertEquals(82, turn.getScore());
        assertEquals("回答覆盖了事务传播。", turn.getFeedback());
        verify(turns).insert(any(InterviewTurn.class));
    }

    @Test
    void restoresAskedStateWhenModelFails() {
        InterviewSessionMapper sessions = mock(InterviewSessionMapper.class);
        InterviewTurnMapper turns = mock(InterviewTurnMapper.class);
        KnowledgeDocumentService knowledge = mock(KnowledgeDocumentService.class);
        LocalStructuredAiClient aiClient = mock(LocalStructuredAiClient.class);
        InterviewSession session = session(2);
        InterviewTurn turn = askedTurn();
        when(sessions.selectOne(any())).thenReturn(session);
        when(turns.selectOne(any())).thenReturn(turn);
        when(knowledge.search(any(), eq(3), any())).thenReturn(List.of());
        when(aiClient.generate(any(), any(), eq(AiInterviewResponse.class), any())).thenThrow(new RuntimeException("model offline"));
        InterviewServiceImpl service = service(sessions, turns, knowledge, aiClient);

        assertThrows(RuntimeException.class, () -> service.answer(session.getId(), "回答", user()));

        assertEquals("ASKED", turn.getTurnStatus());
        verify(turns, times(2)).updateById(turn);
    }

    private InterviewServiceImpl service(InterviewSessionMapper sessions, InterviewTurnMapper turns,
                                         KnowledgeDocumentService knowledge, LocalStructuredAiClient aiClient) {
        return new InterviewServiceImpl(sessions, turns, knowledge, aiClient, new ObjectMapper());
    }

    private User user() {
        User user = new User();
        user.setId("user-a");
        return user;
    }

    private InterviewSession session(int questionLimit) {
        InterviewSession session = new InterviewSession();
        session.setId("session-a");
        session.setUserId("user-a");
        session.setTopic("Java 后端");
        session.setQuestionLimit(questionLimit);
        session.setStatus("IN_PROGRESS");
        return session;
    }

    private InterviewTurn askedTurn() {
        InterviewTurn turn = new InterviewTurn();
        turn.setId("turn-a");
        turn.setSessionId("session-a");
        turn.setTurnNumber(1);
        turn.setQuestion("为什么 Spring 事务会失效？");
        turn.setTurnStatus("ASKED");
        return turn;
    }

    private AiInterviewResponse openingQuestion() {
        AiInterviewResponse response = new AiInterviewResponse();
        response.setQuestion("请说明 Spring 事务的传播机制。");
        response.setQuestionContext("考察事务边界与代理机制。");
        return response;
    }

    private AiInterviewResponse evaluation() {
        AiInterviewResponse response = new AiInterviewResponse();
        response.setScore(82);
        response.setFeedback("回答覆盖了事务传播。");
        response.setStrengths(List.of("能说明代理边界"));
        response.setWeaknesses(List.of("缺少自调用案例"));
        response.setPracticeTasks(List.of("复盘自调用导致事务失效的案例"));
        response.setNextQuestion("如何定位慢 SQL？");
        response.setNextQuestionContext("考察索引与执行计划。");
        return response;
    }
}
