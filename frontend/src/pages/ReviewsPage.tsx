import { useEffect, useState } from 'react'
import api from '../api/client'

interface Review {
  id: number
  clientName: string
  rating: number
  comment: string
  createdAt: string
  approved: boolean
}

export default function ReviewsPage() {
  const [reviews, setReviews] = useState<Review[]>([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ rating: 5, comment: '' })
  const [submitting, setSubmitting] = useState(false)
  const [msg, setMsg] = useState('')

  function load() {
    api.get('/reviews')
      .then(r => setReviews(r.data))
      .catch(() => setReviews([]))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  function submit() {
    if (!form.comment.trim()) return
    setSubmitting(true)
    setMsg('')
    api.post('/reviews', form)
      .then(() => {
        setMsg('✅ Отзыв отправлен на модерацию')
        setForm({ rating: 5, comment: '' })
        load()
      })
      .catch(() => setMsg('❌ Ошибка при отправке'))
      .finally(() => setSubmitting(false))
  }

  function stars(n: number) {
    return '⭐'.repeat(n)
  }

  return (
    <div>
      <h1 style={{ marginBottom: 24 }}>⭐ Отзывы</h1>

      {/* Форма отзыва */}
      <div style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: 12, padding: 24, marginBottom: 32, maxWidth: 500 }}>
        <h3 style={{ marginBottom: 16 }}>Оставить отзыв</h3>
        <div style={{ marginBottom: 12 }}>
          <label style={{ fontWeight: 600, marginRight: 12 }}>Оценка:</label>
          {[1,2,3,4,5].map(n => (
            <button
              key={n}
              onClick={() => setForm(p => ({ ...p, rating: n }))}
              style={{
                background: 'none', border: 'none', cursor: 'pointer', fontSize: 24,
                opacity: n <= form.rating ? 1 : 0.3
              }}
            >⭐</button>
          ))}
        </div>
        <textarea
          className="input"
          rows={3}
          placeholder="Ваш отзыв..."
          value={form.comment}
          onChange={e => setForm(p => ({ ...p, comment: e.target.value }))}
          style={{ width: '100%', resize: 'vertical', marginBottom: 12 }}
        />
        {msg && <div style={{ marginBottom: 8, color: msg.startsWith('✅') ? '#22c55e' : '#ef4444' }}>{msg}</div>}
        <button className="btn btn-primary" onClick={submit} disabled={submitting}>
          {submitting ? 'Отправка...' : 'Отправить'}
        </button>
      </div>

      {/* Список отзывов */}
      {loading ? (
        <div className="text-muted">Загрузка...</div>
      ) : reviews.length === 0 ? (
        <div className="text-muted">Отзывов пока нет.</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {reviews.filter(r => r.approved).map(r => (
            <div key={r.id} style={{
              background: '#fff', border: '1px solid var(--border)', borderRadius: 12, padding: 20
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <strong>{r.clientName}</strong>
                <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                  {new Date(r.createdAt).toLocaleDateString('ru-RU')}
                </span>
              </div>
              <div style={{ marginBottom: 8 }}>{stars(r.rating)}</div>
              <div style={{ color: 'var(--text)' }}>{r.comment}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
