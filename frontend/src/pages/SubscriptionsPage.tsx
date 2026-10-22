import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { CalendarRange, Check, Repeat, Ticket, X } from 'lucide-react'
import dayjs from 'dayjs'
import {
  acceptSubscription,
  approveSubscription,
  cancelSubscription,
  createSubscription,
  declineSubscription,
  getAllSubscriptions,
  getMySubscriptions,
  rejectSubscription,
  transferSubscription,
  useSubscriptionSession,
} from '../api/subscriptions'
import { getServices } from '../api/services'
import { getClients, getSalonSettings } from '../api/users'
import { getAuth, isAdmin } from '../store/auth'
import type { ClientShort, SalonService, SalonSettings, Subscription, SubscriptionType } from '../types'
import { formatDate, subStatusBadge, subStatusLabel } from '../utils/format'

const DEFAULT_ANNUAL_TEXT =
  'Год заботы о себе по фиксированной цене. Записывайтесь в любое удобное свободное окошко — ' +
  'сеансы списываются только после подтверждения записи, а отменённые записи абонемент не расходуют.'

const money = (v?: number | null) => v != null ? `${Number(v).toLocaleString('ru-RU')} ₸` : '—'
const errText = (err: any) => err.response?.data?.error || 'Ошибка'

export default function SubscriptionsPage() {
  const { user } = getAuth()
  const admin = isAdmin(user?.role)
  const owner = user?.role === 'OWNER'
  const client = user?.role === 'CLIENT'

  const [list, setList] = useState<Subscription[]>([])
  const [services, setServices] = useState<SalonService[]>([])
  const [clients, setClients] = useState<ClientShort[]>([])
  const [settings, setSettings] = useState<SalonSettings | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const [requestServiceId, setRequestServiceId] = useState('')
  const [showRequest, setShowRequest] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({
    clientId: '', serviceId: '', type: 'ANNUAL' as SubscriptionType, totalSessions: '', price: '',
    startDate: dayjs().format('YYYY-MM-DD'), expiryDate: '', notes: '',
  })
  const [transferFor, setTransferFor] = useState<Subscription | null>(null)
  const [transferTo, setTransferTo] = useState('')

  useEffect(() => {
    Promise.all([
      admin ? getAllSubscriptions() : getMySubscriptions(),
      getServices(),
      getSalonSettings().catch(() => null),
      admin ? getClients() : Promise.resolve([]),
    ])
      .then(([subs, svcs, st, cl]) => {
        setList(subs); setServices(svcs); setSettings(st); setClients(cl)
        setForm(f => ({
          ...f,
          totalSessions: st?.annualSessions ? String(st.annualSessions) : '',
          price: st?.annualPrice ? String(st.annualPrice) : '',
        }))
      })
      .finally(() => setLoading(false))
  }, [])

  const replace = (updated: Subscription) => setList(l => l.map(s => s.id === updated.id ? updated : s))

  async function run(action: () => Promise<Subscription>, success: string) {
    try {
      replace(await action())
      toast.success(success)
    } catch (err: any) {
      toast.error(errText(err))
    }
  }

  async function handleRequest(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const sub = await createSubscription({ serviceId: Number(requestServiceId), type: 'ANNUAL' })
      setList(l => [sub, ...l])
      setShowRequest(false)
      toast.success('Заявка отправлена — администратор подтвердит её в ближайшее время')
    } catch (err: any) {
      toast.error(errText(err))
    } finally {
      setSaving(false)
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const sub = await createSubscription({
        clientId: Number(form.clientId),
        serviceId: Number(form.serviceId),
        type: form.type,
        totalSessions: form.totalSessions ? Number(form.totalSessions) : undefined,
        price: form.price ? Number(form.price) : undefined,
        startDate: form.startDate,
        expiryDate: form.type === 'COURSE' ? form.expiryDate : undefined,
        notes: form.notes || undefined,
      })
      setList(l => [sub, ...l])
      setShowForm(false)
      toast.success('Абонемент выдан — клиенту нужно принять его в своём кабинете')
    } catch (err: any) {
      toast.error(errText(err))
    } finally {
      setSaving(false)
    }
  }

  async function handleTransfer(e: React.FormEvent) {
    e.preventDefault()
    if (!transferFor) return
    try {
      replace(await transferSubscription(transferFor.id, Number(transferTo)))
      toast.success('Абонемент передан')
      setTransferFor(null)
      setTransferTo('')
    } catch (err: any) {
      toast.error(errText(err))
    }
  }

  if (loading) return <div className="spinner" />

  const annualReady = !!settings?.annualSessions
  const perSession = settings?.annualPrice && settings?.annualSessions
    ? Math.round(settings.annualPrice / settings.annualSessions) : null
  const offers = list.filter(s => s.status === 'OFFERED')
  const pending = list.filter(s => s.status === 'PENDING')
  const rows = list.filter(s => !(client && s.status === 'OFFERED') && !(admin && s.status === 'PENDING'))

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Абонементы</h1>
          <p className="page-subtitle">Годовой абонемент и курсы массажа</p>
        </div>
        {admin && (
          <button className="btn btn-primary" onClick={() => setShowForm(v => !v)}>
            {showForm ? 'Отмена' : '+ Выдать абонемент'}
          </button>
        )}
      </div>

      {/* Условия годового абонемента — видны всем */}
      <div className="card annual-card">
        <div>
          <h2><Ticket size={20} style={{ verticalAlign: -3 }} /> Годовой абонемент</h2>
          <p>{settings?.annualDescription || DEFAULT_ANNUAL_TEXT}</p>
          <ul style={{ margin: '12px 0 0 18px', opacity: .9, lineHeight: 1.7 }}>
            <li>Оформление по заявке — администратор подтверждает её</li>
            <li>Отмена записи не расходует сеанс абонемента</li>
            <li>Неиспользованный абонемент можно передать другому человеку через управляющего</li>
          </ul>
          {client && annualReady && !showRequest && (
            <button className="btn btn-light" style={{ marginTop: 16 }} onClick={() => setShowRequest(true)}>
              Оформить годовой абонемент
            </button>
          )}
          {client && showRequest && (
            <form onSubmit={handleRequest} className="flex gap-2" style={{ marginTop: 16, flexWrap: 'wrap' }}>
              <select className="input" style={{ flex: '1 1 220px', width: 'auto' }} value={requestServiceId}
                onChange={e => setRequestServiceId(e.target.value)} required>
                <option value="">— Какой массаж? —</option>
                {services.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
              <button className="btn btn-light" type="submit" disabled={saving}>Отправить заявку</button>
              <button className="btn btn-ghost" type="button" onClick={() => setShowRequest(false)}>Отмена</button>
            </form>
          )}
        </div>
        <div className="annual-facts">
          <div className="annual-fact"><small>Стоимость</small><strong>{money(settings?.annualPrice)}</strong></div>
          <div className="annual-fact"><small>Сеансов</small><strong>{settings?.annualSessions ?? '—'}</strong></div>
          <div className="annual-fact"><small>Срок действия</small><strong>12 мес.</strong></div>
          <div className="annual-fact"><small>Цена сеанса</small><strong>{money(perSession)}</strong></div>
          {!annualReady && (
            <div style={{ gridColumn: '1 / -1', fontSize: 13, opacity: .85 }}>
              {admin ? 'Задайте цену и количество сеансов в «Настройках».' : 'Условия скоро появятся — уточните у администратора.'}
            </div>
          )}
        </div>
      </div>

      {/* Клиент: абонементы, выданные администратором, ждут согласия */}
      {client && offers.map(s => (
        <div key={s.id} className="card offer-card">
          <div className="flex items-center justify-between gap-3" style={{ flexWrap: 'wrap' }}>
            <div>
              <strong>Вам выдан {s.type === 'ANNUAL' ? 'годовой абонемент' : 'абонемент'}: {s.serviceName}</strong>
              <div className="text-muted text-sm">
                {s.totalSessions} сеансов · {formatDate(s.startDate)} — {formatDate(s.expiryDate)} · {money(s.price)}
              </div>
            </div>
            <div className="flex gap-2">
              <button className="btn btn-primary btn-sm" onClick={() => run(() => acceptSubscription(s.id), 'Абонемент активирован')}>
                <Check size={14} /> Принять
              </button>
              <button className="btn btn-ghost btn-sm" onClick={() => run(() => declineSubscription(s.id), 'Вы отказались от абонемента')}>
                Отказаться
              </button>
            </div>
          </div>
        </div>
      ))}

      {showForm && admin && (
        <div className="card mb-4">
          <h2 style={{ fontWeight: 600, marginBottom: 4 }}>Выдать абонемент</h2>
          <p className="text-muted text-sm mb-4">Клиент увидит абонемент в кабинете и должен принять его — после этого он станет активным.</p>
          <form onSubmit={handleCreate}>
            <div className="field-row">
              <div className="field">
                <label className="label">Клиент</label>
                <select className="input" value={form.clientId} onChange={e => setForm(f => ({ ...f, clientId: e.target.value }))} required>
                  <option value="">— Выберите клиента —</option>
                  {clients.map(c => <option key={c.id} value={c.id}>{c.name}{c.phone ? ` · ${c.phone}` : ''}</option>)}
                </select>
              </div>
              <div className="field">
                <label className="label">Услуга</label>
                <select className="input" value={form.serviceId} onChange={e => setForm(f => ({ ...f, serviceId: e.target.value }))} required>
                  <option value="">— Выберите услугу —</option>
                  {services.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div className="field">
                <label className="label">Тип</label>
                <select className="input" value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value as SubscriptionType }))}>
                  <option value="ANNUAL">Годовой (12 месяцев)</option>
                  <option value="COURSE">Курс на N сеансов</option>
                </select>
              </div>
              <div className="field">
                <label className="label">Количество сеансов</label>
                <input className="input" type="number" min="1" value={form.totalSessions}
                  onChange={e => setForm(f => ({ ...f, totalSessions: e.target.value }))} required={form.type === 'COURSE'} />
              </div>
              <div className="field">
                <label className="label">Стоимость, ₸</label>
                <input className="input" type="number" min="0" value={form.price}
                  onChange={e => setForm(f => ({ ...f, price: e.target.value }))} />
              </div>
              <div className="field">
                <label className="label">Дата начала</label>
                <input className="input" type="date" value={form.startDate} onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))} required />
              </div>
              {form.type === 'COURSE' && (
                <div className="field">
                  <label className="label">Дата окончания</label>
                  <input className="input" type="date" value={form.expiryDate} onChange={e => setForm(f => ({ ...f, expiryDate: e.target.value }))} required />
                </div>
              )}
              <div className="field">
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

      {/* Админ/управляющий: заявки клиентов */}
      {admin && pending.length > 0 && (
        <div className="card mb-4">
          <h2 style={{ fontWeight: 600, marginBottom: 12 }}>Заявки на абонемент · {pending.length}</h2>
          <div className="table-wrap">
            <table>
              <tbody>
                {pending.map(s => (
                  <tr key={s.id}>
                    <td>{s.clientName}<br /><span className="text-muted text-sm">{s.clientPhone}</span></td>
                    <td>{s.type === 'ANNUAL' ? 'Годовой' : 'Курс'} · {s.serviceName}<br />
                      <span className="text-muted text-sm">{s.totalSessions} сеансов · {money(s.price)}</span></td>
                    <td>
                      <div className="flex gap-2">
                        <button className="btn btn-sm btn-primary" onClick={() => run(() => approveSubscription(s.id), 'Абонемент подтверждён')}>
                          <Check size={14} /> Подтвердить
                        </button>
                        <button className="btn btn-sm btn-ghost" onClick={() => {
                          const reason = prompt('Причина отказа (необязательно)') ?? undefined
                          run(() => rejectSubscription(s.id, reason || undefined), 'Заявка отклонена')
                        }}>
                          <X size={14} /> Отклонить
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="card">
        {rows.length === 0 ? (
          <div className="empty-state">Абонементов пока нет</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  {admin && <th>Клиент</th>}
                  <th>Абонемент</th>
                  <th>Сеансы</th>
                  <th>Действует</th>
                  <th>Стоимость</th>
                  <th>Статус</th>
                  {admin && <th>Действия</th>}
                </tr>
              </thead>
              <tbody>
                {rows.map(s => (
                  <tr key={s.id}>
                    {admin && <td>{s.clientName}<br /><span className="text-muted text-sm">{s.clientPhone}</span></td>}
                    <td>
                      {s.type === 'ANNUAL' ? 'Годовой' : 'Курс'} · {s.serviceName}
                      {s.notes && <><br /><span className="text-muted text-sm">{s.notes}</span></>}
                    </td>
                    <td>
                      <span style={{ fontWeight: 700, color: 'var(--brand)' }}>{s.remainingSessions}</span>
                      <span style={{ color: 'var(--text-muted)' }}>/{s.totalSessions}</span>
                    </td>
                    <td><CalendarRange size={13} style={{ verticalAlign: -2 }} /> {formatDate(s.startDate)} — {formatDate(s.expiryDate)}</td>
                    <td>{money(s.price)}</td>
                    <td><span className={`badge ${subStatusBadge(s.status)}`}>{subStatusLabel(s.status)}</span></td>
                    {admin && (
                      <td>
                        <div className="flex gap-2">
                          {s.status === 'ACTIVE' && s.remainingSessions > 0 && (
                            <button className="btn btn-sm btn-secondary" title="Списать сеанс вручную" onClick={() => run(() => useSubscriptionSession(s.id), 'Сеанс списан')}>
                              − Сеанс
                            </button>
                          )}
                          {owner && s.status === 'ACTIVE' && s.remainingSessions > 0 && (
                            <button className="btn btn-sm btn-ghost" onClick={() => setTransferFor(s)}>
                              <Repeat size={14} /> Передать
                            </button>
                          )}
                          {s.status === 'ACTIVE' && (
                            <button className="btn btn-sm btn-danger" onClick={() => {
                              if (confirm('Отменить абонемент?')) run(() => cancelSubscription(s.id), 'Абонемент отменён')
                            }}>
                              Отменить
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {transferFor && (
        <div className="modal-backdrop" onClick={() => setTransferFor(null)}>
          <form className="card modal" onClick={e => e.stopPropagation()} onSubmit={handleTransfer}>
            <h2 style={{ fontWeight: 600, marginBottom: 4 }}>Передать абонемент</h2>
            <p className="text-muted text-sm mb-4">
              {transferFor.clientName} → новый владелец. Останется {transferFor.remainingSessions} сеансов до {formatDate(transferFor.expiryDate)}.
            </p>
            <div className="field">
              <label className="label">Кому передать</label>
              <select className="input" value={transferTo} onChange={e => setTransferTo(e.target.value)} required>
                <option value="">— Выберите клиента —</option>
                {clients.filter(c => c.id !== transferFor.clientId).map(c => (
                  <option key={c.id} value={c.id}>{c.name}{c.phone ? ` · ${c.phone}` : ''}</option>
                ))}
              </select>
            </div>
            <div className="flex gap-2">
              <button type="button" className="btn btn-ghost" onClick={() => setTransferFor(null)}>Отмена</button>
              <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Передать</button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
