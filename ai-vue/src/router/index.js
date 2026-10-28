import { createRouter, createWebHistory } from 'vue-router'
import BackendLayout from '@/components/BackendLayout.vue'
import AuthLayout from '@/components/AuthLayout.vue'
import FrontendLayout from '@/components/FrontendLayout.vue'
import DoctorLayout from '@/components/DoctorLayout.vue'


const backendRoutes = [
    {
        path: '/back',
        redirect: '/back/dashboard',
        component: BackendLayout,
        children: [
            { path: 'dashboard', component: () => import('@/views/dashboard.vue'), meta: { title: '数据分析', icon: 'PieChart' } },
            { path: 'knowledge', component: () => import('@/views/knowledge.vue'), meta: { title: '知识文章', icon: 'ChatLineSquare' } },
            { path: 'consultations', component: () => import('@/views/consultations.vue'), meta: { title: '咨询记录', icon: 'Message' } },
            { path: 'emotional', component: () => import('@/views/emotional.vue'), meta: { title: '情绪日志', icon: 'User' } },
            { path: 'counselor', component: () => import('@/views/counselorManage.vue'), meta: { title: '心理医生', icon: 'Avatar' } },
            { path: 'appointment', component: () => import('@/views/appointmentManage.vue'), meta: { title: '预约管理', icon: 'Calendar' } },
            { path: 'summaries', component: () => import('@/views/consultationSummaries.vue'), meta: { title: '咨询总结', icon: 'Document' } },
            { path: 'risk-alerts', component: () => import('@/views/riskAlerts.vue'), meta: { title: '风险告警', icon: 'WarningFilled' } },
            { path: 'quality', component: () => import('@/views/qualityReport.vue'), meta: { title: '质检报告', icon: 'DataAnalysis' } }
        ]
    },
    { path: '/auth', component: AuthLayout, children: [
        { path: 'login', component: () => import('@/views/login.vue'), meta: { title: '登录' } },
        { path: 'register', component: () => import('@/views/register.vue'), meta: { title: '注册' } }
    ]}
]

const frontendRoutes = [
    { path: '/', component: FrontendLayout, children: [
        { path: '', component: () => import('@/views/home.vue') },
        { path: 'consultation', component: () => import('@/views/consultation.vue') },
        { path: 'emotion-diary', component: () => import('@/views/emotionDiary.vue') },
        { path: 'knowledge', component: () => import('@/views/frontendKnowledge.vue') },
        { path: 'knowledge/article/:id', component: () => import('@/views/articleDetail.vue'), props: true },
        { path: 'appointments', component: () => import('@/views/userAppointments.vue') }
    ]}
]

const doctorRoutes = [
    { path: '/doctor', redirect: '/doctor/dashboard', component: DoctorLayout, children: [
        { path: 'dashboard', component: () => import('@/views/doctorDashboard.vue'), meta: { title: '今日预约', icon: 'Clock' } },
        { path: 'appointments', component: () => import('@/views/doctorAppointments.vue'), meta: { title: '全部预约', icon: 'List' } },
        { path: 'patient/:userId/summaries', component: () => import('@/views/patientSummaries.vue'), meta: { title: '患者总结', icon: 'Document' } }
    ]}
]

const router = createRouter({
    history: createWebHistory(),
    routes: [ ...backendRoutes, ...frontendRoutes, ...doctorRoutes]
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (token) {
        const userInfo = JSON.parse(localStorage.getItem('userInfo'))
        if (userInfo.userType == 2) {
            if (to.path.startsWith('/back')) { next() } else { next('/back/dashboard') }
        } else if (userInfo.userType == 1){
            if (to.path.startsWith('/back') || to.path.startsWith('/auth')) { next('/') } else { next() }
        } else if (userInfo.userType == 3) {
            if (to.path.startsWith('/doctor')) { next() } else { next('/doctor/dashboard') }
        }
    } else {
        if (to.path.startsWith('/back')) { next('/auth/login') } else { next() }
    }
})

export default router
