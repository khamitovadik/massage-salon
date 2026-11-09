import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import api from '../api/client'
import { getMyAppointments } from '../api/appointments'
import { getAuth, isAdmin } from '../store/auth'
import type { Appointment } from '../types'
import { formatDateTime } from '../utils/format'

interface Review {
  id: number
  appointmentId: number
  clientName: string
  employeeName: string
  serviceName: string
  rating: number
  comment: string | null
  createdAt: string
}

function pluralReviews(n: number) {
  const m10 = n % 10, m100 = n % 100
  if (m10 === 1 && m100 !== 11) return 'отзыв'
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return 'отзыва'
  return 'отзывов'
}

export default function ReviewsPage() {
  const { user } = getAuth()
  const admin = isAdmin(user?.role)
  const client = user?.role === 'CLIENT'
  const [reviews, setReviews] = useState<Review[]>([])
  const [myAppointments, setMyAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ appointmentId: '', rating: 5, comment: '' })
  const [submitting, setSubmitting] = useState(false)

  function load() {
    api.get<Review[]>('/reviews')
      .then(r => setReviews(r.data))
      .catch(() => setReviews([]))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    if (client) getMyAppointments().then(setMyAppointments).catch(() => {})
  }, [])

  // Сводка: средний рейтинг и распределение по звёздам
  const average = reviews.length ? reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length : 0
  const countByStar = (n: number) => reviews.filter(r => r.rating === n).length

  // Отзыв можно оставить только о завершённом сеансе и только один раз
  const reviewed = new Set(reviews.map(r => r.appointmentId))
  const reviewable = myAppointments.filter(a => a.status === 'COMPLETED' && !reviewed.has(a.id))

  function submit() {
    if (!form.appointmentId) return
    setSubmitting(true)
    api.post('/reviews', { appointmentId: Number(form.appointmentId), rating: form.rating, comment: form.comment.trim() || null })
      .then(() => {
        toast.success('Спасибо за отзыв!')
        setForm({ appointmentId: '', rating: 5, comment: '' })
        load()
      })
      .catch(err => toast.error(err.response?.data?.error || 'Ошибка при отправке'))
      .finally(() => setSubmitting(false))
  }

  async function remove(id: number) {
    if (!confirm('Удалить отзыв?')) return
    try {
      await api.delete(`/reviews/${id}`)
      setReviews(l => l.filter(r => r.id !== id))
      toast.success('Отзыв удалён')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 24 }}>Отзывы</h1>

      {!loading && reviews.length > 0 && (
        <div style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: 20, marginBottom: 24, maxWidth: 500, display: 'flex', gap: 24, alignItems: 'center' }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 36, fontWeight: 700, lineHeight: 1 }}>{average.toFixed(1)}</div>
            <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>{reviews.length} {pluralReviews(reviews.length)}</div>
          </div>
          <div style={{ flex: 1 }}>
            {[5, 4, 3, 2, 1].map(n => (
              <div key={n} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12 }}>
                <span style={{ width: 24 }}>{n} ★</span>
                <div style={{ flex: 1, height: 6, background: 'var(--border)', borderRadius: 3, overflow: 'hidden' }}>
                  <div style={{ width: `${(countByStar(n) / reviews.length) * 100}%`, height: '100%', background: '#f5b301' }} />
                </div>
                <span style={{ width: 20, textAlign: 'right', color: 'var(--text-muted)' }}>{countByStar(n)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {client && (
        <div style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: 24, marginBottom: 32, maxWidth: 500 }}>
          <h3 style={{ marginBottom: 16 }}>Оставить отзыв</h3>
          {reviewable.length === 0 ? (
            <div className="text-muted">Отзыв можно оставить после завершённого сеанса.</div>
          ) : (
            <>
              <select
                className="input"
                value={form.appointmentId}
                onChange={e => setForm(p => ({ ...p, appointmentId: e.target.value }))}
                style={{ width: '100%', marginBottom: 12 }}
              >
                <option value="">— Выберите сеанс —</option>
                {reviewable.map(a => (
                  <option key={a.id} value={a.id}>
                    {formatDateTime(a.startTime)} · {a.serviceName} · {a.employeeName}
                  </option>
                ))}
              </select>
              <div style={{ marginBottom: 12 }}>
                <label style={{ fontWeight: 600, marginRight: 12 }}>Оценка:</label>
                {[1, 2, 3, 4, 5].map(n => (
                  <button
                    key={n}
                    type="button"
                    aria-label={`${n} из 5`}
                    onClick={() => setForm(p => ({ ...p, rating: n }))}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 24, opacity: n <= form.rating ? 1 : 0.3 }}
                  >⭐</button>
                ))}
              </div>
              <textarea
                className="input"
                rows={3}
                maxLength={1000}
                placeholder="Ваш отзыв..."
                value={form.comment}
                onChange={e => setForm(p => ({ ...p, comment: e.target.value }))}
                style={{ width: '100%', resize: 'vertical', marginBottom: 12 }}
              />
              <button className="btn btn-primary" onClick={submit} disabled={submitting || !form.appointmentId}>
                {submitting ? 'Отправка...' : 'Отправить'}
              </button>
            </>
          )}
        </div>
      )}

      {loading ? (
        <div className="text-muted">Загрузка...</div>
      ) : reviews.length === 0 ? (
        <div className="text-muted">Отзывов пока нет.</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {reviews.map(r => (
            <div key={r.id} style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 8 }}>
                <div>
                  <strong>{r.clientName}</strong>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{r.serviceName} · мастер {r.employeeName}</div>
                </div>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                  <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                    {new Date(r.createdAt).toLocaleDateString('ru-RU')}
                  </span>
                  {admin && (
                    <button className="btn btn-sm btn-danger" title="Удалить отзыв" onClick={() => remove(r.id)}>✕</button>
                  )}
                </div>
              </div>
              <div style={{ marginBottom: 8 }}>{'⭐'.repeat(r.rating)}</div>
              {r.comment && <div style={{ color: 'var(--text)' }}>{r.comment}</div>}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
