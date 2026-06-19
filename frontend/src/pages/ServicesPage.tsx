import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { getServices, createService, deleteService } from '../api/services'
import { getAuth, isAdmin } from '../store/auth'
import type { SalonService } from '../types'

export default function ServicesPage() {
  const { user } = getAuth()
  const admin = isAdmin(user?.role)
  const [list, setList] = useState<SalonService[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({ name: '', description: '', price: '', durationMinutes: '' })

  useEffect(() => {
    getServices().then(setList).finally(() => setLoading(false))
  }, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const s = await createService({
        name: form.name,
        description: form.description || undefined,
        price: Number(form.price),
        durationMinutes: Number(form.durationMinutes),
      })
      setList(l => [...l, s])
      setForm({ name: '', description: '', price: '', durationMinutes: '' })
      setShowForm(false)
      toast.success('Услуга добавлена')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Деактивировать услугу?')) return
    try {
      await deleteService(id)
      setList(l => l.filter(s => s.id !== id))
      toast.success('Услуга деактивирована')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  if (loading) return <div className="spinner" />

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">💆 Услуги</h1>
        {admin && (
          <button className="btn btn-primary" onClick={() => setShowForm(v => !v)}>
            {showForm ? 'Отмена' : '+ Добавить услугу'}
          </button>
        )}
      </div>

      {showForm && admin && (
        <div className="card mb-4">
          <h2 style={{ fontWeight: 600, marginBottom: 16 }}>Новая услуга</h2>
          <form onSubmit={handleCreate}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="field">
                <label className="label">Название</label>
                <input className="input" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} required />
              </div>
              <div className="field">
                <label className="label">Описание</label>
                <input className="input" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
              </div>
              <div className="field">
                <label className="label">Цена (₸)</label>
                <input className="input" type="number" min="0" value={form.price} onChange={e => setForm(f => ({ ...f, price: e.target.value }))} required />
              </div>
              <div className="field">
                <label className="label">Длительность (мин)</label>
                <input className="input" type="number" min="1" value={form.durationMinutes} onChange={e => setForm(f => ({ ...f, durationMinutes: e.target.value }))} required />
              </div>
            </div>
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? 'Сохранение...' : 'Сохранить'}
            </button>
          </form>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 16 }}>
        {list.map(s => (
          <div key={s.id} className="card" style={{ position: 'relative' }}>
            {!s.active && (
              <span className="badge badge-gray" style={{ position: 'absolute', top: 12, right: 12 }}>
                Неактивна
              </span>
            )}
            <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 4 }}>{s.name}</div>
            {s.description && <p style={{ color: 'var(--text-muted)', fontSize: 13, marginBottom: 12 }}>{s.description}</p>}
            <div className="flex gap-3" style={{ marginTop: 'auto' }}>
              <div>
                <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--brand)' }}>{s.price} ₸</div>
                <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{s.durationMinutes} минут</div>
              </div>
              {admin && s.active && (
                <button
                  className="btn btn-sm btn-danger"
                  style={{ marginLeft: 'auto' }}
                  onClick={() => handleDelete(s.id)}
                >
                  Удалить
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
