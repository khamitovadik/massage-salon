import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import {
  getMySubscriptions,
  getAllSubscriptions,
  createSubscription,
  cancelSubscription,
  useSubscriptionSession
} from '../api/subscriptions'
import { getServices } from '../api/services'
import { getAuth, isAdmin } from '../store/auth'
import type { SalonService, Subscription } from '../types'
import { subStatusBadge, subStatusLabel } from '../utils/format'

export default function SubscriptionsPage() {
  const { user } = getAuth()
  const admin = isAdmin(user?.role)
  const [list, setList] = useState<Subscription[]>([])
  const [services, setServices] = useState<SalonService[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    serviceId: '', totalSessions: '10', startDate: '', expiryDate: '', notes: ''
  })

  useEffect(() => {
    const fetchData = admin ? getAllSubscriptions : getMySubscriptions
    Promise.all([fetchData(), getServices()])
      .then(([subs, svcs]) => { setList(subs); setServices(svcs) })
      .finally(() => setLoading(false))
  }, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const sub = await createSubscription({
        serviceId: Number(form.serviceId),
        totalSessions: Number(form.totalSessions),
        startDate: form.startDate,
        expiryDate: form.expiryDate,
        notes: form.notes || undefined,
      })
      setList(l => [sub, ...l])
      setShowForm(false)
      toast.success('Абонемент создан')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    } finally {
      setSaving(false)
    }
  }

  async function handleUse(id: number) {
    try {
      const updated = await useSubscriptionSession(id)
      setList(l => l.map(s => s.id === id ? updated : s))
      toast.success('Сеанс списан')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  async function handleCancel(id: number) {
    if (!confirm('Отменить абонемент?')) return
    try {
      const updated = await cancelSubscription(id)
      setList(l => l.map(s => s.id === id ? updated : s))
      toast.success('Абонемент отменён')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  if (loading) return <div className="spinner" />

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">🎫 Абонементы</h1>
        {admin && (
          <button className="btn btn-primary" onClick={() => setShowForm(v => !v)}>
            {showForm ? 'Отмена' : '+ Выдать абонемент'}
          </button>
        )}
      </div>

      {showForm && admin && (
        <div className="card mb-4">
          <h2 style={{ fontWeight: 600, marginBottom: 16 }}>Новый абонемент</h2>
          <form onSubmit={handleCreate}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="field">
                <label className="label">Услуга</label>
                <select className="input" value={form.serviceId} onChange={e => setForm(f => ({ ...f, serviceId: e.target.value }))} required>
                  <option value="">— Выберите услугу —</option>
                  {services.map(s => (
                    <option key={s.id} value={s.id}>{s.name}</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label className="label">Количество сеансов</label>
                <input className="input" type="number" min="1" value={form.totalSessions} onChange={e => setForm(f => ({ ...f, totalSessions: e.target.value }))} required />
              </div>
              <div className="field">
                <label className="label">Дата начала</label>
                <input className="input" type="date" value={form.startDate} onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))} required />
              </div>
              <div className="field">
                <label className="label">Дата окончания</label>
                <input className="input" type="date" value={form.expiryDate} onChange={e => setForm(f => ({ ...f, expiryDate: e.target.value }))} required />
              </div>
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label className="label">Примечание</label>
                <input className="input" value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} />
              </div>
            </div>
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? 'Сохранение...' : 'Выдать абонемент'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {list.length === 0 ? (
          <div className="empty-state">Абонементов пока нет</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  {admin && <th>Клиент</th>}
                  <th>Услуга</th>
                  <th>Сеансы</th>
                  <th>Действует</th>
                  <th>Статус</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                {list.map(s => (
                  <tr key={s.id}>
                    {admin && <td>{s.clientName}<br /><span className="text-muted text-sm">{s.clientPhone}</span></td>}
                    <td>{s.serviceName}<br /><span className="text-muted text-sm">{s.durationMinutes} мин</span></td>
                    <td>
                      <span style={{ fontWeight: 700, color: 'var(--brand)' }}>{s.remainingSessions}</span>
                      <span style={{ color: 'var(--text-muted)' }}>/{s.totalSessions}</span>
                    </td>
                    <td>
                      {s.startDate} — {s.expiryDate}
                    </td>
                    <td>
                      <span className={`badge ${subStatusBadge(s.status)}`}>{subStatusLabel(s.status)}</span>
                    </td>
                    <td>
                      <div className="flex gap-2">
                        {s.status === 'ACTIVE' && s.remainingSessions > 0 && (
                          <button className="btn btn-sm btn-secondary" onClick={() => handleUse(s.id)}>
                            − Сеанс
                          </button>
                        )}
                        {admin && s.status === 'ACTIVE' && (
                          <button className="btn btn-sm btn-danger" onClick={() => handleCancel(s.id)}>
                            Отменить
                          </button>
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
