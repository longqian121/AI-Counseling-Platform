<template>
    <div>
        <PageHead title="质检报告" />
        <div style="margin-bottom: 16px">
            <el-button type="primary" :loading="triggerLoading" @click="handleTrigger">立即执行质检</el-button>
            <el-button @click="loadLatest">刷新</el-button>
        </div>
        <el-card v-loading="loading">
            <div v-if="!report" style="text-align:center;color:#999;padding:40px">暂无质检报告，点击"立即执行质检"生成</div>
            <el-descriptions v-else :column="2" border>
                <el-descriptions-item label="报告日期">{{ report.reportDate }}</el-descriptions-item>
                <el-descriptions-item label="综合评分">{{ report.overallScore ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="昨日会话数">{{ report.totalSessions }}</el-descriptions-item>
                <el-descriptions-item label="风险告警数">{{ report.riskAlertCount }}</el-descriptions-item>
                <el-descriptions-item label="危机会话数">{{ report.crisisCount }}</el-descriptions-item>
                <el-descriptions-item label="共情评分">{{ report.empathyScore ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="回复相关性评分">{{ report.relevanceScore ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="安全性评分">{{ report.safetyScore ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="高频问题 TOP5" :span="2">{{ report.topIssues || '-' }}</el-descriptions-item>
                <el-descriptions-item label="需复核案例" :span="2">{{ report.flaggedSessionIds || '-' }}</el-descriptions-item>
            </el-descriptions>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { qualityReportLatest, triggerQualityReport } from '@/api/admin'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const triggerLoading = ref(false)
const report = ref(null)

const loadLatest = async () => {
    loading.value = true
    try {
        const res = await qualityReportLatest()
        report.value = res
    } catch {
        report.value = null
    } finally {
        loading.value = false
    }
}

const handleTrigger = async () => {
    triggerLoading.value = true
    try {
        await triggerQualityReport()
        ElMessage.success('质检任务已触发，稍等后刷新查看结果')
        setTimeout(() => loadLatest(), 5000)
    } catch (e) {
        ElMessage.error('触发失败: ' + (e.message || '未知错误'))
    } finally {
        triggerLoading.value = false
    }
}

onMounted(loadLatest)
</script>
