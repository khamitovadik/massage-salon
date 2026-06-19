import { useEffect, useState } from 'react'
import api from '../api/client'

interface Employee { id: number; name: string }
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

export default function SchedulePage() {
  const [employees, setEmployees] = useState<Employee[]>([])
  const [schedules, setSchedules] = useState<Record<number, DaySchedule[]>>({})
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/employees').then(async r => {
      const emps: Employee[] = r.data.map((e: any) => ({
        id: e.id,
        name: e.name ?? `Сотрудник ${e.id}`
      }))
      setEmployees(emps)

      const results = await Promise.all(
        emps.map(e => api.get(`/schedule/employee/${e.id}`).then(r => ({ id: e.id, data: r.data })).catch(() => ({ id: e.id, data: [] })))
      )
      const map: Record<number, DaySchedule[]> = {}
      results.forEach(({ id, data }) => { map[id] = data })
      setSchedules(map)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-muted">Загрузка...</div>

  return (
    <div>
      <h1 style={{ marginBottom: 24 }}>🗓️ Расписание сотрудников</h1>

      {employees.length === 0 ? (
        <div className="text-muted">Сотрудники не найдены.</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {employees.map(emp => {
            const days = schedules[emp.id] ?? []
            return (
              <div key={emp.id} style={{
                background: '#fff', border: '1px solid var(--border)', borderRadius: 12, padding: 20
              }}>
                <h3 style={{ marginBottom: 12 }}>👤 {emp.name}</h3>
                {days.length === 0 ? (
                  <div className="text-muted" style={{ fontSize: 13 }}>Расписание не задано</div>
                ) : (
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {days.map((d, i) => (
                      <div key={i} style={{
                        padding: '8px 14px', borderRadius: 10, textAlign: 'center',
                        background: d.working ? 'var(--brand-light)' : '#f3f4f6',
                        border: `1px solid ${d.working ? 'var(--brand)' : 'var(--border)'}`,
                        minWidth: 60
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
