<template>
    <div>
        <PageHead title="心理医生管理">
            <template #buttons>
                <el-button @click="handleEdit({})" type="primary">新增医生</el-button>
            </template>
        </PageHead>
        <TableSearch :formItem="formItem" @search="handleSearch" />
        <el-table :data="tableData" style="width: 100%; margin-top: 25px">
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="title" label="职称" width="200" />
            <el-table-column prop="specialty" label="擅长领域" min-width="200" />
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
                        {{ scope.row.status === 1 ? '可预约' : '停诊' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="280" fixed="right">
                <template #default="scope">
                    <el-button @click="handleEdit(scope.row)" text type="primary">编辑</el-button>
                    <el-button v-if="scope.row.status === 0" @click="handleStatusChange(scope.row, 1)" text type="success">启用</el-button>
                    <el-button v-if="scope.row.status === 1" @click="handleStatusChange(scope.row, 0)" text type="warning">停诊</el-button>
                    <el-button @click="handleDelete(scope.row)" text type="danger">删除</el-button>
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
        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑医生' : '新增医生'" width="600px">
            <el-form :model="form" label-width="100px">
                <el-form-item label="登录账号" required>
                    <el-input v-model="form.username" placeholder="医生登录用的用户名" v-if="!isEdit" />
                </el-form-item>
                <el-form-item label="登录密码" required>
                    <el-input v-model="form.password" type="password" placeholder="医生登录用的密码" show-password v-if="!isEdit" />
                </el-form-item>
                <el-form-item label="姓名" required>
                    <el-input v-model="form.name" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="职称">
                    <el-input v-model="form.title" placeholder="如：高级心理咨询师" />
                </el-form-item>
                <el-form-item label="擅长领域">
                    <el-input v-model="form.specialty" placeholder="如：焦虑症、抑郁症、婚姻咨询" />
                </el-form-item>
                <el-form-item label="头像URL">
                    <el-input v-model="form.avatar" placeholder="头像图片地址" />
                </el-form-item>
                <el-form-item label="个人简介">
                    <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入个人简介" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>
<script setup>
import { onMounted, ref, reactive } from 'vue'
import PageHead from '@/components/PageHead.vue'
import TableSearch from '@/components/TableSearch.vue'
import { counselorPage, createCounselor, updateCounselor, deleteCounselor, changeCounselorStatus } from '@/api/admin'
import { ElMessageBox, ElMessage } from 'element-plus'

const formItem = [
    { comp: 'input', prop: 'name', label: '医生姓名', placeholder: '请输入医生姓名' },
    { comp: 'select', prop: 'status', label: '状态', placeholder: '请选择状态', options: [
        { label: '可预约', value: '1' },
        { label: '停诊', value: '0' }
    ] }
]

const pagination = reactive({ currentPage: 1, size: 10, total: 0 })
const tableData = ref([])
const searchParams = ref({})

const handleSearch = async (formData) => {
    searchParams.value = formData || {}
    const params = { currentPage: pagination.currentPage, size: pagination.size, ...searchParams.value }
    const { records, total } = await counselorPage(params)
    tableData.value = records
    pagination.total = total
}

const handlePageChange = (page) => {
    pagination.currentPage = page
    handleSearch()
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ username: '', password: '', name: '', title: '', specialty: '', avatar: '', description: '' })
let editingId = null

const handleEdit = (row) => {
    if (row.id) {
        isEdit.value = true
        editingId = row.id
        Object.assign(form, { name: row.name, title: row.title, specialty: row.specialty, avatar: row.avatar, description: row.description })
    } else {
        isEdit.value = false
        editingId = null
        Object.assign(form, { username: '', password: '', name: '', title: '', specialty: '', avatar: '', description: '' })
    }
    dialogVisible.value = true
}

const handleSubmit = async () => {
    if (!form.name) {
        ElMessage.warning('请输入医生姓名')
        return
    }
    if (!isEdit.value && (!form.username || !form.password)) {
        ElMessage.warning('请输入登录账号和密码')
        return
    }
    if (isEdit.value) {
        await updateCounselor(editingId, { ...form })
        ElMessage.success('更新成功')
    } else {
        await createCounselor({ username: form.username, password: form.password, name: form.name, title: form.title, specialty: form.specialty, avatar: form.avatar, description: form.description })
        ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    handleSearch()
}

const handleStatusChange = (row, status) => {
    const action = status === 1 ? '启用' : '停诊'
    ElMessageBox.confirm(`确认${action}${row.name}医生吗？`, '确认', { confirmButtonText: `确认${action}`, cancelButtonText: '取消', type: 'info' })
        .then(async () => {
            await changeCounselorStatus(row.id, status)
            ElMessage.success(`${action}成功`)
            handleSearch()
        })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`确认删除${row.name}医生吗？`, '确认', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'danger' })
        .then(async () => {
            await deleteCounselor(row.id)
            ElMessage.success('删除成功')
            handleSearch()
        })
}

onMounted(() => {
    handleSearch()
})
</script>
