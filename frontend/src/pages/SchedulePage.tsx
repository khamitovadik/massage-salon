import { useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import { ChevronLeft, ChevronRight, Pencil } from 'lucide-react'
import api from '../api/client'
import { getAuth, isAdmin, isStaff } from '../store/auth'
import { getCurrentBranchId } from '../store/branch'
import { usePublicBranches } from '../store/useBranches'
import type { Appointment, AppointmentStatus } from '../types'

interface Employee { id: number; userId: number; name: string; specialization?: string; branchIds: number[] }
interface DaySchedule {
  dayOfWeek: string
  working: boolean
  workStart: string
  workEnd: string
  branchId?: number | null
}
/** Часы на конкретную дату — важнее недельного расписания */
interface WorkDay {
  employeeId: number
  workDate: string
  working: boolean
  branchId?: number | null
  workStart: string | null
  workEnd: string | null
}

const DAY_NAMES: Record<string, string> = {
  MONDAY: 'Пн', TUESDAY: 'Вт', WEDNESDAY: 'Ср',
  THURSDAY: 'Чт', FRIDAY: 'Пт', SATURDAY: 'Сб', SUNDAY: 'Вс'
}
const JS_DAYS = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
const MONTHS = ['янв.', 'февр.', 'мар.', 'апр.', 'мая', 'июн.', 'июл.', 'авг.', 'сент.', 'окт.', 'нояб.', 'дек.']

// Видимая часть сетки и время, подставляемое в редактор по умолчанию (часы салона)
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
const hhmm = (min: number) => `${pad(Math.floor(min / 60))}:${pad(min % 60)}`
// Варианты времени в редакторе: каждые 30 минут с 08:00 до 23:30 (LocalTime на бэкенде не принимает 24:00)
const TIME_OPTIONS = Array.from({ length: 32 }, (_, i) => hhmm(8 * 60 + i * 30))

interface Editor { empId: number; working: boolean; start: string; end: string; dates: string[]; weeks: number; branchId: number | null }
const REPEAT_WEEKS = [1, 2, 4, 8, 12]

export default function SchedulePage() {
  const { user } = getAuth()
  const admin = isAdmin(user?.role)
  const staff = isStaff(user?.role)
  const { branches } = usePublicBranches()
  const branchName = (id?: number | null) => branches.find(b => b.id === id)?.name
  // Филиал, выбранный в шапке; у мастера фильтра нет — он видит все свои дни
  const viewBranch = admin ? getCurrentBranchId() : null

  const [employees, setEmployees] = useState<Employee[]>([])
  const [schedules, setSchedules] = useState<Record<number, DaySchedule[]>>({})
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [workDays, setWorkDays] = useState<WorkDay[]>([])
  const [editor, setEditor] = useState<Editor | null>(null)
  const [savingDays, setSavingDays] = useState(false)
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
        branchIds: e.branchIds ?? [],
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
  const weekFrom = isoDate(week[0])

  function loadWorkDays() {
    if (!staff) return
    api.get<WorkDay[]>('/schedule/days', { params: { from: weekFrom, to: isoDate(week[6]) } })
      .then(r => setWorkDays(r.data)).catch(() => setWorkDays([]))
  }
  useEffect(loadWorkDays, [weekFrom])

  const override = (empId: number, d: string) => workDays.find(w => w.employeeId === empId && w.workDate === d)
  // Подпись для дня без часов: «Выходной», если его так отметили, иначе «Не задано»
  const offLabel = (empId: number, d: Date = date) =>
    elsewhere(empId, d) ? `В филиале «${branchName(dayBranch(empId, d)) ?? 'другом'}»`
    : override(empId, isoDate(d)) || (schedules[empId] ?? []).some(s => s.dayOfWeek === JS_DAYS[d.getDay()]) ? 'Выходной' : 'Часы не заданы'

  // Филиал, в котором мастер работает в этот день (null — в любом своём)
  function dayBranch(empId: number, d: Date = date): number | null {
    const o = override(empId, isoDate(d))
    if (o) return o.branchId ?? null
    return (schedules[empId] ?? []).find(s => s.dayOfWeek === JS_DAYS[d.getDay()])?.branchId ?? null
  }

  // В шапке выбран филиал, а мастер в этот день работает в другом
  const elsewhere = (empId: number, d: Date = date) => {
    const b = dayBranch(empId, d)
    return viewBranch != null && b != null && b !== viewBranch && workHoursAny(empId, d) != null
  }

  // Рабочие часы мастера на дату; null — не работает (или работает в другом филиале).
  // Порядок как на бэкенде: график на дату → недельное расписание → иначе не работает
  function workHours(empId: number, d: Date = date): [number, number] | null {
    return elsewhere(empId, d) ? null : workHoursAny(empId, d)
  }

  function workHoursAny(empId: number, d: Date = date): [number, number] | null {
    const o = override(empId, isoDate(d))
    if (o) return o.working && o.workStart && o.workEnd ? [minutesOf(o.workStart), minutesOf(o.workEnd)] : null
    const sch = (schedules[empId] ?? []).find(s => s.dayOfWeek === JS_DAYS[d.getDay()])
    if (!sch) return null
    if (!sch.working) return null
    return [minutesOf(sch.workStart), minutesOf(sch.workEnd)]
  }

  function openEditor(empId: number) {
    const h = workHours(empId)
    const emp = employees.find(e => e.id === empId)
    const empBranches = emp?.branchIds ?? []
    // Филиал дня: уже назначенный → выбранный в шапке → единственный филиал мастера
    const current = dayBranch(empId)
    const initialBranch = current && !elsewhere(empId) ? current
      : viewBranch && empBranches.includes(viewBranch) ? viewBranch
      : empBranches.length === 1 ? empBranches[0] : null
    setEditor({
      branchId: initialBranch,
      // Часы ещё не заданы — открываем сразу с «Работает», руководитель обычно за этим и пришёл
      empId, working: !!h || offLabel(empId) !== 'Выходной',
      start: hhmm(h ? h[0] : DAY_START * 60), end: hhmm(h ? h[1] : DAY_END * 60),
      dates: [day], weeks: 1,
    })
  }

  // Выбранные дни, повторённые на N недель вперёд: «Пн и Ср на 4 недели» = 8 дат
  const editorDates = (e: Editor) => [...new Set(
    e.dates.flatMap(d => Array.from({ length: e.weeks }, (_, k) => isoDate(addDays(new Date(`${d}T12:00:00`), k * 7))))
  )]

  async function saveEditor(reset = false) {
    if (!editor || editor.dates.length === 0) return
    if (!reset && editor.working && editor.start >= editor.end) {
      toast.error('Начало должно быть раньше конца')
      return
    }
    setSavingDays(true)
    try {
      await Promise.all(editorDates(editor).map(d => {
        const url = `/schedule/employee/${editor.empId}/day/${d}`
        return reset
          ? api.delete(url)
          : api.put(url, editor.working
            ? { working: true, workStart: editor.start, workEnd: editor.end, branchId: editor.branchId ?? undefined }
            : { working: false })
      }))
      toast.success(reset ? 'Вернули обычный график' : 'График сохранён')
      setEditor(null)
      loadWorkDays()
    } catch (err: any) {
      toast.error(err?.response?.data?.error || 'Не удалось сохранить график')
    } finally {
      setSavingDays(false)
    }
  }

  // Сетка покрывает и часы салона, и нестандартные графики мастеров
  const [gridStart, gridEnd] = useMemo(() => {
    let s = DAY_START * 60, e = DAY_END * 60
    employees.forEach(emp => {
      const h = workHours(emp.id)
      if (h) { s = Math.min(s, h[0]); e = Math.max(e, h[1]) }
    })
    return [Math.floor(s / 60), Math.ceil(e / 60)]
  }, [employees, schedules, workDays, day])

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

      {admin && employees.length > 0 && !editor && (
        <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 10 }}>
          Нажмите на имя мастера, чтобы задать его часы работы на выбранные дни.
        </div>
      )}

      {editor && (
        <div className="card" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
            <h3 style={{ margin: 0 }}>Часы работы: {employees.find(e => e.id === editor.empId)?.name}</h3>
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
              Неделя {week[0].getDate()} {MONTHS[week[0].getMonth()]} – {week[6].getDate()} {MONTHS[week[6].getMonth()]}
            </span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, minmax(0, 1fr))', gap: 6, marginBottom: 12 }}>
            {week.map(d => {
              const iso = isoDate(d)
              const on = editor.dates.includes(iso)
              const h = workHours(editor.empId, d)
              return (
                <button key={iso} type="button"
                  onClick={() => setEditor({ ...editor, dates: on ? editor.dates.filter(x => x !== iso) : [...editor.dates, iso] })}
                  style={{
                    padding: '6px 2px', borderRadius: 10, cursor: 'pointer', textAlign: 'center', fontSize: 12,
                    background: on ? 'var(--brand)' : 'transparent', color: on ? 'var(--card)' : 'var(--text)',
                    border: `1px solid ${on ? 'var(--brand)' : 'var(--border)'}`,
                  }}>
                  <div style={{ fontWeight: 600 }}>{DAY_NAMES[JS_DAYS[d.getDay()]]} {d.getDate()}</div>
                  <div style={{ fontSize: 11, opacity: .8 }}>{h ? `${hhmm(h[0])}–${hhmm(h[1])}` : offLabel(editor.empId, d) === 'Выходной' ? 'вых.' : '—'}</div>
                </button>
              )
            })}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap', marginBottom: 14 }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
              <input type="checkbox" checked={editor.working} onChange={e => setEditor({ ...editor, working: e.target.checked })} />
              Работает
            </label>
            {editor.working ? (
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                с
                <select className="input" style={{ width: 'auto' }} value={editor.start} onChange={e => setEditor({ ...editor, start: e.target.value })}>
                  {TIME_OPTIONS.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
                до
                <select className="input" style={{ width: 'auto' }} value={editor.end} onChange={e => setEditor({ ...editor, end: e.target.value })}>
                  {TIME_OPTIONS.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </span>
            ) : (
              <span style={{ color: 'var(--text-muted)' }}>Выходной — записаться к мастеру будет нельзя</span>
            )}
            {editor.working && (employees.find(e => e.id === editor.empId)?.branchIds.length ?? 0) > 1 && (
              <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                в филиале
                <select className="input" style={{ width: 'auto' }} value={editor.branchId ?? ''}
                  onChange={e => setEditor({ ...editor, branchId: e.target.value ? Number(e.target.value) : null })}>
                  <option value="">любом своём</option>
                  {employees.find(e => e.id === editor.empId)!.branchIds.map(id => (
                    <option key={id} value={id}>{branchName(id) ?? `#${id}`}</option>
                  ))}
                </select>
              </span>
            )}
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              повторять
              <select className="input" style={{ width: 'auto' }} value={editor.weeks} onChange={e => setEditor({ ...editor, weeks: Number(e.target.value) })}>
                {REPEAT_WEEKS.map(w => <option key={w} value={w}>{w === 1 ? 'только эту неделю' : `${w} нед. вперёд`}</option>)}
              </select>
            </span>
          </div>
          <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: -6, marginBottom: 12 }}>
            Дни из других недель тоже можно отметить: листайте недели стрелками вверху, выбор сохраняется.
          </div>

          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button className="btn btn-primary" disabled={savingDays || editor.dates.length === 0} onClick={() => saveEditor()}>
              Сохранить{editorDates(editor).length > 1 ? ` (${editorDates(editor).length} дн.)` : ''}
            </button>
            <button className="btn btn-secondary" disabled={savingDays || editor.dates.length === 0} onClick={() => saveEditor(true)}
              title="Убрать особые часы — день пойдёт по обычному графику">
              Как обычно
            </button>
            <button className="btn btn-secondary" disabled={savingDays} onClick={() => setEditor(null)}>Отмена</button>
          </div>
        </div>
      )}

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
              const special = !!override(emp.id, day)
              const selected = editor?.empId === emp.id
              return (
                <div key={emp.id}
                  onClick={admin ? () => openEditor(emp.id) : undefined}
                  title={admin ? 'Изменить часы работы' : undefined}
                  style={{
                    padding: '12px 10px', borderBottom: '1px solid var(--border)', borderLeft: '1px solid var(--border)',
                    cursor: admin ? 'pointer' : undefined, background: selected ? 'var(--brand-light)' : undefined,
                  }}>
                  <div style={{ fontWeight: 600, fontSize: 14, display: 'flex', alignItems: 'center', gap: 6 }}>
                    {emp.name}
                    {admin && <Pencil size={13} style={{ color: 'var(--text-faint)' }} />}
                  </div>
                  <div style={{ fontSize: 12, color: special ? 'var(--brand)' : 'var(--text-muted)' }}>
                    {h ? `${hhmm(h[0])}–${hhmm(h[1])}` : offLabel(emp.id)}
                    {count > 0 && <span style={{ color: 'var(--text-muted)' }}> · {count} зап.</span>}
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
