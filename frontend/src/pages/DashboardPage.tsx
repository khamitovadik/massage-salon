import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllAppointments, getMasterAppointments, getMyAppointments } from '../api/appointments'
import { getAllSubscriptions, getMyActiveSubscriptions } from '../api/subscriptions'
import { getAuth, isAdmin } from '../store/auth'
import TelegramConnect from '../components/TelegramConnect'
import type { Appointment, Subscription } from '../types'
import { formatDateTime, statusBadge, statusLabel } from '../utils/format'

export default function DashboardPage() {
  const { user } = getAuth()
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [subs, setSubs] = useState<Subscription[]>([])
  const [loading, setLoading] = useState(true)

  const admin = isAdmin(user?.role)
  const master = user?.role === 'EMPLOYEE'

  useEffect(() => {
    // Руководитель видит весь салон, мастер — свои записи, клиент — свои записи и абонементы
    const appts = admin ? getAllAppointments() : master ? getMasterAppointments() : getMyAppointments()
    const activeSubs = admin
      ? getAllSubscriptions().then(l => l.filter(s => s.status === 'ACTIVE'))
      : master ? Promise.resolve([]) : getMyActiveSubscriptions()
    Promise.all([appts, activeSubs])
      .then(([a, s]) => { setAppointments(a); setSubs(s) })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="spinner" />

  // Список приходит от поздних к ранним — ближайшие идут первыми после разворота
  const upcoming = appointments
    .filter(a => a.status !== 'CANCELLED' && a.status !== 'COMPLETED' && new Date(a.endTime) > new Date())
    .slice()
    .sort((x, y) => x.startTime.localeCompare(y.startTime))
  const nearest = upcoming.slice(0, 5)

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Привет, {user?.name}!</h1>
        <Link to="/appointments/new" className="btn btn-primary">+ Новая запись</Link>
      </div>

      <TelegramConnect />

      {/* Stats */}
      <div className="stat-grid">
        <div className="card stat-card">
          <div className="stat-value" style={{ color: 'var(--brand)' }}>{upcoming.length}</div>
          <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>Предстоящих записей</div>
        </div>
        <div className="card stat-card">
          <div className="stat-value" style={{ color: 'var(--success)' }}>{subs.length}</div>
          <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>Активных абонементов</div>
        </div>
        <div className="card stat-card">
          <div className="stat-value" style={{ color: 'var(--warning)' }}>
            {subs.reduce((sum, s) => sum + s.remainingSessions, 0)}
          </div>
          <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>Сеансов по абонементам</div>
        </div>
      </div>

      {/* Recent appointments */}
      <div className="card">
        <div className="flex justify-between items-center mb-4">
          <h2 style={{ fontWeight: 600, fontSize: 16 }}>Ближайшие записи</h2>
          <Link to="/appointments" className="btn btn-secondary btn-sm">Все записи</Link>
        </div>
        {upcoming.length === 0 ? (
          <div className="empty-state">
            Нет предстоящих записей.{' '}
            <Link to="/appointments/new">Записаться?</Link>
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Дата и время</th>
                  <th>Услуга</th>
                  <th>Сотрудник</th>
                  <th>Статус</th>
                </tr>
              </thead>
              <tbody>
                {nearest.map(a => (
                  <tr key={a.id}>
                    <td>{formatDateTime(a.startTime)}</td>
                    <td>{a.serviceName}</td>
                    <td>{a.employeeName}</td>
                    <td><span className={`badge ${statusBadge(a.status)}`}>{statusLabel(a.status)}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Active subscriptions */}
      {subs.length > 0 && (
        <div className="card mt-4">
          <div className="flex justify-between items-center mb-4">
            <h2 style={{ fontWeight: 600, fontSize: 16 }}>Активные абонементы</h2>
            <Link to="/subscriptions" className="btn btn-secondary btn-sm">Все абонементы</Link>
          </div>
          <div style={{ display: 'grid', gap: 12 }}>
            {subs.map(s => (
              <div key={s.id} style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                padding: '12px 16px', background: 'var(--brand-light)', borderRadius: 8
              }}>
                <div>
                  <div style={{ fontWeight: 500 }}>{s.serviceName}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                    Действует до {s.expiryDate}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontWeight: 700, fontSize: 18, color: 'var(--brand)' }}>
                    {s.remainingSessions}/{s.totalSessions}
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>сеансов</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
