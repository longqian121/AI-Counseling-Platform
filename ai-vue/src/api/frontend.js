import service from '@/utils/request'

export const register = (data) => {
    return service.post('/user/add', data)
}

export const startSession = (data) => {
    return service.post('/psychological-chat/session/start', data)
}

export const getSessionList = (params) => {
    return service.get('/psychological-chat/sessions', { params })
}

export const deleteSession = (sessionId) => {
    return service.delete(`/psychological-chat/sessions/${sessionId}`)
}

export const getSessionDetail = (sessionId) => {
    return service.get(`/psychological-chat/sessions/${sessionId}/messages`)
}

export const getSessionEmotion = (sessionId) => {
    return service.get(`/psychological-chat/session/${sessionId}/emotion`)
}

export const addEmotionDiary = (data) => {
    return service.post('/emotion-diary', data)
}

export const getKnowledgeList = (params) => {
    return service.get('/knowledge/article/page', { params })
}

export const getKnowledgeDetail = (articleId) => {
    return service.get(`/knowledge/article/${articleId}`)
}
export function getMyAppointments(params) { return service.get('/appointment/my', { params }) }
export function cancelAppointment(id) { return service.put('/appointment/' + id + '/cancel') }

export function generateSummary(sessionId) { return service.post('/consultation/session/' + sessionId + '/summary/generate') }
export function getSessionSummary(sessionId) { return service.get('/consultation/session/' + sessionId + '/summary') }
export function getMySummaries() { return service.get('/consultation/my-summaries') }
