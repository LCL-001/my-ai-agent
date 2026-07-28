import api from './client'

export const createGapAnalysis = (jobDescription) => api.post('/gap-analyses', { jobDescription })
export const listGapAnalyses = () => api.get('/gap-analyses')
