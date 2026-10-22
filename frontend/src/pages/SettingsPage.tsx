import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Check, Clock, Lightbulb, MapPin, Phone, Store, Ticket } from 'lucide-react'
import api from '../api/client'
import Logo from '../components/Logo'

interface SalonSettings {
  name: string
  address: string
  phone: string
  email: string
  description: string
  workingHoursStart: string
  workingHoursEnd: string
  annualPrice: string
  annualSessions: string
  annualDescription: string
}

const EMPTY: SalonSettings = {
  name: '', address: '', phone: '', email: '',
  description: '', workingHoursStart: '09:00', workingHoursEnd: '21:00',
  annualPrice: '', annualSessions: '', annualDescription: ''
}

export default function SettingsPage() {
  const [form, setForm] = useState<SalonSettings>(EMPTY)
  const [saved, setSaved] = useState<SalonSettings>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.get('/settings')
      .then(r => {
        const data = { ...EMPTY, ...r.data }
        for (const key of ['annualPrice', 'annualSessions', 'annualDescription'] as const) {
          data[key] = data[key] == null ? '' : String(data[key])
        }
        setForm(data); setSaved(data)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  function save() {
    setSaving(true)
    api.put('/settings', form)
      .then(() => { setSaved(form); toast.success('Настройки сохранены') })
      .catch(() => toast.error('Не удалось сохранить настройки'))
      .finally(() => setSaving(false))
  }

  const set = (key: keyof SalonSettings) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(p => ({ ...p, [key]: e.target.value }))

  const dirty = JSON.stringify(form) !== JSON.stringify(saved)
  const hours = saved.workingHoursStart && saved.workingHoursEnd
    ? `${saved.workingHoursStart.slice(0, 5)} – ${saved.workingHoursEnd.slice(0, 5)}`
    : ''

  if (loading) return <div className="spinner" />

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 className="page-title">Настройки</h1>
        <p className="page-subtitle">Информация о вашем салоне, контакты и часы работы.</p>
      </div>

      <div className="settings-grid">
        <div className="card" style={{ padding: 0 }}>
          <div className="settings-head">
            <span className="icon-tile"><Store size={18} /></span>
            <div>
              <h2 className="section-title">Основная информация</h2>
              <p className="text-muted text-sm">Эти данные используются в работе вашего салона.</p>
            </div>
          </div>

          <div className="settings-body">
            <div className="field">
              <label className="label">Название салона</label>
              <input className="input" value={form.name} onChange={set('name')} placeholder="Em.Del.Relaxx" />
            </div>
            <div className="field">
              <label className="label">Адрес</label>
              <div className="input-icon">
                <input className="input" value={form.address} onChange={set('address')} placeholder="Укажите адрес салона" />
                <MapPin size={16} />
              </div>
            </div>
            <div className="field-row">
              <div className="field">
                <label className="label">Телефон</label>
                <input className="input" type="tel" value={form.phone} onChange={set('phone')} placeholder="+7 (___) ___-__-__" />
              </div>
              <div className="field">
                <label className="label">Email</label>
                <input className="input" type="email" value={form.email} onChange={set('email')} placeholder="example@mail.ru" />
              </div>
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label className="label">Описание</label>
              <textarea className="input" rows={3} value={form.description} onChange={set('description')}
                placeholder="Расскажите о вашем салоне" style={{ resize: 'vertical' }} />
            </div>
          </div>

          <div className="settings-body" style={{ borderTop: '1px solid var(--border)' }}>
            <h3 className="subsection-title"><Clock size={16} color="var(--brand)" /> Часы работы</h3>
            <div className="field-row">
              <div className="field">
                <label className="label">Открытие</label>
                <input type="time" className="input" value={form.workingHoursStart} onChange={set('workingHoursStart')} />
              </div>
              <div className="field">
                <label className="label">Закрытие</label>
                <input type="time" className="input" value={form.workingHoursEnd} onChange={set('workingHoursEnd')} />
              </div>
            </div>
            <p className="text-muted" style={{ fontSize: 12 }}>Укажите время начала и окончания рабочего дня салона.</p>
          </div>

          <div className="settings-body" style={{ borderTop: '1px solid var(--border)' }}>
            <h3 className="subsection-title"><Ticket size={16} color="var(--brand)" /> Годовой абонемент</h3>
            <div className="field-row">
              <div className="field">
                <label className="label">Стоимость, ₸</label>
                <input type="number" min="0" className="input" value={form.annualPrice} onChange={set('annualPrice')} placeholder="Например, 300000" />
              </div>
              <div className="field">
                <label className="label">Количество сеансов</label>
                <input type="number" min="1" className="input" value={form.annualSessions} onChange={set('annualSessions')} placeholder="Например, 48" />
              </div>
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label className="label">Описание для клиентов</label>
              <textarea className="input" rows={3} value={form.annualDescription} onChange={set('annualDescription')}
                placeholder="Что входит, кому подходит, бонусы..." style={{ resize: 'vertical' }} />
            </div>
            <p className="text-muted" style={{ fontSize: 12, marginTop: 8 }}>Эти условия клиенты видят в разделе «Абонементы».</p>
          </div>

          <div className="settings-foot">
            <span className="text-muted" style={{ fontSize: 12 }}>
              {dirty ? 'Есть несохранённые изменения' : 'Изменения применятся после сохранения'}
            </span>
            <div className="flex gap-2">
              <button className="btn btn-ghost" onClick={() => setForm(saved)} disabled={!dirty || saving}>Отмена</button>
              <button className="btn btn-primary" onClick={save} disabled={saving}>
                <Check size={16} /> {saving ? 'Сохранение...' : 'Сохранить'}
              </button>
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="card">
            <div className="text-sm" style={{ fontWeight: 600, marginBottom: 16 }}>Ваш салон</div>
            <div className="text-center" style={{ paddingBottom: 16, borderBottom: '1px solid var(--border)' }}>
              <div style={{ display: 'inline-block', marginBottom: 12 }}><Logo size={56} variant="soft" /></div>
              <div style={{ fontWeight: 600, fontSize: 16 }}>{saved.name || 'Em.Del.Relaxx'}</div>
              <div className="text-muted" style={{ fontSize: 12, marginTop: 2 }}>
                {saved.description || 'Массаж и забота о себе'}
              </div>
            </div>
            <ul className="salon-facts">
              <li><MapPin size={15} />{saved.address || 'Адрес не указан'}</li>
              <li><Phone size={15} />{saved.phone || 'Телефон не указан'}</li>
              <li><Clock size={15} />{hours || 'Часы работы не указаны'}</li>
            </ul>
          </div>

          <div className="tip-card">
            <div className="tip-title"><Lightbulb size={16} /> Начните с главного</div>
            <p>Добавьте адрес и контакты, чтобы вся важная информация о салоне была под рукой у администратора.</p>
            <p className="tip-note">Не забудьте сохранить изменения перед выходом из настроек.</p>
          </div>
        </div>
      </div>
    </div>
  )
}
