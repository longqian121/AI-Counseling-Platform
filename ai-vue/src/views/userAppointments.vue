<template>
    <div class="user-appointments">
        <h2 class="page-title">我的预约</h2>
        <el-table :data="tableData" style="width: 100%; margin-top: 20px" v-loading="loading">
            <el-table-column prop="counselorName" label="医生姓名" width="120" />
            <el-table-column prop="counselorTitle" label="职称" width="120" />
            <el-table-column prop="appointmentDate" label="预约日期" width="130" />
            <el-table-column prop="timeSlot" label="时间段" width="130" />
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="statusTag(scope.row.status)">
                        {{ statusText(scope.row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
                <template #default="scope">
                    <el-popconfirm
                        v-if="scope.row.status === 1"
                        title="确认取消该预约吗？"
                        confirm-button-text="确认取消"
                        cancel-button-text="再想想"
                        @confirm="handleCancel(scope.row.id)"
                    >
                        <template #reference>
                            <el-button type="danger" text>取消预约</el-button>
                        </template>
                    </el-popconfirm>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            style="margin-top: 25px; justify-content: center"
            v-model:current-page="pagination.pageNum"
            :page-size="pagination.pageSize"
            layout="prev, pager, next"
            :total="pagination.total"
            @current-change="handlePageChange"
        />
    </div>
</template>
<script setup>
import { onMounted, ref, reactive } from 'vue'
import { getMyAppointments, cancelAppointment } from '@/api/frontend'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const tableData = ref([])

const statusTag = (status) => {
    return status === 1 ? 'warning' : status === 2 ? 'success' : 'info'
}

const statusText = (status) => {
    return status === 1 ? '已预约' : status === 2 ? '已完成' : '已取消'
}

const fetchData = async () => {
    loading.value = true
    try {
        const { records, total } = await getMyAppointments({
            pageNum: pagination.pageNum,
            pageSize: pagination.pageSize
        })
        tableData.value = records
        pagination.total = total
    } finally {
        loading.value = false
    }
}

const handlePageChange = (page) => {
    pagination.pageNum = page
    fetchData()
}

const handleCancel = async (id) => {
    await cancelAppointment(id)
    ElMessage.success('预约已取消')
    fetchData()
}

onMounted(() => {
    fetchData()
})
</script>
<style scoped lang="scss">
.user-appointments {
    max-width: 1000px;
    margin: 0 auto;
    padding: 20px;

    .page-title {
        font-size: 22px;
        font-weight: 600;
        color: #333;
    }
}
</style>
