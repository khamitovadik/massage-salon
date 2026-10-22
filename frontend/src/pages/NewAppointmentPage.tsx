import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import dayjs from 'dayjs'
import { createAppointment } from '../api/appointments'
import { getEmployees } from '../api/employees'
import { getHourlySlots } from '../api/schedule'
import { getServices } from '../api/services'
import { getClientSubscriptions, getMyActiveSubscriptions } from '../api/subscriptions'
import { getClients } from '../api/users'
import { getAuth, isStaff } from '../store/auth'
import type { ClientShort, Employee, HourSlot, SalonService, Subscription } from '../types'

const DAYS_AHEAD = 14

const SLOT_LABEL: Record<string, string> = {
  BUSY: 'Занято',
  BLOCKED: 'Недоступно',
  PAST: 'Прошло',
}

export default function NewAppointmentPage() {
  const navigate = useNavigate()
  const { user } = getAuth()
  const staff = isStaff(user?.role)

  const [employees, setEmployees] = useState<Employee[]>([])
  const [services, setServices] = useState<SalonService[]>([])
  const [clients, setClients] = useState<ClientShort[]>([])
  const [subs, setSubs] = useState<Subscription[]>([])
  const [slots, setSlots] = useState<HourSlot[]>([])
  const [loading, setLoading] = useState(true)
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  const [employeeId, setEmployeeId] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [clientId, setClientId] = useState('')
  const [subscriptionId, setSubscriptionId] = useState('')
  const [date, setDate] = useState(dayjs().format('YYYY-MM-DD'))
  const [time, setTime] = useState('')
  const [comment, setComment] = useState('')

  useEffect(() => {
    Promise.all([getEmployees(), getServices(), staff ? getClients() : Promise.resolve([])])
      .then(([e, s, c]) => {
        setEmployees(e)
        setServices(s)
        setClients(c)
        // Мастер по умолчанию записывает к себе
        const me = e.find(em => em.userId === user?.id)
        if (user?.role === 'EMPLOYEE' && me) setEmployeeId(String(me.id))
      })
      .finally(() => setLoading(false))
  }, [])

  function loadSlots() {
    if (!employeeId) { setSlots([]); return }
    setSlotsLoading(true)
    getHourlySlots(Number(employeeId), date)
      .then(setSlots)
      .catch(() => setSlots([]))
      .finally(() => setSlotsLoading(false))
  }

  useEffect(() => { setTime(''); loadSlots() }, [employeeId, date])

  // Абонементы клиента, подходящие под выбранную услугу
  useEffect(() => {
    setSubscriptionId('')
    if (staff && !clientId) { setSubs([]); return }
    const load = staff ? getClientSubscriptions(Number(clientId)) : getMyActiveSubscriptions()
    load.then(list => setSubs(list.filter(s => s.status === 'ACTIVE' && s.remainingSessions > 0)))
      .catch(() => setSubs([]))
  }, [clientId])

  const service = services.find(s => s.id === Number(serviceId))
  const hoursNeeded = Math.max(1, Math.ceil((service?.durationMinutes ?? 60) / 60))
  const matchingSubs = subs.filter(s => s.serviceId === Number(serviceId))

  // Окошко можно выбрать, если свободны все часы, которые займёт услуга
  const selectable = useMemo(() => slots.map((s, i) =>
    s.status === 'FREE' && slots.slice(i, i + hoursNeeded).length === hoursNeeded
      && slots.slice(i, i + hoursNeeded).every(x => x.status === 'FREE')
  ), [slots, hoursNeeded])

  const selectedIdx = slots.findIndex(s => s.time === time)
  const days = Array.from({ length: DAYS_AHEAD }, (_, i) => dayjs().add(i, 'day'))

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (staff && !clientId) return toast.error('Выберите клиента')
    if (!serviceId || !employeeId) return toast.error('Выберите услугу и мастера')
    if (!time) return toast.error('Выберите свободное окошко')

    setSaving(true)
    try {
      await createAppointment({
        employeeId: Number(employeeId),
        serviceId: Number(serviceId),
        startTime: `${date}T${time}:00`,
        comment: comment || undefined,
        clientId: staff ? Number(clientId) : undefined,
        subscriptionId: subscriptionId ? Number(subscriptionId) : undefined,
      })
      toast.success('Запись создана!')
      navigate('/appointments')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка создания записи')
      loadSlots()
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="spinner" />

  return (
    <div style={{ maxWidth: 760 }}>
      <div className="page-header">
        <div>
          <h1 className="page-title">Записаться</h1>
          <p className="page-subtitle">Выберите услугу, мастера и свободное окошко — каждое длится 1 час.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="card">
        {staff && (
          <div className="field">
            <label className="label">Клиент</label>
            <select className="input" value={clientId} onChange={e => setClientId(e.target.value)} required>
              <option value="">— Выберите клиента —</option>
              {clients.map(c => <option key={c.id} value={c.id}>{c.name}{c.phone ? ` · ${c.phone}` : ''}</option>)}
            </select>
          </div>
        )}

        <div className="field-row">
          <div className="field">
            <label className="label">Услуга</label>
            <select className="input" value={serviceId} onChange={e => { setServiceId(e.target.value); setSubscriptionId(''); setTime('') }} required>
              <option value="">— Выберите услугу —</option>
              {services.map(s => (
                <option key={s.id} value={s.id}>{s.name} — {s.price} ₸ ({s.durationMinutes} мин)</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label className="label">Мастер</label>
            <select className="input" value={employeeId} onChange={e => setEmployeeId(e.target.value)} required>
              <option value="">— Выберите мастера —</option>
              {employees.map(em => <option key={em.id} value={em.id}>{em.name} — {em.specialization}</option>)}
            </select>
          </div>
        </div>

        <div className="field">
          <label className="label">Дата</label>
          <div className="day-strip">
            {days.map(d => {
              const value = d.format('YYYY-MM-DD')
              return (
                <button type="button" key={value}
                  className={`day-chip${value === date ? ' is-active' : ''}`}
                  onClick={() => setDate(value)}>
                  <small>{d.format('dd')}</small>
                  <strong>{d.format('D')}</strong>
                  <small>{d.format('MMM')}</small>
                </button>
              )
            })}
          </div>
        </div>

        <div className="field">
          <div className="flex items-center justify-between" style={{ marginBottom: 8 }}>
            <label className="label" style={{ margin: 0 }}>Время</label>
            <div className="slot-legend">
              <span><i className="slot-dot is-free" /> Свободно</span>
              <span><i className="slot-dot is-busy" /> Занято</span>
              <span><i className="slot-dot is-off" /> Недоступно</span>
            </div>
          </div>

          {!employeeId ? (
            <div className="slot-empty">Выберите мастера, чтобы увидеть свободное время</div>
          ) : slotsLoading ? (
            <div className="spinner" />
          ) : slots.length === 0 ? (
            <div className="slot-empty">В этот день мастер не работает — выберите другую дату</div>
          ) : (
            <div className="slot-grid">
              {slots.map((s, i) => {
                const canPick = selectable[i]
                const inSelection = selectedIdx >= 0 && i >= selectedIdx && i < selectedIdx + hoursNeeded
                const cls = s.status === 'BUSY' ? 'is-busy'
                  : s.status !== 'FREE' ? 'is-off'
                  : inSelection ? 'is-selected'
                  : canPick ? 'is-free' : 'is-short'
                const hint = s.status === 'FREE' && !canPick
                  ? `Услуга длится ${service?.durationMinutes} мин — следующий час занят`
                  : s.status === 'BUSY' && s.clientName ? `${s.clientName} · ${s.serviceName}` : undefined
                return (
                  <button type="button" key={s.time} className={`slot ${cls}`} disabled={!canPick}
                    title={hint} onClick={() => setTime(s.time)}>
                    <strong>{s.time}–{dayjs(`${date}T${s.time}`).add(1, 'hour').format('HH:mm')}</strong>
                    <span>
                      {s.status === 'BUSY' && s.clientName ? s.clientName
                        : SLOT_LABEL[s.status] ?? (inSelection ? 'Выбрано' : canPick ? 'Свободно' : 'Мало времени')}
                    </span>
                  </button>
                )
              })}
            </div>
          )}
        </div>

        {matchingSubs.length > 0 && (
          <div className="field">
            <label className="label">Оплата</label>
            <select className="input" value={subscriptionId} onChange={e => setSubscriptionId(e.target.value)}>
              <option value="">Разовое посещение — {service?.price} ₸</option>
              {matchingSubs.map(s => (
                <option key={s.id} value={s.id}>
                  {s.type === 'ANNUAL' ? 'Годовой абонемент' : 'Абонемент'} — осталось {s.remainingSessions} из {s.totalSessions}
                </option>
              ))}
            </select>
            <p className="text-muted text-sm mt-2">Сеанс спишется после подтверждения записи. При отмене записи сеанс не списывается.</p>
          </div>
        )}

        <div className="field">
          <label className="label">Комментарий (необязательно)</label>
          <textarea className="input" rows={2} value={comment} onChange={e => setComment(e.target.value)}
            placeholder="Пожелания, особенности..." style={{ resize: 'vertical', height: 'auto', paddingTop: 8 }} />
        </div>

        {time && service && (
          <div className="booking-summary">
            {dayjs(`${date}T${time}`).format('D MMMM, dd')} · {time} · {service.name} · {service.durationMinutes} мин
          </div>
        )}

        <div className="flex gap-3">
          <button type="button" className="btn btn-ghost" onClick={() => navigate(-1)}>Назад</button>
          <button type="submit" className="btn btn-primary" disabled={saving || !time} style={{ flex: 1 }}>
            {saving ? 'Сохранение...' : 'Записаться'}
          </button>
        </div>
      </form>
    </div>
  )
}
