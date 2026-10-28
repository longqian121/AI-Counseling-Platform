import service from '@/utils/request'

export function login(data) {
    return service.post('/user/login', data)
}


export function categoryTree() {
    return service.get('/knowledge/category/tree')
}

export function articlePage(params) {
     return service.get('/knowledge/article/page', { params })
}

export function uploadFile(file, businessInfo) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'ARTICLE')
    formData.append('businessId', businessInfo.businessId)
    formData.append('businessField', 'cover')

    return service.post('/file/upload', formData)
}


export function createArticle(data) {
    return service.post('/knowledge/article', data)
}

export function getArticleDetail(id) {
    return service.get(`/knowledge/article/${id}`)
}


export function updateArticle(id, data) {
    return service.put(`/knowledge/article/${id}`, data)
}

export function changeArticleStatus(id, data) {
    return service.put(`/knowledge/article/${id}/status`, data)
}

export function deleteArticle(id) {
    return service.delete(`/knowledge/article/${id}`)
}

export function getConsultationPage(params) {
    return service.get('/psychological-chat/admin/sessions', { params })
}

export function getSessionDetail(sessionId) {
    return service.get(`/psychological-chat/sessions/${sessionId}/messages`)
}

export function getEmotionalPage(params) {
    return service.get('/emotion-diary/admin/page', { params })
}

export function deleteEmotional(id) {
    return service.delete(`/emotion-diary/admin/${id}`)
}

export function getAnalyticsOverview() {
    return service.get(`/data-analytics/overview`)
}

export function logout() {
    return service.post('/user/logout')
}
export function counselorPage(params) {
    return service.get('/admin/counselor/page', { params })
}

export function createCounselor(data) {
    return service.post('/admin/counselor', data)
}

export function updateCounselor(id, data) {
    return service.put(`/admin/counselor/${id}`, data)
}

export function deleteCounselor(id) {
    return service.delete(`/admin/counselor/${id}`)
}

export function changeCounselorStatus(id, status) {
    return service.put(`/admin/counselor/${id}/status`, null, { params: { status } })
}

export function appointmentPage(params) {
    return service.get('/admin/appointment/page', { params })
}

export function getAppointmentDetail(id) {
    return service.get(`/admin/appointment/${id}`)
}

export function changeAppointmentStatus(id, status) {
    return service.put(`/admin/appointment/${id}/status`, null, { params: { status } })
}

export function summaryPage(params) { return service.get('/admin/consultation/summary/page', { params }) }
export function riskAlertPage(params) { return service.get('/admin/risk-alert/page', { params }) }
export function markRiskAlertRead(id) { return service.put('/admin/risk-alert/' + id + '/read') }
export function riskAlertUnreadCount() { return service.get('/admin/risk-alert/unread-count') }
export function qualityReportLatest() { return service.get('/admin/quality/report/latest') }
export function triggerQualityReport() { return service.post('/admin/quality/report/trigger') }
export function getSummaryDetail(id) { return service.get('/admin/consultation/summary/' + id) }
