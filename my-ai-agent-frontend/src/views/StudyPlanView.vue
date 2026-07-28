<template>
  <AppLayout>
    <main class="plan-page">
      <header><p>SEVEN DAY FIELD PLAN</p><h1>学习计划</h1><span>从一份有证据的差距报告出发，先编辑草稿，再确认执行。</span></header>
      <section class="creation"><select v-model="selectedAnalysis"><option value="">选择一份差距报告</option><option v-for="analysis in analyses" :key="analysis.id" :value="analysis.id">{{ analysis.summary }}</option></select><input v-model="title" placeholder="计划名称（可选）"><button :disabled="!selectedAnalysis || creating" @click="createDraft">生成 7 天草稿</button></section>
      <section v-for="plan in plans" :key="plan.id" class="plan"><div class="plan-head"><div><small>{{ plan.status }}</small><h2>{{ plan.title }}</h2><p>{{ plan.draftNote }}</p></div><button v-if="plan.status === 'DRAFT'" @click="confirm(plan.id)">确认并排期</button></div><ol><li v-for="task in plan.tasks" :key="task.id"><div><b>{{ task.title }}</b><p>{{ task.description }}</p><small>{{ task.scheduledDate || '确认后排期' }}</small></div><select :value="task.taskStatus" @change="changeStatus(plan.id, task, $event.target.value)"><option value="TODO">待完成</option><option value="DONE">已完成</option><option value="SKIPPED">跳过</option></select></li></ol></section>
    </main>
  </AppLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { confirmStudyPlan, createStudyPlanDraft, listGapAnalyses, listStudyPlans, updateStudyTask } from '../api/studyPlan'
const analyses = ref([]); const plans = ref([]); const selectedAnalysis = ref(''); const title = ref(''); const creating = ref(false)
const load = async () => { const [analysisResponse, planResponse] = await Promise.all([listGapAnalyses(), listStudyPlans()]); analyses.value = analysisResponse.data.data; plans.value = planResponse.data.data }
const createDraft = async () => { creating.value = true; try { await createStudyPlanDraft(selectedAnalysis.value, title.value); await load() } finally { creating.value = false } }
const confirm = async (planId) => { await confirmStudyPlan(planId); await load() }
const changeStatus = async (planId, task, taskStatus) => { await updateStudyTask(planId, task.id, { taskStatus }); await load() }
onMounted(load)
</script>

<style scoped>
.plan-page { max-width: 1050px; margin: auto; padding: 48px 36px 90px; color: #102a43; }.plan-page header p { font: 800 12px/1 sans-serif; letter-spacing: .16em; color: #d65b25; }.plan-page h1 { font: 42px Georgia, 'Noto Serif SC', serif; margin: 7px 0; }.plan-page header span { color: #627d98; }.creation { display: flex; gap: 10px; margin: 32px 0; background: #eaf3f8; padding: 18px; }.creation select, .creation input { flex: 1; min-width: 0; padding: 10px; border: 1px solid #a9c1d1; }.creation button, .plan-head button { border: 0; background: #07599c; color: #fff; font-weight: 700; padding: 10px 16px; cursor: pointer; }.plan { margin-top: 22px; border: 1px solid #b7cddd; background: #fffdf8; box-shadow: 7px 7px 0 #dbeaf3; }.plan-head { display: flex; justify-content: space-between; gap: 20px; padding: 20px; border-bottom: 1px solid #d9e4ec; }.plan-head small { color: #d65b25; font-weight: 800; letter-spacing: .1em; }.plan h2 { margin: 5px 0; }.plan-head p { margin: 0; color: #627d98; }ol { list-style: none; padding: 0; margin: 0; }li { display: flex; justify-content: space-between; gap: 16px; padding: 15px 20px; border-bottom: 1px solid #e4edf2; }li p { margin: 6px 0; color: #486581; }li small { color: #829ab1; }li select { align-self: center; padding: 7px; border: 1px solid #a9c1d1; }@media (max-width: 650px) { .plan-page { padding: 30px 16px; }.creation, .plan-head, li { flex-direction: column; }.plan-page h1 { font-size: 32px; } }
</style>
