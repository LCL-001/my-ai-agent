package com.lcl.myaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.myaiagent.common.ErrorCode;
import com.lcl.myaiagent.exception.BusinessException;
import com.lcl.myaiagent.mapper.GapAnalysisMapper;
import com.lcl.myaiagent.mapper.StudyPlanMapper;
import com.lcl.myaiagent.mapper.StudyTaskMapper;
import com.lcl.myaiagent.model.po.GapAnalysis;
import com.lcl.myaiagent.model.po.StudyPlan;
import com.lcl.myaiagent.model.po.StudyTask;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.GapAnalysisVO;
import com.lcl.myaiagent.model.vo.StudyPlanVO;
import com.lcl.myaiagent.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudyPlanServiceImpl implements StudyPlanService {

    private final StudyPlanMapper studyPlanMapper;
    private final StudyTaskMapper studyTaskMapper;
    private final GapAnalysisMapper gapAnalysisMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudyPlanVO createDraft(String gapAnalysisId, String title, User loginUser) {
        GapAnalysis analysis = gapAnalysisMapper.selectOne(new LambdaQueryWrapper<GapAnalysis>()
                .eq(GapAnalysis::getId, gapAnalysisId).eq(GapAnalysis::getUserId, loginUser.getId()));
        if (analysis == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分析报告不存在或无权使用");
        }
        GapAnalysisVO report = readReport(analysis);
        StudyPlan plan = new StudyPlan();
        plan.setId(UUID.randomUUID().toString());
        plan.setUserId(loginUser.getId());
        plan.setGapAnalysisId(analysis.getId());
        plan.setTitle(title == null || title.isBlank() ? "7 天面试补弱计划" : title.trim());
        plan.setStatus("DRAFT");
        plan.setConfirmToken(UUID.randomUUID().toString());
        plan.setDraftNote(report.getSummary());
        studyPlanMapper.insert(plan);
        List<String> skills = report.getGaps() == null || report.getGaps().isEmpty()
                ? List.of("项目复盘", "Java 基础", "Spring Boot", "MySQL", "Redis", "JVM", "模拟面试")
                : report.getGaps().stream().map(GapAnalysisVO.GapItem::getSkill).toList();
        for (int day = 1; day <= 7; day++) {
            String skill = skills.get((day - 1) % skills.size());
            StudyTask task = new StudyTask();
            task.setId(UUID.randomUUID().toString());
            task.setPlanId(plan.getId());
            task.setUserId(loginUser.getId());
            task.setDayNumber(day);
            task.setTitle("Day " + day + " · " + skill);
            task.setDescription("整理 " + skill + " 的知识点，补一份可复述的项目或八股证据，并完成 20 分钟自测。");
            task.setTaskStatus("TODO");
            studyTaskMapper.insert(task);
        }
        return toPlanVO(plan, tasks(plan.getId(), loginUser.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudyPlanVO confirm(String planId, User loginUser) {
        StudyPlan plan = plan(planId, loginUser);
        if ("DRAFT".equals(plan.getStatus())) {
            plan.setStatus("CONFIRMED");
            studyPlanMapper.updateById(plan);
            List<StudyTask> tasks = tasks(planId, loginUser.getId());
            for (StudyTask task : tasks) {
                if (task.getScheduledDate() == null) {
                    task.setScheduledDate(LocalDate.now().plusDays(task.getDayNumber() - 1L));
                    studyTaskMapper.updateById(task);
                }
            }
        }
        return toPlanVO(plan, tasks(planId, loginUser.getId()));
    }

    @Override
    public StudyPlanVO updateTask(String planId, String taskId, StudyPlanVO.Task update, User loginUser) {
        StudyPlan plan = plan(planId, loginUser);
        StudyTask task = studyTaskMapper.selectOne(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, taskId).eq(StudyTask::getPlanId, plan.getId()).eq(StudyTask::getUserId, loginUser.getId()));
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在或无权操作");
        }
        if (update.getTitle() != null && !update.getTitle().isBlank()) task.setTitle(update.getTitle().trim());
        if (update.getDescription() != null) task.setDescription(update.getDescription());
        if (update.getScheduledDate() != null) task.setScheduledDate(update.getScheduledDate());
        if (update.getTaskStatus() != null) {
            if (!List.of("TODO", "DONE", "SKIPPED").contains(update.getTaskStatus())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的任务状态");
            }
            task.setTaskStatus(update.getTaskStatus());
        }
        studyTaskMapper.updateById(task);
        return toPlanVO(plan, tasks(planId, loginUser.getId()));
    }

    @Override
    public List<StudyPlanVO> list(User loginUser) {
        return studyPlanMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, loginUser.getId()).orderByDesc(StudyPlan::getCreateTime))
                .stream().map(plan -> toPlanVO(plan, tasks(plan.getId(), loginUser.getId()))).toList();
    }

    private StudyPlan plan(String planId, User loginUser) {
        StudyPlan plan = studyPlanMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, planId).eq(StudyPlan::getUserId, loginUser.getId()));
        if (plan == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "计划不存在或无权操作");
        return plan;
    }

    private List<StudyTask> tasks(String planId, String userId) {
        return studyTaskMapper.selectList(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getPlanId, planId).eq(StudyTask::getUserId, userId).orderByAsc(StudyTask::getDayNumber));
    }

    private StudyPlanVO toPlanVO(StudyPlan plan, List<StudyTask> tasks) {
        StudyPlanVO valueObject = new StudyPlanVO();
        valueObject.setId(plan.getId()); valueObject.setGapAnalysisId(plan.getGapAnalysisId()); valueObject.setTitle(plan.getTitle());
        valueObject.setStatus(plan.getStatus()); valueObject.setDraftNote(plan.getDraftNote()); valueObject.setCreateTime(plan.getCreateTime());
        valueObject.setTasks(tasks.stream().map(task -> { StudyPlanVO.Task value = new StudyPlanVO.Task(); value.setId(task.getId()); value.setDayNumber(task.getDayNumber()); value.setTitle(task.getTitle()); value.setDescription(task.getDescription()); value.setTaskStatus(task.getTaskStatus()); value.setScheduledDate(task.getScheduledDate()); return value; }).toList());
        return valueObject;
    }

    private GapAnalysisVO readReport(GapAnalysis analysis) {
        try { return objectMapper.readValue(analysis.getContent(), GapAnalysisVO.class); }
        catch (JsonProcessingException exception) { throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分析报告无法读取"); }
    }
}
