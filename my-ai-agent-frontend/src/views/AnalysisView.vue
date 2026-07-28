<template>
  <AppLayout>
    <main class="analysis-page">
      <header class="page-header">
        <p class="eyebrow">EVIDENCE-BASED REVIEW</p>
        <h1>能力差距分析</h1>
        <p>粘贴目标 JD，用你的私有资料验证每一个缺口；没有资料依据时，系统会明确说“不知道”。</p>
      </header>
      <section class="input-panel">
        <label for="jd">目标岗位 JD</label>
        <textarea id="jd" v-model="jobDescription" placeholder="粘贴 Java 后端实习岗位的职责与要求…" />
        <button :disabled="loading || !jobDescription.trim()" @click="runAnalysis">{{ loading ? '正在核对资料…' : '生成有证据的分析' }}</button>
      </section>
      <section v-if="report" class="report-panel">
        <div class="report-title"><span>REPORT</span><time>{{ formatDate(report.createTime) }}</time></div>
        <p class="summary" :class="{ warning: report.insufficientEvidence }">{{ report.summary }}</p>
        <div v-if="report.gaps?.length" class="gap-list">
          <article v-for="gap in report.gaps" :key="gap.skill" class="gap-item">
            <div class="skill"><strong>{{ gap.skill }}</strong><span>{{ gap.severity }}</span></div>
            <p>{{ gap.recommendation }}</p>
            <div v-for="evidence in gap.evidence" :key="`${evidence.documentName}-${evidence.chunkIndex}`" class="evidence">
              <small>{{ evidence.documentName }} · 片段 {{ evidence.chunkIndex + 1 }}</small>
              <blockquote>{{ evidence.content }}</blockquote>
            </div>
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
const runAnalysis = async () => { loading.value = true; try { const response = await createGapAnalysis(jobDescription.value); report.value = response.data.data } finally { loading.value = false } }
const formatDate = (value) => value ? new Date(value).toLocaleString('zh-CN') : '刚刚生成'
</script>

<style scoped>
.analysis-page { max-width: 1060px; margin: 0 auto; padding: 48px 36px 88px; color: #102a43; }.page-header { max-width: 720px; margin-bottom: 30px; }.eyebrow { color: #e1672d; letter-spacing: .18em; font-size: 12px; font-weight: 800; }h1 { font-family: Georgia, 'Noto Serif SC', serif; font-size: 42px; margin: 8px 0 12px; }.page-header > p:last-child { color: #627d98; line-height: 1.75; }.input-panel, .report-panel { background: #fffdf7; border: 1px solid #c8d8e5; box-shadow: 8px 8px 0 #d9e9f3; padding: 25px; }.input-panel { display: grid; gap: 12px; }label, .report-title { font-size: 12px; letter-spacing: .12em; font-weight: 800; }textarea { min-height: 170px; resize: vertical; border: 1px solid #9fb3c8; background: #f7fbfd; padding: 14px; font: inherit; line-height: 1.65; }button { justify-self: start; border: 0; padding: 11px 18px; background: #0b5cab; color: white; font-weight: 700; cursor: pointer; }button:disabled { opacity: .55; cursor: wait; }.report-panel { margin-top: 32px; }.report-title { display: flex; justify-content: space-between; color: #486581; }.summary { font-size: 18px; line-height: 1.7; }.warning { color: #a04a1a; }.gap-list { display: grid; gap: 14px; }.gap-item { border-top: 3px solid #0b5cab; background: #f4f9fb; padding: 16px; }.skill { display: flex; justify-content: space-between; }.skill span { color: #bd3d19; font-size: 11px; letter-spacing: .08em; font-weight: 800; }.gap-item p { line-height: 1.6; }.evidence { border-left: 3px solid #e1672d; padding-left: 12px; }.evidence small { color: #627d98; }.evidence blockquote { margin: 7px 0 0; color: #334e68; line-height: 1.65; }@media (max-width: 640px) { .analysis-page { padding: 32px 18px; } h1 { font-size: 32px; } }
</style>
