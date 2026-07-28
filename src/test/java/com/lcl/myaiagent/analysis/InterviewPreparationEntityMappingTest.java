package com.lcl.myaiagent.analysis;

import com.baomidou.mybatisplus.annotation.TableField;
import com.lcl.myaiagent.model.po.GapAnalysis;
import com.lcl.myaiagent.model.po.InterviewSession;
import com.lcl.myaiagent.model.po.InterviewTurn;
import com.lcl.myaiagent.model.po.StudyPlan;
import com.lcl.myaiagent.model.po.StudyTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InterviewPreparationEntityMappingTest {

    @Test
    void mapsGapAnalysisAndStudyPlanPropertiesToSnakeCaseColumns() {
        assertColumn(GapAnalysis.class, "userId", "user_id");
        assertColumn(StudyPlan.class, "userId", "user_id");
        assertColumn(StudyPlan.class, "gapAnalysisId", "gap_analysis_id");
        assertColumn(StudyPlan.class, "confirmToken", "confirm_token");
        assertColumn(StudyPlan.class, "draftNote", "draft_note");
    }

    @Test
    void mapsStudyTaskAndInterviewPropertiesToSnakeCaseColumns() {
        assertColumn(StudyTask.class, "planId", "plan_id");
        assertColumn(StudyTask.class, "userId", "user_id");
        assertColumn(StudyTask.class, "dayNumber", "day_number");
        assertColumn(StudyTask.class, "taskStatus", "task_status");
        assertColumn(StudyTask.class, "scheduledDate", "scheduled_date");
        assertColumn(InterviewSession.class, "userId", "user_id");
        assertColumn(InterviewSession.class, "questionLimit", "question_limit");
        assertColumn(InterviewTurn.class, "sessionId", "session_id");
        assertColumn(InterviewTurn.class, "turnNumber", "turn_number");
        assertColumn(InterviewTurn.class, "questionContext", "question_context");
        assertColumn(InterviewTurn.class, "feedbackDetail", "feedback_detail");
        assertColumn(InterviewTurn.class, "evidenceDetail", "evidence_detail");
        assertColumn(InterviewTurn.class, "turnStatus", "turn_status");
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
