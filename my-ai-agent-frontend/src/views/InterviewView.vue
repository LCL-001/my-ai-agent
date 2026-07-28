<template>
  <AppLayout>
    <main class="interview-page">
      <header class="page-header">
        <p>STATEFUL MOCK INTERVIEW</p>
        <h1>模拟面试</h1>
        <span>每次答题后，AI 会给出结构化反馈、补弱任务和下一题；全过程可中断、恢复与回看。</span>
      </header>

      <section v-if="!session" class="start-panel">
        <label for="interview-topic">面试主题</label>
        <input id="interview-topic" v-model="topic" placeholder="例如：Java 后端实习" @keyup.enter="start" />
        <button :disabled="loading" @click="start">{{ loading ? '正在准备首题…' : '开始 5 题面试' }}</button>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      </section>

      <section v-else class="session-panel">
        <header class="session-header">
          <div><p>SESSION</p><h2>{{ session.topic }}</h2></div>
          <span class="session-status">{{ statusLabel(session.status) }}</span>
          <button v-if="session.status === 'IN_PROGRESS'" class="secondary" :disabled="loading" @click="pause">中断</button>
          <button v-if="session.status === 'PAUSED'" :disabled="loading" @click="resume">恢复</button>
        </header>

        <article v-for="turn in session.turns" :key="turn.turnNumber" class="turn-card" :class="turn.turnStatus.toLowerCase()">
          <div class="question-heading"><span>Q{{ turn.turnNumber }}</span><strong>{{ turn.question }}</strong></div>
          <p v-if="turn.questionContext" class="question-context">考察：{{ turn.questionContext }}</p>

          <template v-if="turn.answer">
            <section class="answer-block"><small>你的回答</small><p>{{ turn.answer }}</p></section>
            <section v-if="turn.feedback" class="feedback-block">
              <header><strong>评分 {{ turn.score }}</strong><span>{{ turn.feedback }}</span></header>
              <div class="feedback-grid">
                <div><small>做得好的地方</small><ul><li v-for="item in turn.strengths || []" :key="item">{{ item }}</li></ul></div>
                <div><small>需要补强</small><ul><li v-for="item in turn.weaknesses || []" :key="item">{{ item }}</li></ul></div>
              </div>
              <div v-if="turn.practiceTasks?.length" class="practice"><small>补弱任务</small><ol><li v-for="task in turn.practiceTasks" :key="task">{{ task }}</li></ol></div>
            </section>
          </template>

          <div v-if="turn.evidence?.length" class="evidence-row">
            <span>参考资料：</span>
            <RouterLink v-for="evidence in turn.evidence" :key="`${evidence.documentId}-${evidence.chunkIndex}`" :to="{ path: '/prepare', query: { documentId: evidence.documentId } }">
              {{ evidence.documentName }} / 分片 {{ evidence.chunkIndex + 1 }}
            </RouterLink>
          </div>

          <template v-if="turn.turnStatus === 'ASKED' && session.status === 'IN_PROGRESS'">
            <textarea v-model="answer" placeholder="按定义、原理、实践案例和边界条件组织你的回答" />
            <button :disabled="loading || !answer.trim()" @click="submit">{{ loading ? 'AI 正在评分并准备下一题…' : '提交回答' }}</button>
          </template>
          <p v-if="turn.turnStatus === 'EVALUATING'" class="pending">正在生成反馈，请稍候；刷新页面后仍可恢复本题。</p>
        </article>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      </section>
    </main>
  </AppLayout>
</template>

