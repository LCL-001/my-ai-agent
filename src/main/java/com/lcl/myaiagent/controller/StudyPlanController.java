package com.lcl.myaiagent.controller;

import com.lcl.myaiagent.common.BaseResponse;
import com.lcl.myaiagent.common.ResultUtils;
import com.lcl.myaiagent.model.dto.StudyPlanDraftRequest;
import com.lcl.myaiagent.model.dto.StudyTaskUpdateRequest;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.StudyPlanVO;
import com.lcl.myaiagent.service.StudyPlanService;
import com.lcl.myaiagent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {
    private final StudyPlanService studyPlanService;
    private final UserService userService;

    @PostMapping("/drafts")
    public BaseResponse<StudyPlanVO> createDraft(@RequestBody StudyPlanDraftRequest draftRequest, HttpServletRequest request) {
        return ResultUtils.success(studyPlanService.createDraft(draftRequest.getGapAnalysisId(), draftRequest.getTitle(), userService.getLoginUser(request)));
    }
    @PostMapping("/{planId}/confirm")
    public BaseResponse<StudyPlanVO> confirm(@PathVariable String planId, HttpServletRequest request) {
        return ResultUtils.success(studyPlanService.confirm(planId, userService.getLoginUser(request)));
    }
    @PutMapping("/{planId}/tasks/{taskId}")
    public BaseResponse<StudyPlanVO> updateTask(@PathVariable String planId, @PathVariable String taskId, @RequestBody StudyTaskUpdateRequest updateRequest, HttpServletRequest request) {
        StudyPlanVO.Task task = new StudyPlanVO.Task();
        task.setTitle(updateRequest.getTitle()); task.setDescription(updateRequest.getDescription()); task.setTaskStatus(updateRequest.getTaskStatus()); task.setScheduledDate(updateRequest.getScheduledDate());
        return ResultUtils.success(studyPlanService.updateTask(planId, taskId, task, userService.getLoginUser(request)));
    }
    @GetMapping
    public BaseResponse<List<StudyPlanVO>> list(HttpServletRequest request) {
        return ResultUtils.success(studyPlanService.list(userService.getLoginUser(request)));
    }
}
