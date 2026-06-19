import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { createAppointment } from '../api/appointments'
import { getEmployees } from '../api/employees'
import { getServices } from '../api/services'
import type { Employee, SalonService } from '../types'

export default function NewAppointmentPage() {
  const navigate = useNavigate()
  const [employees, setEmployees] = useState<Employee[]>([])
  const [services, setServices] = useState<SalonService[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const [form, setForm] = useState({
    employeeId: '',
    serviceId: '',
    startTime: '',
    comment: '',
  })

  useEffect(() => {
    Promise.all([getEmployees(), getServices()])
      .then(([e, s]) => { setEmployees(e); setServices(s) })
      .finally(() => setLoading(false))
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.employeeId || !form.serviceId || !form.startTime) {
      toast.error('Заполните все поля')
      return
    }
    setSaving(true)
    try {
      await createAppointment({
        employeeId: Number(form.employeeId),
        serviceId: Number(form.serviceId),
        startTime: new Date(form.startTime).toISOString().slice(0, 19),
        comment: form.comment || undefined,
      })
      toast.success('Запись создана!')
      navigate('/appointments')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка создания записи')
    } finally {
      setSaving(false)
    }
  }

  const selectedService = services.find(s => s.id === Number(form.serviceId))

  if (loading) return <div className="spinner" />

  return (
    <div style={{ maxWidth: 520 }}>
      <div className="page-header">
        <h1 className="page-title">+ Новая запись</h1>
      </div>

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label className="label">Сотрудник</label>
            <select
              className="input"
              value={form.employeeId}
              onChange={e => setForm(f => ({ ...f, employeeId: e.target.value }))}
              required
            >
              <option value="">— Выберите сотрудника —</option>
              {employees.map(em => (
                <option key={em.id} value={em.id}>
                  {em.name} — {em.specialization}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label className="label">Услуга</label>
            <select
              className="input"
              value={form.serviceId}
              onChange={e => setForm(f => ({ ...f, serviceId: e.target.value }))}
              required
            >
              <option value="">— Выберите услугу —</option>
              {services.map(s => (
                <option key={s.id} value={s.id}>
                  {s.name} — {s.price} ₸ ({s.durationMinutes} мин)
                </option>
              ))}
            </select>
          </div>

          {selectedService && (
            <div style={{
              background: 'var(--brand-light)', borderRadius: 8, padding: '10px 14px',
              marginBottom: 16, fontSize: 13, color: 'var(--brand-dark)'
            }}>
              ⏱ {selectedService.durationMinutes} мин · 💰 {selectedService.price} ₸
              {selectedService.description && <> · {selectedService.description}</>}
            </div>
          )}

          <div className="field">
            <label className="label">Дата и время</label>
            <input
              className="input"
              type="datetime-local"
              value={form.startTime}
              onChange={e => setForm(f => ({ ...f, startTime: e.target.value }))}
              required
            />
          </div>

          <div className="field">
            <label className="label">Комментарий (необязательно)</label>
            <textarea
              className="input"
              rows={3}
              value={form.comment}
              onChange={e => setForm(f => ({ ...f, comment: e.target.value }))}
              placeholder="Пожелания, особенности..."
              style={{ resize: 'vertical' }}
            />
          </div>

          <div className="flex gap-3">
            <button type="button" className="btn btn-ghost" onClick={() => navigate(-1)}>
              Назад
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving} style={{ flex: 1 }}>
              {saving ? 'Сохранение...' : '📅 Записаться'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