<script setup>
import { ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { answerInterview, pauseInterview, resumeInterview, startInterview } from '../api/interview'

const topic = ref('Java 后端实习')
const answer = ref('')
const session = ref(null)
const loading = ref(false)
const errorMessage = ref('')

async function start() {
  loading.value = true
  errorMessage.value = ''
  try {
    session.value = (await startInterview(topic.value, 5)).data.data
  } catch (error) {
    errorMessage.value = error.message || '无法生成首题，请确认本地模型已启动后重试。'
  } finally {
    loading.value = false
  }
}

async function submit() {
  loading.value = true
  errorMessage.value = ''
  try {
    session.value = (await answerInterview(session.value.id, answer.value)).data.data
    answer.value = ''
  } catch (error) {
    errorMessage.value = error.message || '评分失败，本题没有被提交，可以检查模型后重试。'
  } finally {
    loading.value = false
  }
}

async function pause() {
  loading.value = true
  try { session.value = (await pauseInterview(session.value.id)).data.data } finally { loading.value = false }
}

async function resume() {
  loading.value = true
  try { session.value = (await resumeInterview(session.value.id)).data.data } finally { loading.value = false }
}

const statusLabel = (status) => ({ IN_PROGRESS: '进行中', PAUSED: '已暂停', COMPLETED: '已完成' })[status] || status
</script>

<style scoped>
.interview-page { max-width: 920px; margin: 0 auto; padding: 48px 36px 88px; color: #102a43; }.page-header { display: grid; gap: 7px; margin-bottom: 28px; }.page-header p, .session-header p { color: #d65b25; font: 800 12px ui-monospace, monospace; letter-spacing: .12em; }.page-header h1 { margin: 0; font: 42px Georgia, 'Noto Serif SC', serif; }.page-header span { color: #627d98; line-height: 1.65; }
.start-panel, .session-panel { background: #fffdf8; border: 1px solid #bdd0dc; box-shadow: 8px 8px 0 #d9e9f3; padding: 24px; }.start-panel { display: grid; gap: 12px; }.start-panel label { font-size: 12px; font-weight: 800; letter-spacing: .1em; }.start-panel input, textarea { width: 100%; box-sizing: border-box; border: 1px solid #9fb3c8; background: #f7fbfd; padding: 12px; font: inherit; }.start-panel button, .turn-card > button, .session-header button { width: max-content; border: 0; padding: 10px 16px; background: #07599c; color: #fff; font-weight: 700; cursor: pointer; }.start-panel button:disabled, .turn-card > button:disabled, .session-header button:disabled { opacity: .55; cursor: wait; }.session-header { display: flex; align-items: center; gap: 12px; border-bottom: 1px solid #d9e4ec; padding-bottom: 16px; }.session-header h2 { margin: 3px 0 0; font-size: 22px; }.session-status { margin-left: auto; color: #d65b25; font-size: 12px; font-weight: 800; }.session-header .secondary { background: #627d98; }
.turn-card { margin-top: 18px; padding: 18px; border: 1px solid #d9e4ec; border-left: 4px solid #0b5cab; background: #fff; }.turn-card.answered { border-left-color: #15937c; }.turn-card.evaluating { border-left-color: #d59b19; }.question-heading { display: grid; grid-template-columns: 34px 1fr; gap: 10px; align-items: start; }.question-heading span { color: #07599c; font: 800 12px ui-monospace, monospace; padding-top: 3px; }.question-heading strong { line-height: 1.55; }.question-context { margin: 9px 0 0 44px; color: #627d98; font-size: 13px; }.answer-block, .feedback-block { margin-top: 14px; padding: 13px; background: #f7fbfd; }.answer-block small, .feedback-block small { color: #627d98; font-weight: 800; }.answer-block p { white-space: pre-wrap; line-height: 1.65; margin: 7px 0 0; }.feedback-block header { display: grid; gap: 4px; }.feedback-block header strong { color: #07599c; }.feedback-block header span { line-height: 1.6; }.feedback-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 12px; }.feedback-grid div:first-child small { color: #087f5b; }.feedback-grid div:last-child small { color: #bd3d19; }.feedback-grid ul, .practice ol { margin: 6px 0 0; padding-left: 20px; }.feedback-grid li, .practice li { margin: 5px 0; line-height: 1.5; font-size: 13px; }.practice { border-top: 1px solid #d9e4ec; margin-top: 13px; padding-top: 11px; }.practice small { color: #7c3aed; }.turn-card textarea { min-height: 130px; resize: vertical; margin-top: 16px; line-height: 1.65; }.turn-card > button { margin-top: 10px; }.evidence-row { display: flex; flex-wrap: wrap; gap: 7px; align-items: center; margin-top: 13px; color: #627d98; font-size: 12px; }.evidence-row a { color: #07599c; background: #eff6ff; padding: 4px 6px; }.pending { color: #9a6700; font-size: 13px; }.error-message { color: #bd3d19; font-size: 13px; margin: 12px 0 0; }
@media (max-width: 640px) { .interview-page { padding: 32px 18px; }.page-header h1 { font-size: 32px; }.session-header { align-items: flex-start; flex-wrap: wrap; }.session-status { margin-left: 0; }.feedback-grid { grid-template-columns: 1fr; }.question-context { margin-left: 0; } }
</style>
