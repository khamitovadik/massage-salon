import { useEffect, useState } from 'react'
import api from '../api/client'

interface SalonSettings {
  name: string
  address: string
  phone: string
  email: string
  description: string
  workingHoursStart: string
  workingHoursEnd: string
}

export default function SettingsPage() {
  const [form, setForm] = useState<SalonSettings>({
    name: '', address: '', phone: '', email: '',
    description: '', workingHoursStart: '09:00', workingHoursEnd: '21:00'
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => {
    api.get('/settings')
      .then(r => setForm(r.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  function save() {
    setSaving(true)
    setMsg('')
    api.put('/settings', form)
      .then(() => setMsg('✅ Настройки сохранены'))
      .catch(() => setMsg('❌ Ошибка при сохранении'))
      .finally(() => setSaving(false))
  }

  function field(label: string, key: keyof SalonSettings, type = 'text') {
    return (
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', fontWeight: 600, marginBottom: 6 }}>{label}</label>
        {key === 'description' ? (
          <textarea
            className="input"
            rows={3}
            value={form[key]}
            onChange={e => setForm(p => ({ ...p, [key]: e.target.value }))}
            style={{ width: '100%', resize: 'vertical' }}
          />
        ) : (
          <input
            type={type}
            className="input"
            value={form[key]}
            onChange={e => setForm(p => ({ ...p, [key]: e.target.value }))}
            style={{ width: '100%', maxWidth: 400 }}
          />
        )}
      </div>
    )
  }

  if (loading) return <div className="text-muted">Загрузка...</div>

  return (
    <div>
      <h1 style={{ marginBottom: 24 }}>⚙️ Настройки салона</h1>

      <div style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: 12, padding: 24, maxWidth: 600 }}>
        {field('Название салона', 'name')}
        {field('Адрес', 'address')}
        {field('Телефон', 'phone', 'tel')}
        {field('Email', 'email', 'email')}
        {field('Описание', 'description')}

        <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', fontWeight: 600, marginBottom: 6 }}>Открытие</label>
            <input type="time" className="input" value={form.workingHoursStart}
              onChange={e => setForm(p => ({ ...p, workingHoursStart: e.target.value }))} style={{ width: '100%' }} />
          </div>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', fontWeight: 600, marginBottom: 6 }}>Закрытие</label>
            <input type="time" className="input" value={form.workingHoursEnd}
              onChange={e => setForm(p => ({ ...p, workingHoursEnd: e.target.value }))} style={{ width: '100%' }} />
          </div>
        </div>

        {msg && <div style={{ marginBottom: 12, color: msg.startsWith('✅') ? '#22c55e' : '#ef4444' }}>{msg}</div>}

        <button className="btn btn-primary" onClick={save} disabled={saving}>
          {saving ? 'Сохранение...' : 'Сохранить'}
        </button>
      </div>
    </div>
  )
}
