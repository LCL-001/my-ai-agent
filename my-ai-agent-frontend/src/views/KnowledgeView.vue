<script setup>
import { computed, onMounted, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { deleteKnowledgeDocument, listKnowledgeDocuments, searchKnowledge, uploadKnowledgeDocument } from '@/api/knowledge'
import { BookOpen, FileSearch, FileUp, Search, Trash2, X } from 'lucide-vue-next'

const documents = ref([])
const selectedFile = ref(null)
const selectedType = ref('RESUME')
const uploading = ref(false)
const searchQuery = ref('')
const searchResults = ref([])
const searching = ref(false)
const errorMessage = ref('')

const typeOptions = [
  { value: 'RESUME', label: '简历' },
  { value: 'JOB_DESCRIPTION', label: '岗位 JD' },
  { value: 'NOTE', label: '复习笔记' },
  { value: 'QUESTION_SET', label: '题库' },
]

const readyDocuments = computed(() => documents.value.filter((document) => document.status === 'READY').length)

async function loadDocuments() {
  const response = await listKnowledgeDocuments()
  documents.value = response.data.data || []
}

function chooseFile(event) {
  selectedFile.value = event.target.files?.[0] || null
  errorMessage.value = ''
}

async function uploadDocument() {
  if (!selectedFile.value) {
    errorMessage.value = '先选择一份资料，再开始入库。'
    return
  }
  uploading.value = true
  errorMessage.value = ''
  try {
    await uploadKnowledgeDocument(selectedFile.value, selectedType.value)
    selectedFile.value = null
    document.querySelector('#knowledge-file').value = ''
    await loadDocuments()
  } catch (error) {
    errorMessage.value = error.message || '资料入库失败，请检查 PgVector 服务。'
  } finally {
    uploading.value = false
  }
}

async function removeDocument(documentId) {
  await deleteKnowledgeDocument(documentId)
  await loadDocuments()
}

async function runSearch() {
  if (!searchQuery.value.trim()) return
  searching.value = true
  errorMessage.value = ''
  try {
    const response = await searchKnowledge(searchQuery.value.trim())
    searchResults.value = response.data.data || []
  } catch (error) {
    errorMessage.value = error.message || '检索失败，请稍后重试。'
  } finally {
    searching.value = false
  }
}

onMounted(loadDocuments)
</script>

<template>
  <AppLayout>
    <section class="knowledge-workspace">
      <header class="workspace-header">
        <div>
          <p class="eyebrow">PRIVATE STUDY ARCHIVE</p>
          <h1>把资料变成可追溯的准备依据</h1>
          <p>上传简历、JD 与笔记；后续分析和模拟面试只引用属于你的资料。</p>
        </div>
        <div class="archive-count"><strong>{{ readyDocuments }}</strong><span>份已就绪资料</span></div>
      </header>

      <div class="workspace-grid">
        <section class="panel upload-panel">
          <div class="panel-heading"><FileUp :size="19" /><h2>入库新资料</h2></div>
          <label class="file-drop" for="knowledge-file">
            <BookOpen :size="29" />
            <strong>{{ selectedFile?.name || '选择 PDF、DOCX、Markdown 或 TXT' }}</strong>
            <span>最大 10MB，资料仅归当前账号使用</span>
          </label>
          <input id="knowledge-file" type="file" accept=".pdf,.docx,.md,.markdown,.txt" @change="chooseFile" />
          <div class="type-picker">
            <button v-for="option in typeOptions" :key="option.value" :class="{ active: selectedType === option.value }" @click="selectedType = option.value">
              {{ option.label }}
            </button>
          </div>
          <button class="primary-action" :disabled="uploading" @click="uploadDocument">{{ uploading ? '正在解析并向量化…' : '加入私有资料库' }}</button>
          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        </section>

        <section class="panel search-panel">
          <div class="panel-heading"><FileSearch :size="19" /><h2>检索验证</h2></div>
          <p class="panel-copy">先验证资料能否命中，再用它生成能力分析。</p>
          <div class="search-box">
            <input v-model="searchQuery" placeholder="例如：我简历中有哪些 Redis 实践？" @keyup.enter="runSearch" />
            <button :disabled="searching" @click="runSearch"><Search :size="18" /></button>
          </div>
          <div v-if="searchResults.length" class="result-list">
            <article v-for="result in searchResults" :key="`${result.documentId}-${result.chunkIndex}`" class="result-card">
              <div><span>{{ result.documentType }}</span><small>{{ result.documentName }} · 片段 {{ result.chunkIndex + 1 }}</small></div>
              <p>{{ result.content }}</p>
            </article>
          </div>
          <div v-else class="empty-search">输入一个问题，检查私有知识库的引用来源。</div>
        </section>
      </div>

      <section class="panel document-panel">
        <div class="panel-heading"><BookOpen :size="19" /><h2>我的资料</h2></div>
        <div v-if="documents.length" class="document-table">
          <article v-for="documentItem in documents" :key="documentItem.id" class="document-row">
            <div class="document-name"><strong>{{ documentItem.name }}</strong><span>{{ documentItem.documentType }}</span></div>
            <div class="document-status" :class="documentItem.status.toLowerCase()">{{ documentItem.status === 'READY' ? `${documentItem.chunkCount} 个检索片段` : documentItem.status }}</div>
            <button class="icon-button" title="删除资料" @click="removeDocument(documentItem.id)"><Trash2 :size="17" /></button>
          </article>
        </div>
        <div v-else class="empty-documents">第一份资料通常从简历开始。上传后即可验证检索结果。</div>
      </section>
    </section>
  </AppLayout>
</template>

<style scoped>
.knowledge-workspace { min-height: 100%; overflow-y: auto; padding: 42px clamp(24px, 5vw, 72px) 64px; background: radial-gradient(circle at 78% -8%, #dbeafe 0, transparent 31%), #f8fafc; }
.workspace-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 30px; max-width: 1180px; margin: 0 auto 30px; }
.eyebrow { color: #2563eb; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11px; letter-spacing: .14em; font-weight: 700; }
h1 { margin: 7px 0 8px; font-size: clamp(29px, 4vw, 46px); letter-spacing: -.045em; line-height: 1.08; max-width: 670px; }
.workspace-header p:not(.eyebrow) { color: var(--text-secondary); font-size: 15px; }
.archive-count { min-width: 145px; padding: 16px 18px; background: #0f172a; color: #fff; border-radius: 4px 22px 4px 22px; display: grid; gap: 1px; }
.archive-count strong { font-size: 30px; line-height: 1; }.archive-count span { color: #bfdbfe; font-size: 12px; }
.workspace-grid { max-width: 1180px; margin: 0 auto 20px; display: grid; grid-template-columns: minmax(280px, .85fr) minmax(360px, 1.15fr); gap: 20px; }
.panel { background: rgba(255,255,255,.93); border: 1px solid #dbe3ee; box-shadow: 0 16px 35px rgba(15,23,42,.05); border-radius: 14px; padding: 24px; }
.panel-heading { display: flex; align-items: center; gap: 9px; color: #1d4ed8; }.panel-heading h2 { color: var(--text); font-size: 16px; }
.file-drop { margin: 20px 0 14px; min-height: 154px; border: 1.5px dashed #93c5fd; background: #eff6ff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; text-align: center; padding: 18px; cursor: pointer; transition: .2s; }
.file-drop:hover { background: #dbeafe; border-color: #2563eb; }.file-drop strong { max-width: 280px; word-break: break-word; }.file-drop span,.panel-copy { color: var(--text-secondary); font-size: 12px; } #knowledge-file { display: none; }
.type-picker { display: flex; flex-wrap: wrap; gap: 7px; margin-bottom: 16px; }.type-picker button { border: 1px solid #dbe3ee; color: #64748b; border-radius: 999px; padding: 6px 10px; font-size: 12px; }.type-picker button.active { background: #1d4ed8; border-color: #1d4ed8; color: #fff; }
.primary-action { width: 100%; background: #0f172a; color: #fff; padding: 11px; border-radius: 7px; font-weight: 650; }.primary-action:disabled { opacity: .65; cursor: progress; }.error-text { margin-top: 10px; color: var(--danger); font-size: 12px; }
.panel-copy { margin: 16px 0 12px; }.search-box { display: flex; background: #f1f5f9; border: 1px solid #dbe3ee; padding: 4px; }.search-box input { min-width: 0; flex: 1; padding: 9px 10px; }.search-box button { width: 37px; background: #1d4ed8; color: #fff; display: grid; place-items: center; }.result-list { margin-top: 13px; max-height: 250px; overflow-y: auto; display: grid; gap: 8px; }.result-card { border-left: 3px solid #60a5fa; background: #f8fafc; padding: 10px 12px; }.result-card div { display: flex; justify-content: space-between; gap: 8px; }.result-card span { color: #1d4ed8; font-size: 10px; font-weight: 700; }.result-card small { color: var(--text-muted); font-size: 10px; }.result-card p { margin-top: 6px; font-size: 12px; color: #475569; white-space: pre-wrap; }.empty-search,.empty-documents { color: var(--text-muted); font-size: 13px; padding: 34px 0; text-align: center; }
.document-panel { max-width: 1180px; margin: 0 auto; }.document-table { margin-top: 15px; display: grid; }.document-row { display: grid; grid-template-columns: 1fr auto 34px; align-items: center; gap: 15px; padding: 13px 0; border-top: 1px solid #e2e8f0; }.document-name { display: grid; gap: 2px; }.document-name strong { font-size: 14px; }.document-name span { color: #64748b; font-size: 11px; font-family: ui-monospace, monospace; }.document-status { color: #64748b; font-size: 12px; }.document-status.ready { color: #059669; }.document-status.failed { color: #dc2626; }.icon-button { color: #94a3b8; padding: 7px; }.icon-button:hover { color: #dc2626; background: #fff1f2; }
@media (max-width: 800px) { .workspace-header { display: grid; }.workspace-grid { grid-template-columns: 1fr; }.knowledge-workspace { padding: 28px 18px; }.archive-count { width: max-content; } }
</style>
