import { useEffect, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import api from '../api/client'
import { getAuth, isAdmin, isStaff } from '../store/auth'
import type { Appointment, AppointmentStatus } from '../types'

interface Employee { id: number; userId: number; name: string; specialization?: string }
interface DaySchedule {
  dayOfWeek: string
  working: boolean
  workStart: string
  workEnd: string
}

const DAY_NAMES: Record<string, string> = {
  MONDAY: 'Пн', TUESDAY: 'Вт', WEDNESDAY: 'Ср',
  THURSDAY: 'Чт', FRIDAY: 'Пт', SATURDAY: 'Сб', SUNDAY: 'Вс'
}
const JS_DAYS = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
const MONTHS = ['янв.', 'февр.', 'мар.', 'апр.', 'мая', 'июн.', 'июл.', 'авг.', 'сент.', 'окт.', 'нояб.', 'дек.']

// Часы салона по умолчанию — как DEFAULT_START/END в ScheduleService
const DAY_START = 11
const DAY_END = 23
const HOUR_PX = 64

const STATUS_STYLE: Record<AppointmentStatus, { label: string; color: string }> = {
  // --warning почти совпадает с --brand в тёмной теме, поэтому «ожидает» — отдельным цветом
  PENDING: { label: 'Ожидает', color: 'var(--danger)' },
  CONFIRMED: { label: 'Подтверждена', color: 'var(--brand)' },
  COMPLETED: { label: 'Завершена', color: 'var(--success)' },
  CANCELLED: { label: 'Отменена', color: 'var(--text-faint)' },
}

const pad = (n: number) => String(n).padStart(2, '0')
const isoDate = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const addDays = (d: Date, n: number) => { const r = new Date(d); r.setDate(r.getDate() + n); return r }
const minutesOf = (hhmm: string) => { const [h, m] = hhmm.split(':').map(Number); return h * 60 + m }
const timeOf = (iso: string) => iso.slice(11, 16)

export default function SchedulePage() {
  const { user } = getAuth()
  const admin = isAdmin(user?.role)
  const staff = isStaff(user?.role)

  const [employees, setEmployees] = useState<Employee[]>([])
  const [schedules, setSchedules] = useState<Record<number, DaySchedule[]>>({})
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [date, setDate] = useState(() => new Date())
  const [loading, setLoading] = useState(true)
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 60_000)
    return () => clearInterval(t)
  }, [])

  useEffect(() => {
    api.get('/employees').then(async r => {
      let emps: Employee[] = r.data.map((e: any) => ({
        id: e.id,
        userId: e.userId,
        name: e.name ?? `Сотрудник ${e.id}`,
        specialization: e.specialization,
      }))
      // Мастер видит только свою колонку
      if (staff && !admin) emps = emps.filter(e => e.userId === user?.id)
      setEmployees(emps)

      const results = await Promise.all(
        emps.map(e => api.get(`/schedule/employee/${e.id}`).then(r => ({ id: e.id, data: r.data })).catch(() => ({ id: e.id, data: [] })))
      )
      const map: Record<number, DaySchedule[]> = {}
      results.forEach(({ id, data }) => { map[id] = data })
      setSchedules(map)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  const day = isoDate(date)

  useEffect(() => {
    if (!staff) return
    const params = { from: `${day}T00:00:00`, to: `${day}T23:59:59` }
    const load = admin
      ? api.get<Appointment[]>('/appointments/graph/all', { params })
      : employees.length
        ? api.get<Appointment[]>(`/appointments/graph/employee/${employees[0].id}`, { params })
        : null
    load?.then(r => setAppointments(r.data.filter(a => a.status !== 'CANCELLED'))).catch(() => setAppointments([]))
  }, [day, employees])

  const week = useMemo(() => {
    const monday = addDays(date, -((date.getDay() + 6) % 7))
    return Array.from({ length: 7 }, (_, i) => addDays(monday, i))
  }, [day])

  // Рабочие часы мастера в выбранный день; null — выходной
  function workHours(empId: number): [number, number] | null {
    const sch = (schedules[empId] ?? []).find(s => s.dayOfWeek === JS_DAYS[date.getDay()])
    if (!sch) return [DAY_START * 60, DAY_END * 60]
    if (!sch.working) return null
    return [minutesOf(sch.workStart), minutesOf(sch.workEnd)]
  }

  // Сетка покрывает и часы салона, и нестандартные графики мастеров
  const [gridStart, gridEnd] = useMemo(() => {
    let s = DAY_START * 60, e = DAY_END * 60
    employees.forEach(emp => {
      const h = workHours(emp.id)
      if (h) { s = Math.min(s, h[0]); e = Math.max(e, h[1]) }
    })
    return [Math.floor(s / 60), Math.ceil(e / 60)]
  }, [employees, schedules, day])

  const hours = Array.from({ length: gridEnd - gridStart }, (_, i) => gridStart + i)
  const top = (min: number) => ((min - gridStart * 60) / 60) * HOUR_PX
  const isToday = day === isoDate(now)
  const nowMin = now.getHours() * 60 + now.getMinutes()

  if (loading) return <div className="text-muted">Загрузка...</div>

  if (!staff) return <WeeklySummary employees={employees} schedules={schedules} />

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', marginBottom: 16 }}>
        <h1 className="page-title" style={{ margin: 0 }}>Расписание</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button className="btn btn-sm btn-secondary" onClick={() => setDate(addDays(date, -7))} aria-label="Предыдущая неделя"><ChevronLeft size={16} /></button>
          <button className="btn btn-sm btn-secondary" onClick={() => setDate(new Date())}>Сегодня</button>
          <button className="btn btn-sm btn-secondary" onClick={() => setDate(addDays(date, 7))} aria-label="Следующая неделя"><ChevronRight size={16} /></button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, minmax(0, 1fr))', gap: 6, marginBottom: 16 }}>
        {week.map(d => {
          const active = isoDate(d) === day
          const today = isoDate(d) === isoDate(now)
          return (
            <button key={isoDate(d)} onClick={() => setDate(d)} style={{
              padding: '8px 4px', borderRadius: 12, cursor: 'pointer', textAlign: 'center',
              background: active ? 'var(--brand)' : 'var(--card)',
              color: active ? 'var(--card)' : 'var(--text)',
              border: `1px solid ${active ? 'var(--brand)' : today ? 'var(--brand-soft)' : 'var(--border)'}`,
            }}>
              <div style={{ fontSize: 12, opacity: .8 }}>{DAY_NAMES[JS_DAYS[d.getDay()]]}</div>
              <div style={{ fontSize: 18, fontWeight: 700, lineHeight: 1.3 }}>{d.getDate()}</div>
              <div style={{ fontSize: 11, opacity: .8 }}>{MONTHS[d.getMonth()]}</div>
            </button>
          )
        })}
      </div>

      {employees.length === 0 ? (
        <div className="card text-muted">Сотрудники не найдены.</div>
      ) : (
        <div className="card" style={{ padding: 0, overflowX: 'auto' }}>
          <div style={{ display: 'grid', gridTemplateColumns: `56px repeat(${employees.length}, minmax(150px, 1fr))`, minWidth: 56 + employees.length * 150 }}>
            {/* Шапка с мастерами */}
            <div style={{ borderBottom: '1px solid var(--border)', position: 'sticky', left: 0, background: 'var(--card)', zIndex: 3 }} />
            {employees.map(emp => {
              const h = workHours(emp.id)
              const count = appointments.filter(a => a.employeeId === emp.id).length
              return (
                <div key={emp.id} style={{ padding: '12px 10px', borderBottom: '1px solid var(--border)', borderLeft: '1px solid var(--border)' }}>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>{emp.name}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                    {h ? `${pad(Math.floor(h[0] / 60))}:${pad(h[0] % 60)}–${pad(Math.floor(h[1] / 60))}:${pad(h[1] % 60)}` : 'Выходной'}
                    {count > 0 && ` · ${count} зап.`}
                  </div>
                </div>
              )
            })}

            {/* Шкала времени */}
            <div style={{ position: 'sticky', left: 0, background: 'var(--card)', zIndex: 2 }}>
              {hours.map(h => (
                <div key={h} style={{ height: HOUR_PX, fontSize: 12, color: 'var(--text-faint)', textAlign: 'right', paddingRight: 8, transform: h > gridStart ? 'translateY(-7px)' : undefined, paddingTop: h > gridStart ? 0 : 2 }}>
                  {pad(h)}:00
                </div>
              ))}
            </div>

            {/* Колонки мастеров */}
            {employees.map(emp => {
              const h = workHours(emp.id)
              return (
                <div key={emp.id} style={{ position: 'relative', height: hours.length * HOUR_PX, borderLeft: '1px solid var(--border)' }}>
                  {hours.map(hr => (
                    <div key={hr} style={{ position: 'absolute', left: 0, right: 0, top: top(hr * 60), height: HOUR_PX, borderTop: '1px solid var(--border)' }} />
                  ))}

                  {/* Нерабочее время — штриховка */}
                  {(h ? [[gridStart * 60, h[0]], [h[1], gridEnd * 60]] : [[gridStart * 60, gridEnd * 60]])
                    .filter(([s, e]) => e > s)
                    .map(([s, e]) => (
                      <div key={s} style={{
                        position: 'absolute', left: 0, right: 0, top: top(s), height: top(e) - top(s),
                        background: 'repeating-linear-gradient(135deg, transparent 0 6px, var(--border) 6px 7px)', opacity: .6,
                      }} />
                    ))}

                  {appointments.filter(a => a.employeeId === emp.id).map(a => {
                    const s = minutesOf(timeOf(a.startTime))
                    const e = minutesOf(timeOf(a.endTime))
                    const st = STATUS_STYLE[a.status]
                    return (
                      <div key={a.id} title={`${timeOf(a.startTime)}–${timeOf(a.endTime)} · ${a.serviceName}\n${a.clientName} ${a.clientPhone ?? ''}\n${st.label}${a.comment ? `\n${a.comment}` : ''}`} style={{
                        position: 'absolute', left: 4, right: 4, top: top(s) + 2, height: Math.max(top(e) - top(s) - 4, 22),
                        background: 'var(--brand-light)', borderLeft: `4px solid ${st.color}`, borderRadius: 8,
                        padding: '4px 8px', overflow: 'hidden', fontSize: 12, lineHeight: 1.35, zIndex: 1,
                      }}>
                        <div style={{ fontWeight: 600 }}>{timeOf(a.startTime)}–{timeOf(a.endTime)} · {a.clientName}</div>
                        <div style={{ color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{a.serviceName}</div>
                        <div style={{ color: st.color, fontSize: 11 }}>{st.label}</div>
                      </div>
                    )
                  })}

                  {isToday && nowMin >= gridStart * 60 && nowMin <= gridEnd * 60 && (
                    <div style={{ position: 'absolute', left: 0, right: 0, top: top(nowMin), borderTop: '2px solid var(--danger)', zIndex: 2 }} />
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginTop: 12, fontSize: 12, color: 'var(--text-muted)' }}>
        {(['PENDING', 'CONFIRMED', 'COMPLETED'] as AppointmentStatus[]).map(s => (
          <span key={s} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 10, height: 10, borderRadius: 3, background: STATUS_STYLE[s].color }} />{STATUS_STYLE[s].label}
          </span>
        ))}
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: 3, background: 'repeating-linear-gradient(135deg, transparent 0 3px, var(--text-faint) 3px 4px)' }} />Нерабочее время
        </span>
      </div>
    </div>
  )
}

/** Клиентам — только недельный график мастеров, без чужих записей */
function WeeklySummary({ employees, schedules }: { employees: Employee[]; schedules: Record<number, DaySchedule[]> }) {
  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 24 }}>Расписание сотрудников</h1>
      {employees.length === 0 ? (
        <div className="text-muted">Сотрудники не найдены.</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {employees.map(emp => {
            const days = schedules[emp.id] ?? []
            return (
              <div key={emp.id} className="card">
                <h3 style={{ marginBottom: 12 }}>{emp.name}</h3>
                {days.length === 0 ? (
                  <div className="text-muted" style={{ fontSize: 13 }}>Ежедневно {DAY_START}:00–{DAY_END}:00</div>
                ) : (
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {days.map((d, i) => (
                      <div key={i} style={{
                        padding: '8px 14px', borderRadius: 10, textAlign: 'center', minWidth: 60,
                        background: d.working ? 'var(--brand-light)' : 'var(--border)',
                        border: `1px solid ${d.working ? 'var(--brand)' : 'var(--border)'}`,
                      }}>
                        <div style={{ fontWeight: 700, color: d.working ? 'var(--brand)' : 'var(--text-muted)', fontSize: 13 }}>
                          {DAY_NAMES[d.dayOfWeek] ?? d.dayOfWeek}
                        </div>
                        <div style={{ fontSize: 11, color: d.working ? 'var(--text)' : 'var(--text-muted)', marginTop: 4 }}>
                          {d.working ? `${d.workStart}–${d.workEnd}` : 'Выходной'}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
