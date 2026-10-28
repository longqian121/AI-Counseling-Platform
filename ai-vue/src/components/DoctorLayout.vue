<template>
    <div class="doctor-layout">
        <el-container>
            <el-aside width="200px">
                <div class="logo-area">
                    <h3>医生工作台</h3>
                </div>
                <el-menu :default-active="activeMenu" router>
                    <el-menu-item index="/doctor/dashboard">
                        <el-icon><Clock /></el-icon>
                        <span>今日预约</span>
                    </el-menu-item>
                    <el-menu-item index="/doctor/appointments">
                        <el-icon><List /></el-icon>
                        <span>全部预约</span>
                    </el-menu-item>
                </el-menu>
            </el-aside>
            <el-container>
                <el-header height="50px">
                    <div class="header-right">
                        <span class="doctor-name">{{ counselorName }}</span>
                        <el-button text @click="handleLogout">退出登录</el-button>
                    </div>
                </el-header>
                <el-main>
                    <router-view></router-view>
                </el-main>
            </el-container>
        </el-container>
    </div>
</template>
<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, List } from '@element-plus/icons-vue'
import { logout } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)

const counselorName = ref('')
try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo'))
    counselorName.value = userInfo?.displayName || userInfo?.nickname || userInfo?.username || ''
} catch (e) {}

const handleLogout = () => {
    logout().then(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/auth/login')
    })
}
</script>
<style scoped lang="scss">
.doctor-layout {
    height: 100vh;
    .el-container { height: 100%; }
    .el-aside {
        background: #304156;
        .logo-area {
            height: 60px;
            display: flex;
            align-items: center;
            justify-content: center;
            h3 { color: #fff; font-size: 16px; }
        }
    }
    .el-header {
        border-bottom: 1px solid #e6e6e6;
        display: flex;
        align-items: center;
        justify-content: flex-end;
        .header-right {
            display: flex;
            align-items: center;
            gap: 15px;
            .doctor-name { color: #333; font-weight: 500; }
        }
    }
    .el-main { background: #f0f2f5; }
}
</style>
