<template>
    <div>
        <h2 class="page-title">患者历史总结</h2>
        <div v-loading="loading">
            <el-empty v-if="!loading && summaries.length === 0" description="暂无咨询总结" />
            <el-card v-for="item in summaries" :key="item.id" class="summary-card" shadow="hover">
                <template #header>
                    <div class="card-header">
                        <span>会话 #{{ item.sessionId }}</span>
                        <span style="font-size:12px;color:#999">{{ item.createdAt }}</span>
                    </div>
                </template>
                <div class="section" v-if="item.keyIssues">
                    <h4>主要问题</h4>
                    <p>{{ item.keyIssues }}</p>
                </div>
                <div class="section" v-if="item.emotionalState">
                    <h4>情绪分析</h4>
                    <p>{{ item.emotionalState }}</p>
                </div>
                <div class="section" v-if="item.suggestions">
                    <h4>AI 建议</h4>
                    <p>{{ item.suggestions }}</p>
                </div>
                <div class="section" v-if="item.followUpGoals">
                    <h4>后续目标</h4>
                    <p>{{ item.followUpGoals }}</p>
                </div>
            </el-card>
        </div>
    </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPatientSummaries } from '@/api/doctor'

const route = useRoute()
const loading = ref(false)
const summaries = ref([])

onMounted(async () => {
    loading.value = true
    try {
        const res = await getPatientSummaries(route.params.userId)
        summaries.value = res || []
    } finally {
        loading.value = false
    }
})
</script>
<style scoped lang="scss">
.page-title { font-size: 20px; font-weight: 600; color: #333; margin-bottom: 20px; }
.summary-card { margin-bottom: 16px; .card-header { display: flex; justify-content: space-between; align-items: center; } .section { margin-bottom: 14px; h4 { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 6px; } p { font-size: 14px; color: #555; line-height: 1.7; white-space: pre-wrap; } } }
</style>
