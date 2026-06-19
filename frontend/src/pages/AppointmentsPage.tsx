import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { cancelAppointment, getAllAppointments, getMyAppointments, updateAppointmentStatus } from '../api/appointments'
import { getAuth, isAdmin } from '../store/auth'
import type { Appointment, AppointmentStatus } from '../types'
import { formatDateTime, statusBadge, statusLabel } from '../utils/format'

export default function AppointmentsPage() {
  const { user } = getAuth()
  const admin = isAdmin(user?.role)
  const [list, setList] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)

  function load() {
    const fn = admin ? getAllAppointments : getMyAppointments
    fn().then(setList).finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function handleCancel(id: number) {
    if (!confirm('Отменить запись?')) return
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
        <h1 className="page-title">📅 Записи</h1>
        <Link to="/appointments/new" className="btn btn-primary">+ Новая запись</Link>
      </div>

      <div className="card">
        {list.length === 0 ? (
          <div className="empty-state">Записей пока нет</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  {admin && <th>Клиент</th>}
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
                    {admin && <td>{a.clientName}<br /><span className="text-muted text-sm">{a.clientPhone}</span></td>}
                    <td>
                      {formatDateTime(a.startTime)}<br />
                      <span className="text-muted text-sm">{a.durationMinutes} мин</span>
                    </td>
                    <td>{a.serviceName}</td>
                    <td>{a.employeeName}<br /><span className="text-muted text-sm">{a.employeeSpecialization}</span></td>
                    <td>{a.servicePrice} ₸</td>
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
                        {a.status !== 'CANCELLED' && a.status !== 'COMPLETED' && (
                          <button className="btn btn-sm btn-danger" onClick={() => handleCancel(a.id)}>✕</button>
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
