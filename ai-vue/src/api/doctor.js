import service from '@/utils/request'

export function getTodayAppointments() { return service.get('/doctor/appointments/today') }
export function getDoctorAppointments(params) { return service.get('/doctor/appointments', { params }) }
export function completeAppointment(id) { return service.put('/doctor/appointment/' + id + '/complete') }
export function getPatientSummaries(userId) { return service.get('/doctor/patient/' + userId + '/summaries') }
export function getBriefing(appointmentId) { return service.get('/doctor/briefing/' + appointmentId) }
export function getLatestBriefing(userId) { return service.get('/doctor/briefing/my-patient/' + userId) }
