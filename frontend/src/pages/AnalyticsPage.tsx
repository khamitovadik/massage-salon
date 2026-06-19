import { useEffect, useState } from 'react'
import api from '../api/client'

interface Stats {
  totalAppointments: number
  completedAppointments: number
  cancelledAppointments: number
  totalRevenue: number
  activeSubscriptions: number
  totalClients: number
}

export default function AnalyticsPage() {
  const [stats, setStats] = useState<Stats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/analytics/summary')
      .then(r => setStats(r.data))
      .catch(() => setError('Не удалось загрузить аналитику'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-muted">Загрузка...</div>
  if (error) return <div style={{ color: 'red' }}>{error}</div>

  return (
    <div>
      <h1 style={{ marginBottom: 24 }}>📊 Аналитика</h1>

      {stats && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16 }}>
          <StatCard label="Всего записей" value={stats.totalAppointments} color="#6366f1" />
          <StatCard label="Завершено" value={stats.completedAppointments} color="#22c55e" />
          <StatCard label="Отменено" value={stats.cancelledAppointments} color="#ef4444" />
          <StatCard label="Выручка (₸)" value={stats.totalRevenue.toLocaleString()} color="#f59e0b" />
          <StatCard label="Абонементы" value={stats.activeSubscriptions} color="#8b5cf6" />
          <StatCard label="Клиентов" value={stats.totalClients} color="#06b6d4" />
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value, color }: { label: string; value: number | string; color: string }) {
  return (
    <div style={{
      background: '#fff', border: '1px solid var(--border)', borderRadius: 12,
      padding: '20px 24px', borderTop: `4px solid ${color}`
    }}>
      <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 8 }}>{label}</div>
      <div style={{ fontSize: 28, fontWeight: 700, color }}>{value}</div>
    </div>
  )
}
