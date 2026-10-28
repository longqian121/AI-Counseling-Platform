<template>
    <div>
        <PageHead title="风险告警" />
        <el-card>
            <el-row :gutter="12" style="margin-bottom: 16px">
                <el-col :span="4">
                    <el-select v-model="filterRiskLevel" placeholder="风险等级" clearable>
                        <el-option label="高度危机(4)" :value="4" />
                        <el-option label="中度风险(3)" :value="3" />
                        <el-option label="轻度风险(2)" :value="2" />
                        <el-option label="关注(1)" :value="1" />
                        <el-option label="正常(0)" :value="0" />
                    </el-select>
                </el-col>
                <el-col :span="4">
                    <el-select v-model="filterIsRead" placeholder="已读状态" clearable>
                        <el-option label="未读" :value="0" />
                        <el-option label="已读" :value="1" />
                    </el-select>
                </el-col>
                <el-col :span="4">
                    <el-button type="primary" @click="search">查询</el-button>
                </el-col>
            </el-row>
            <el-table :data="tableData" stripe v-loading="loading" @row-click="showDetail">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="userId" label="用户ID" width="80" />
                <el-table-column prop="sessionId" label="会话ID" width="80" />
                <el-table-column prop="evidence" label="关键证据" min-width="250" show-overflow-tooltip />
                <el-table-column label="风险等级" width="100">
                    <template #default="{ row }">
                        <el-tag :type="riskTagType(row.riskLevel)" size="small">
                            {{ riskLevelLabel(row.riskLevel) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="riskType" label="风险类型" width="110" />
                <el-table-column prop="confidence" label="置信度" width="80" />
                <el-table-column label="已读" width="70">
                    <template #default="{ row }">
                        <el-tag :type="row.isRead ? 'success' : 'danger'" size="small">
                            {{ row.isRead ? '已读' : '未读' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="触发时间" width="160" />
                <el-table-column label="操作" width="100" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="!row.isRead" type="primary" text size="small" @click.stop="markRead(row.id)">
                            标记已读
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div style="margin-top: 16px; text-align: right">
                <el-pagination
                    v-model:current-page="pageNum"
                    v-model:page-size="pageSize"
                    :total="total"
                    layout="total, prev, pager, next"
                    @current-change="loadData"
                />
            </div>
        </el-card>

        <el-dialog v-model="detailVisible" title="告警详情" width="600px">
            <template v-if="selectedRow">
                <el-descriptions :column="1" border>
                    <el-descriptions-item label="用户ID">{{ selectedRow.userId }}</el-descriptions-item>
                    <el-descriptions-item label="会话ID">{{ selectedRow.sessionId }}</el-descriptions-item>
                    <el-descriptions-item label="风险等级">
                        <el-tag :type="riskTagType(selectedRow.riskLevel)">
                            {{ riskLevelLabel(selectedRow.riskLevel) }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="风险类型">{{ selectedRow.riskType }}</el-descriptions-item>
                    <el-descriptions-item label="置信度">{{ selectedRow.confidence }}%</el-descriptions-item>
                    <el-descriptions-item label="关键证据">{{ selectedRow.evidence }}</el-descriptions-item>
                    <el-descriptions-item label="建议措施">{{ selectedRow.suggestedAction }}</el-descriptions-item>
                    <el-descriptions-item label="触发时间">{{ selectedRow.createdAt }}</el-descriptions-item>
                </el-descriptions>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import PageHead from '@/components/PageHead.vue'
import { riskAlertPage, markRiskAlertRead } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterRiskLevel = ref(null)
const filterIsRead = ref(null)

const detailVisible = ref(false)
const selectedRow = ref(null)

const riskLevelLabel = (level) => {
    const map = { 0: '正常', 1: '关注', 2: '轻度', 3: '中度', 4: '高度' }
    return map[level] || '未知'
}

const riskTagType = (level) => {
    const map = { 0: 'info', 1: 'warning', 2: 'warning', 3: 'danger', 4: 'danger' }
    return map[level] || 'info'
}

const loadData = async () => {
    loading.value = true
    try {
        const params = {
            currentPage: pageNum.value,
            size: pageSize.value,
            riskLevel: filterRiskLevel.value,
            isRead: filterIsRead.value
        }
        const res = await riskAlertPage(params)
tableData.value = res.records
total.value = res.total
    } finally {
        loading.value = false
    }
}

const search = () => {
    pageNum.value = 1
    loadData()
}

const showDetail = (row) => {
    selectedRow.value = row
    detailVisible.value = true
}

const markRead = async (id) => {
    await markRiskAlertRead(id)
    loadData()
}

onMounted(() => {
    loadData()
})
</script>
