<template>
    <div class="doctor-dashboard">
        <h2 class="page-title">今日预约</h2>
        <el-table :data="tableData" style="width: 100%; margin-top: 20px" v-loading="loading">
            <el-table-column prop="userNickname" label="患者昵称" width="150" />
            <el-table-column prop="timeSlot" label="时间段" width="150" />
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.status === 1 ? 'warning' : scope.row.status === 2 ? 'success' : 'info'">
                        {{ scope.row.status === 1 ? '已预约' : scope.row.status === 2 ? '已完成' : '已取消' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="200">
                <template #default="scope">
                    <el-button type="success" text @click="viewBriefing(scope.row)">查看简报</el-button>
                    <el-popconfirm
                        v-if="scope.row.status === 1"
                        title="确认将该预约标记为已完成？"
                        confirm-button-text="确认"
                        cancel-button-text="取消"
                        @confirm="handleComplete(scope.row.id)"
                    >
                        <template #reference>
                            <el-button type="success" text>标记完成</el-button>
                        </template>
                    </el-popconfirm>
                </template>
            </el-table-column>
        </el-table>

        <el-dialog v-model="briefingVisible" title="来访者背景简报" width="650px">
            <div v-loading="briefingLoading">
                <div v-if="briefingError" style="color:#999;text-align:center;padding:20px">暂无简报</div>
                <div v-else-if="briefingContent">
                    <div v-if="briefingContent.patientBackground" style="margin-bottom:16px">
                        <h4>患者背景</h4><p>{{ briefingContent.patientBackground }}</p>
                    </div>
                    <div v-if="briefingContent.keyConcerns" style="margin-bottom:16px">
                        <h4>核心关切</h4><p>{{ briefingContent.keyConcerns }}</p>
                    </div>
                    <div v-if="briefingContent.emotionTrend" style="margin-bottom:16px">
                        <h4>情绪轨迹</h4><p>{{ briefingContent.emotionTrend }}</p>
                    </div>
                    <div v-if="briefingContent.riskHistory" style="margin-bottom:16px;background:#fff3f3;padding:10px;border-radius:4px">
                        <h4 style="color:#e74c3c">风险提示</h4><p>{{ briefingContent.riskHistory }}</p>
                    </div>
                    <div v-if="briefingContent.suggestedApproach" style="margin-bottom:16px">
                        <h4>沟通建议</h4><p style="white-space:pre-line">{{ briefingContent.suggestedApproach }}</p>
                    </div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getTodayAppointments, completeAppointment, getBriefing } from '@/api/doctor'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const briefingContent = ref(null)
const briefingVisible = ref(false)
const briefingLoading = ref(false)
const briefingError = ref(false)

const fetchData = async () => {
    loading.value = true
    try {
        tableData.value = await getTodayAppointments()
    } finally {
        loading.value = false
    }
}

const handleComplete = async (id) => {
    await completeAppointment(id)
    ElMessage.success('已标记为完成')
    fetchData()
}

const viewBriefing = async (row) => {
    briefingVisible.value = true
    briefingContent.value = null
    briefingError.value = false
    briefingLoading.value = true
    try {
        const res = await getBriefing(row.id)
briefingContent.value = res
    } catch {
        briefingError.value = true
    } finally {
        briefingLoading.value = false
    }
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.doctor-dashboard {
    .page-title { font-size: 20px; font-weight: 600; color: #333; }
}
</style>
