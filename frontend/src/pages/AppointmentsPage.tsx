import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { cancelAppointment, getAllAppointments, getMasterAppointments, getMyAppointments, updateAppointmentStatus } from '../api/appointments'
import { getAuth, isAdmin } from '../store/auth'
import type { Appointment, AppointmentStatus } from '../types'
import { formatDateTime, statusBadge, statusLabel } from '../utils/format'

export default function AppointmentsPage() {
  const { user } = getAuth()
  const admin = isAdmin(user?.role)
  const master = user?.role === 'EMPLOYEE'
  const showClient = admin || master
  // Мастер может записывать, но не отменять; клиент отменяет свои записи, админ — любые
  const canCancel = !master
  const [list, setList] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)

  function load() {
    const fn = admin ? getAllAppointments : master ? getMasterAppointments : getMyAppointments
    fn().then(setList).finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function handleCancel(id: number) {
    const appt = list.find(a => a.id === id)
    const note = appt?.subscriptionId ? '\nСеанс абонемента не будет списан.' : ''
    if (!confirm('Отменить запись?' + note)) return
    try {
      const updated = await cancelAppointment(id)
      setList(l => l.map(a => a.id === id ? updated : a))
      toast.success('Запись отменена')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  async function handleStatus(id: number, status: AppointmentStatus) {
    try {
      const updated = await updateAppointmentStatus(id, status)
      setList(l => l.map(a => a.id === id ? updated : a))
      toast.success('Статус обновлён')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  if (loading) return <div className="spinner" />

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Записи</h1>
        <Link to="/appointments/new" className="btn btn-primary">+ Записаться</Link>
      </div>

      <div className="card">
        {list.length === 0 ? (
          <div className="empty-state">Записей пока нет</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  {showClient && <th>Клиент</th>}
                  <th>Дата и время</th>
                  <th>Услуга</th>
                  <th>Сотрудник</th>
                  <th>Цена</th>
                  <th>Статус</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                {list.map(a => (
                  <tr key={a.id}>
                    {showClient && <td>{a.clientName}<br /><span className="text-muted text-sm">{a.clientPhone}</span></td>}
                    <td>
                      {formatDateTime(a.startTime)}<br />
                      <span className="text-muted text-sm">{a.durationMinutes} мин</span>
                    </td>
                    <td>{a.serviceName}</td>
                    <td>{a.employeeName}<br /><span className="text-muted text-sm">{a.employeeSpecialization}</span></td>
                    <td>
                      {a.subscriptionId ? <span className="badge badge-purple">Абонемент</span> : <>{a.servicePrice} ₸</>}
                    </td>
                    <td>
                      <span className={`badge ${statusBadge(a.status)}`}>{statusLabel(a.status)}</span>
                    </td>
                    <td>
                      <div className="flex gap-2">
                        {admin && a.status !== 'CANCELLED' && a.status !== 'COMPLETED' && (
                          <>
                            <button className="btn btn-sm btn-secondary" onClick={() => handleStatus(a.id, 'CONFIRMED')}>✓</button>
                            <button className="btn btn-sm btn-primary" onClick={() => handleStatus(a.id, 'COMPLETED')}>✅</button>
                          </>
                        )}
                        {canCancel && a.status !== 'CANCELLED' && a.status !== 'COMPLETED' && (
                          <button className="btn btn-sm btn-danger" title="Отменить запись" onClick={() => handleCancel(a.id)}>✕</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
