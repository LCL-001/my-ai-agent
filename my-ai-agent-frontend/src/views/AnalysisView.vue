<template>
  <AppLayout>
    <main class="analysis-page">
      <header class="page-header">
        <p class="eyebrow">EVIDENCE-BASED REVIEW</p>
        <h1>能力差距分析</h1>
        <p>粘贴目标 JD。系统只会使用你私有资料库中检索到的分片生成结论；缺少依据时会明确标注，不会编造来源。</p>
      </header>

      <section class="input-panel">
        <label for="jd">目标岗位 JD</label>
        <textarea id="jd" v-model="jobDescription" placeholder="粘贴 Java 后端实习岗位的职责与要求" />
        <button :disabled="loading || !jobDescription.trim()" @click="runAnalysis">
          {{ loading ? '正在检索资料并生成分析…' : '生成有证据的分析' }}
        </button>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      </section>

      <section v-if="report" class="report-panel">
        <div class="report-title"><span>REPORT</span><time>{{ formatDate(report.createTime) }}</time></div>
        <p class="summary" :class="{ warning: report.insufficientEvidence }">{{ report.summary }}</p>
        <div v-if="report.gaps?.length" class="gap-list">
          <article v-for="gap in report.gaps" :key="gap.skill" class="gap-item" :class="gap.verdict?.toLowerCase()">
            <div class="skill">
              <strong>{{ gap.skill }}</strong>
              <div class="badges"><span>{{ verdictLabel(gap.verdict) }}</span><small>{{ severityLabel(gap.severity) }}</small></div>
            </div>
            <p>{{ gap.recommendation }}</p>
            <div v-if="gap.evidenceAvailable && gap.evidence?.length" class="evidence-list">
              <div v-for="evidence in gap.evidence" :key="`${evidence.documentId}-${evidence.chunkIndex}`" class="evidence">
                <div class="evidence-heading">
                  <small>{{ evidence.documentName }} · 分片 {{ evidence.chunkIndex + 1 }}</small>
                  <RouterLink :to="{ path: '/prepare', query: { documentId: evidence.documentId } }">查看资料</RouterLink>
                </div>
                <blockquote>{{ evidence.content }}</blockquote>
              </div>
            </div>
            <p v-else class="no-evidence">没有可验证的私有资料证据，请补充材料后重试。</p>
          </article>
        </div>
      </section>
    </main>
  </AppLayout>
</template>

<script setup>
import { ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { createGapAnalysis } from '../api/analysis'

const jobDescription = ref('')
const report = ref(null)
const loading = ref(false)
const errorMessage = ref('')

async function runAnalysis() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await createGapAnalysis(jobDescription.value)
    report.value = response.data.data
  } catch (error) {
    errorMessage.value = error.message || '分析失败，请检查本地模型和资料库后重试。'
  } finally {
    loading.value = false
  }
}

const formatDate = (value) => value ? new Date(value).toLocaleString('zh-CN') : '刚刚生成'
const verdictLabel = (value) => ({ MATCHED: '已验证匹配', GAP: '明确差距', UNVERIFIED: '证据不足' })[value] || '待判断'
const severityLabel = (value) => ({ LOW: '低优先级', MEDIUM: '中优先级', HIGH: '高优先级', UNKNOWN: '待补充' })[value] || '待补充'
</script>

<style scoped>
.analysis-page { max-width: 1060px; margin: 0 auto; padding: 48px 36px 88px; color: #102a43; }
.page-header { max-width: 750px; margin-bottom: 30px; }.eyebrow { color: #e1672d; letter-spacing: .18em; font-size: 12px; font-weight: 800; }
h1 { font-family: Georgia, 'Noto Serif SC', serif; font-size: 42px; margin: 8px 0 12px; }.page-header > p:last-child { color: #627d98; line-height: 1.75; }
.input-panel, .report-panel { background: #fffdf7; border: 1px solid #c8d8e5; box-shadow: 8px 8px 0 #d9e9f3; padding: 25px; }.input-panel { display: grid; gap: 12px; }
label, .report-title { font-size: 12px; letter-spacing: .12em; font-weight: 800; } textarea { min-height: 170px; resize: vertical; border: 1px solid #9fb3c8; background: #f7fbfd; padding: 14px; font: inherit; line-height: 1.65; }
button { justify-self: start; border: 0; padding: 11px 18px; background: #0b5cab; color: white; font-weight: 700; cursor: pointer; } button:disabled { opacity: .55; cursor: wait; }.error-message { color: #bd3d19; font-size: 13px; }
.report-panel { margin-top: 32px; }.report-title { display: flex; justify-content: space-between; color: #486581; }.summary { font-size: 18px; line-height: 1.7; }.warning { color: #a04a1a; }
.gap-list { display: grid; gap: 14px; }.gap-item { border-top: 3px solid #0b5cab; background: #f4f9fb; padding: 16px; }.gap-item.gap { border-color: #bd3d19; }.gap-item.unverified { border-color: #d59b19; }
.skill, .badges, .evidence-heading { display: flex; align-items: center; gap: 8px; }.skill { justify-content: space-between; }.badges span, .badges small { padding: 4px 7px; border-radius: 999px; font-size: 11px; font-weight: 800; }.badges span { background: #dbeafe; color: #07599c; }.gap-item.gap .badges span { background: #fee2e2; color: #bd3d19; }.gap-item.unverified .badges span { background: #fef3c7; color: #9a6700; }.badges small { color: #627d98; background: #e9f1f6; }
.gap-item > p { line-height: 1.6; }.evidence-list { display: grid; gap: 9px; }.evidence { border-left: 3px solid #e1672d; padding-left: 12px; }.evidence-heading { justify-content: space-between; }.evidence small { color: #627d98; }.evidence a { font-size: 12px; color: #07599c; font-weight: 700; }.evidence blockquote { margin: 7px 0 0; color: #334e68; line-height: 1.65; white-space: pre-wrap; }.no-evidence { color: #8a5c00; font-size: 13px; }
@media (max-width: 640px) { .analysis-page { padding: 32px 18px; } h1 { font-size: 32px; }.skill { align-items: flex-start; flex-direction: column; }.evidence-heading { align-items: flex-start; flex-direction: column; gap: 3px; } }
</style>
