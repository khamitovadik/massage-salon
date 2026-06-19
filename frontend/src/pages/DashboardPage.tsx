import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyAppointments } from '../api/appointments'
import { getMyActiveSubscriptions } from '../api/subscriptions'
import { getAuth } from '../store/auth'
import type { Appointment, Subscription } from '../types'
import { formatDateTime, statusBadge } from '../utils/format'

export default function DashboardPage() {
  const { user } = getAuth()
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [subs, setSubs] = useState<Subscription[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getMyAppointments(), getMyActiveSubscriptions()])
      .then(([a, s]) => { setAppointments(a.slice(0, 5)); setSubs(s) })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="spinner" />

  const upcoming = appointments.filter(a => a.status !== 'CANCELLED' && a.status !== 'COMPLETED')

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">👋 Привет, {user?.name}!</h1>
        <Link to="/appointments/new" className="btn btn-primary">+ Новая запись</Link>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 32 }}>
        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--brand)' }}>{upcoming.length}</div>
          <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>Предстоящих записей</div>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--success)' }}>{subs.length}</div>
          <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>Активных абонементов</div>
        </div>
        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--warning)' }}>
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
                {upcoming.map(a => (
                  <tr key={a.id}>
                    <td>{formatDateTime(a.startTime)}</td>
                    <td>{a.serviceName}</td>
                    <td>{a.employeeName}</td>
                    <td><span className={`badge ${statusBadge(a.status)}`}>{a.status}</span></td>
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
