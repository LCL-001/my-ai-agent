import api from './client'

export const listStudyPlans = () => api.get('/study-plans')
export const listGapAnalyses = () => api.get('/gap-analyses')
export const createStudyPlanDraft = (gapAnalysisId, title) => api.post('/study-plans/drafts', { gapAnalysisId, title })
export const confirmStudyPlan = (planId) => api.post(`/study-plans/${planId}/confirm`)
export const updateStudyTask = (planId, taskId, task) => api.put(`/study-plans/${planId}/tasks/${taskId}`, task)
