<template>
    <div>
        <PageHead title="咨询总结" />
        <el-table :data="tableData" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="sessionId" label="会话ID" width="100" />
            <el-table-column prop="keyIssues" label="主要问题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="emotionalState" label="情绪状态" width="120" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="生成时间" width="170" />
            <el-table-column label="操作" width="80" fixed="right">
                <template #default="scope">
                    <el-button type="primary" text @click="viewDetail(scope.row)">详情</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            style="margin-top: 25px; justify-content: flex-end"
            v-model:current-page="pagination.currentPage"
            :page-size="pagination.size"
            layout="prev, pager, next"
            :total="pagination.total"
            @current-change="handlePageChange"
        />
        <el-dialog v-model="showDetail" title="总结详情" width="600px">
            <div v-if="detail" class="summary-detail">
                <div class="detail-section" v-if="detail.keyIssues">
                    <h4>主要问题</h4>
                    <p>{{ detail.keyIssues }}</p>
                </div>
                <div class="detail-section" v-if="detail.emotionalState">
                    <h4>情绪分析</h4>
                    <p>{{ detail.emotionalState }}</p>
                </div>
                <div class="detail-section" v-if="detail.suggestions">
                    <h4>AI 建议</h4>
                    <p>{{ detail.suggestions }}</p>
                </div>
                <div class="detail-section" v-if="detail.followUpGoals">
                    <h4>后续目标</h4>
                    <p>{{ detail.followUpGoals }}</p>
                </div>
            </div>
            <template #footer>
                <el-button @click="showDetail = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>
<script setup>
import { onMounted, ref, reactive } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { summaryPage, getSummaryDetail } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ currentPage: 1, size: 10, total: 0 })
const showDetail = ref(false)
const detail = ref(null)

const fetchData = async () => {
    loading.value = true
    try {
        const { records, total } = await summaryPage({ currentPage: pagination.currentPage, size: pagination.size })
        tableData.value = records
        pagination.total = total
    } finally {
        loading.value = false
    }
}

const handlePageChange = (page) => {
    pagination.currentPage = page
    fetchData()
}

const viewDetail = async (row) => {
    const res = await getSummaryDetail(row.id)
    detail.value = res
    showDetail.value = true
}

onMounted(() => { fetchData() })
</script>
<style scoped lang="scss">
.summary-detail { .detail-section { margin-bottom: 20px; h4 { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 8px; padding-bottom: 6px; border-bottom: 2px solid #4096ff; } p { font-size: 14px; color: #555; line-height: 1.7; white-space: pre-wrap; } } }
</style>
