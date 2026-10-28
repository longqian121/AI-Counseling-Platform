<template>
    <div>
        <PageHead title="预约管理" />
        <TableSearch :formItem="formItem" @search="handleSearch" />
        <el-table :data="tableData" style="width: 100%; margin-top: 25px">
            <el-table-column prop="id" label="预约ID" width="80" />
            <el-table-column label="用户" width="150">
                <template #default="scope">
                    <span>{{ scope.row.nickname || scope.row.userId }}</span>
                </template>
            </el-table-column>
            <el-table-column label="心理医生" width="150">
                <template #default="scope">
                    <span>{{ scope.row.counselorName || '未知' }}</span>
                </template>
            </el-table-column>
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
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                    <el-button v-if="scope.row.status === 1" @click="handleStatusChange(scope.row, 2)" text type="success">完成</el-button>
                    <el-button v-if="scope.row.status === 1" @click="handleStatusChange(scope.row, 0)" text type="danger">取消</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination
            style="margin-top: 25px"
            :page-size="pagination.size"
            layout="prev, pager, next"
            :total="pagination.total"
            @change="handlePageChange"
        />
    </div>
</template>
<script setup>
import { onMounted, ref, reactive } from 'vue'
import PageHead from '@/components/PageHead.vue'
import TableSearch from '@/components/TableSearch.vue'
import { appointmentPage, changeAppointmentStatus } from '@/api/admin'
import { ElMessageBox, ElMessage } from 'element-plus'

const formItem = [
    { comp: 'input', prop: 'userId', label: '用户ID', placeholder: '请输入用户ID' },
    { comp: 'input', prop: 'counselorId', label: '医生ID', placeholder: '请输入医生ID' },
    { comp: 'select', prop: 'status', label: '状态', placeholder: '请选择状态', options: [
        { label: '已预约', value: '1' },
        { label: '已完成', value: '2' },
        { label: '已取消', value: '0' }
    ] }
]

const pagination = reactive({ currentPage: 1, size: 10, total: 0 })
const tableData = ref([])
const searchParams = ref({})

const statusTag = (status) => {
    return status === 1 ? 'warning' : status === 2 ? 'success' : 'info'
}

const statusText = (status) => {
    return status === 1 ? '已预约' : status === 2 ? '已完成' : '已取消'
}

const handleSearch = async (formData) => {
    searchParams.value = formData || {}
    const params = { currentPage: pagination.currentPage, size: pagination.size, ...searchParams.value }
    const { records, total } = await appointmentPage(params)
    tableData.value = records
    pagination.total = total
}

const handlePageChange = (page) => {
    pagination.currentPage = page
    handleSearch()
}

const handleStatusChange = (row, status) => {
    const action = status === 2 ? '完成' : '取消'
    ElMessageBox.confirm(`确认将预约标记为${action}吗？`, '确认', { confirmButtonText: `确认${action}`, cancelButtonText: '取消', type: 'info' })
        .then(async () => {
            await changeAppointmentStatus(row.id, status)
            ElMessage.success(`预约已${action}`)
            handleSearch()
        })
}

onMounted(() => {
    handleSearch()
})
</script>
