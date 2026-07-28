package com.lcl.myaiagent.service;

import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.StudyPlanVO;

import java.util.List;

public interface StudyPlanService {
    StudyPlanVO createDraft(String gapAnalysisId, String title, User loginUser);
    StudyPlanVO confirm(String planId, User loginUser);
    StudyPlanVO updateTask(String planId, String taskId, StudyPlanVO.Task task, User loginUser);
    List<StudyPlanVO> list(User loginUser);
}
