import api from './client'

export function listKnowledgeDocuments() {
  return api.get('/knowledge/documents')
}

export function uploadKnowledgeDocument(file, type) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return api.post('/knowledge/documents', formData)
}

export function deleteKnowledgeDocument(documentId) {
  return api.delete(`/knowledge/documents/${documentId}`)
}

export function listKnowledgeChunks(documentId) {
  return api.get(`/knowledge/documents/${documentId}/chunks`)
}

export function reindexKnowledgeDocument(documentId) {
  return api.post(`/knowledge/documents/${documentId}/reindex`)
}

export function searchKnowledge(query) {
  return api.get('/knowledge/documents/search', { params: { query, limit: 5 } })
}
