import { useEffect, useMemo, useState, type ReactNode } from 'react'
import api from '../api/client'

interface Kpi { revenue: number; total: number; completed: number; cancelled: number; cancelRate: number; avgCheck: number; clients: number }
interface Day { date: string; count: number; revenue: number }
interface Weekday { name: string; total: number; cancelled: number }
interface ServiceRow { name: string; count: number; revenue: number; share: number; cancelled: number }
interface EmployeeRow { id: number; name: string; specialization: string | null; total: number; completed: number; cancelled: number; revenue: number; avgRating: number | null; reviewCount: number }
interface ClientRow { name: string; phone: string | null; visits: number; spent: number; lastVisit: string; daysSince: number }
interface Insight { level: 'good' | 'warn' | 'info'; title: string; text: string }
interface Insights {
  current: Kpi; previous: Kpi; byDay: Day[]; heatmap: number[][]; weekdays: Weekday[]
  services: ServiceRow[]; employees: EmployeeRow[]
  clients: { newClients: number; returningClients: number; repeatRate: number; top: ClientRow[]; lapsed: ClientRow[] }
  reviews: { average: number; count: number; distribution: number[]; reviewRate: number }
  subscriptions: { active: number; expiringSoon: number; visitsBySubscription: number; subscriptionShare: number }
  avgLeadDays: number
  insights: Insight[]
}

const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']
const LEVEL = {
  good: { color: 'var(--success)', icon: '✓' },
  warn: { color: 'var(--danger)', icon: '!' },
  info: { color: 'var(--warning)', icon: 'i' },
}

const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const money = (n: number) => `${Math.round(n).toLocaleString('ru-RU')} ₸`
const pct = (v: number) => `${Math.round(v * 100)}%`

type PeriodKey = 'month' | 'prev' | '30' | '90'
const PERIODS: { key: PeriodKey; label: string }[] = [
  { key: 'month', label: 'Этот месяц' },
  { key: 'prev', label: 'Прошлый месяц' },
  { key: '30', label: '30 дней' },
  { key: '90', label: '90 дней' },
]

function range(key: PeriodKey): { from: string; to: string } {
  const now = new Date()
  if (key === 'month') return { from: iso(new Date(now.getFullYear(), now.getMonth(), 1)), to: iso(new Date(now.getFullYear(), now.getMonth() + 1, 0)) }
  if (key === 'prev') return { from: iso(new Date(now.getFullYear(), now.getMonth() - 1, 1)), to: iso(new Date(now.getFullYear(), now.getMonth(), 0)) }
  const days = Number(key)
  return { from: iso(new Date(now.getFullYear(), now.getMonth(), now.getDate() - days + 1)), to: iso(now) }
}

export default function AnalyticsPage() {
  const [period, setPeriod] = useState<PeriodKey>('month')
  const [data, setData] = useState<Insights | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    api.get<Insights>('/analytics/insights', { params: range(period) })
      .then(r => setData(r.data))
      .catch(() => setError('Не удалось загрузить аналитику'))
      .finally(() => setLoading(false))
  }, [period])

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 24 }}>
        <h1 className="page-title">Аналитика</h1>
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {PERIODS.map(p => (
            <button key={p.key} className={`btn btn-sm ${period === p.key ? 'btn-primary' : ''}`} onClick={() => setPeriod(p.key)}>
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {error && <div style={{ color: 'var(--danger)' }}>{error}</div>}
      {loading && !data && <div className="text-muted">Загрузка...</div>}
      {data && <Dashboard d={data} dim={loading} />}
    </div>
  )
}

function Dashboard({ d, dim }: { d: Insights; dim: boolean }) {
  return (
    <div style={{ opacity: dim ? 0.5 : 1, transition: 'opacity .15s', display: 'flex', flexDirection: 'column', gap: 24 }}>
      <InsightsList items={d.insights} />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(190px, 100%), 1fr))', gap: 14 }}>
        <KpiCard label="Выручка" value={money(d.current.revenue)} now={d.current.revenue} before={d.previous.revenue} />
        <KpiCard label="Завершено сеансов" value={String(d.current.completed)} now={d.current.completed} before={d.previous.completed} />
        <KpiCard label="Средний чек" value={money(d.current.avgCheck)} now={d.current.avgCheck} before={d.previous.avgCheck} />
        <KpiCard label="Отмены" value={pct(d.current.cancelRate)} now={d.current.cancelRate} before={d.previous.cancelRate} invert />
        <KpiCard label="Клиентов" value={String(d.current.clients)} now={d.current.clients} before={d.previous.clients} />
      </div>

      <Card title="Выручка по дням" hint="Столбики — выручка завершённых сеансов, точки — число записей">
        <DayChart days={d.byDay} />
      </Card>

      <Card title="Когда загружен салон" hint="Записи по дням недели и часам: чем насыщеннее клетка, тем больше записей; пустые клетки — свободные окна для акций">
        <Heatmap heat={d.heatmap} />
        <WeekdayCancels weekdays={d.weekdays} />
      </Card>

      <div style={grid2}>
        <Card title="Услуги по выручке">
          <ServiceBars rows={d.services} />
        </Card>
        <Card title="Клиенты" hint="Новые — первый завершённый визит попал в период">
          <ClientsBlock c={d.clients} />
        </Card>
      </div>

      <Card title="Мастера" hint="Выручка, отмены и оценка клиентов за период">
        <EmployeesTable rows={d.employees} />
      </Card>

      <div style={grid2}>
        <Card title="Оценки клиентов">
          <RatingBlock r={d.reviews} />
        </Card>
        <Card title="Абонементы и запись">
          <SubsBlock s={d.subscriptions} lead={d.avgLeadDays} />
        </Card>
      </div>

      <Card title="Кого вернуть" hint="Были 2+ раза, но давно не приходили — готовый список для рассылки">
        <ClientTable rows={d.clients.lapsed} empty="Уснувших постоянных клиентов нет — отлично." showSince />
      </Card>
    </div>
  )
}

const grid2: React.CSSProperties = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(360px, 100%), 1fr))', gap: 24 }

// ------------------------------------------------------------------ общие блоки

function Card({ title, hint, children }: { title: string; hint?: string; children: ReactNode }) {
  return (
    <section style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: 20, minWidth: 0 }}>
      <h3 style={{ marginBottom: hint ? 4 : 14 }}>{title}</h3>
      {hint && <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 14 }}>{hint}</div>}
      {children}
    </section>
  )
}

function InsightsList({ items }: { items: Insight[] }) {
  if (items.length === 0) return null
  return (
    <Card title="Выводы" hint="Считаются автоматически по данным за выбранный период">
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {items.map((i, idx) => (
          <div key={idx} style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
            <span style={{
              flexShrink: 0, width: 22, height: 22, borderRadius: '50%', background: LEVEL[i.level].color, color: '#fff',
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700,
            }}>{LEVEL[i.level].icon}</span>
            <div>
              <div style={{ fontWeight: 600 }}>{i.title}</div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>{i.text}</div>
            </div>
          </div>
        ))}
      </div>
    </Card>
  )
}

function KpiCard({ label, value, now, before, invert }: { label: string; value: string; now: number; before: number; invert?: boolean }) {
  const delta = before > 0 ? ((now - before) / before) * 100 : null
  // для отмен рост — это плохо
  const good = delta === null ? null : invert ? delta < 0 : delta > 0
  return (
    <div style={{ background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 18px' }}>
      <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 6 }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: 700 }}>{value}</div>
      <div style={{ fontSize: 12, marginTop: 4, color: delta === null || Math.abs(delta) < 1 ? 'var(--text-faint)' : good ? 'var(--success)' : 'var(--danger)' }}>
        {delta === null ? 'нет данных за прошлый период' : Math.abs(delta) < 1 ? 'как в прошлый период' : `${delta > 0 ? '▲' : '▼'} ${Math.abs(Math.round(delta))}% к прошлому периоду`}
      </div>
    </div>
  )
}

// ------------------------------------------------------------------ графики

function DayChart({ days }: { days: Day[] }) {
  const [hover, setHover] = useState<number | null>(null)
  const W = 800, H = 200, padL = 8, padB = 22
  const maxRev = Math.max(1, ...days.map(x => x.revenue))
  const maxCnt = Math.max(1, ...days.map(x => x.count))
  const step = (W - padL) / days.length
  const bw = Math.max(2, step * 0.65)
  const labelEvery = Math.ceil(days.length / 10)
  const h = hover !== null ? days[hover] : null

  return (
    <div>
      <div style={{ minHeight: 20, fontSize: 13, marginBottom: 6, color: 'var(--text-muted)' }}>
        {h ? <><strong style={{ color: 'var(--text)' }}>{new Date(h.date).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' })}</strong> · {money(h.revenue)} · записей: {h.count}</> : 'Наведите на день, чтобы увидеть детали'}
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto', display: 'block' }} role="img" aria-label="Выручка по дням">
        {days.map((x, i) => {
          const bh = (x.revenue / maxRev) * (H - padB - 10)
          const cx = padL + i * step + step / 2
          const cy = H - padB - (x.count / maxCnt) * (H - padB - 10)
          return (
            <g key={x.date} onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}>
              <rect x={padL + i * step} y={0} width={step} height={H - padB} fill="transparent" />
              <rect x={cx - bw / 2} y={H - padB - bh} width={bw} height={bh} rx={2}
                fill="var(--brand)" opacity={hover === null || hover === i ? 0.9 : 0.4} />
              {x.count > 0 && <circle cx={cx} cy={cy} r={3} fill="var(--warning)" />}
              {i % labelEvery === 0 && (
                <text x={cx} y={H - 6} textAnchor="middle" fontSize={11} fill="var(--text-faint)">{Number(x.date.slice(8))}</text>
              )}
            </g>
          )
        })}
        <line x1={0} x2={W} y1={H - padB} y2={H - padB} stroke="var(--border-strong)" />
      </svg>
    </div>
  )
}

function Heatmap({ heat }: { heat: number[][] }) {
  // показываем только часы, где вообще есть записи
  const hours = useMemo(() => {
    const used = Array.from({ length: 24 }, (_, h) => h).filter(h => heat.some(row => row[h] > 0))
    if (used.length === 0) return []
    const min = used[0], max = used[used.length - 1]
    return Array.from({ length: max - min + 1 }, (_, i) => min + i)
  }, [heat])
  const max = Math.max(1, ...heat.flat())

  if (hours.length === 0) return <div className="text-muted">Нет записей за период.</div>

  return (
    <div style={{ overflowX: 'auto' }}>
      <div style={{ display: 'grid', gridTemplateColumns: `34px repeat(${hours.length}, minmax(28px, 1fr))`, gap: 3, minWidth: 34 + hours.length * 31 }}>
        <div />
        {hours.map(h => <div key={h} style={{ fontSize: 11, textAlign: 'center', color: 'var(--text-faint)' }}>{h}</div>)}
        {WEEKDAYS.map((name, d) => (
          <Row key={name} name={name} cells={hours.map(h => heat[d][h])} max={max} hours={hours} />
        ))}
      </div>
    </div>
  )
}

function Row({ name, cells, max, hours }: { name: string; cells: number[]; max: number; hours: number[] }) {
  return (
    <>
      <div style={{ fontSize: 12, color: 'var(--text-muted)', alignSelf: 'center' }}>{name}</div>
      {cells.map((v, i) => (
        <div key={i} title={`${name}, ${hours[i]}:00 — записей: ${v}`}
          style={{
            height: 26, borderRadius: 4, fontSize: 11, display: 'flex', alignItems: 'center', justifyContent: 'center',
            // фиксированный оттенок: чем насыщеннее клетка, тем больше записей — в любой теме
            background: v === 0 ? 'var(--field)' : `rgba(133, 101, 79, ${0.2 + 0.8 * (v / max)})`,
            color: v / max > 0.5 ? '#fff' : 'var(--text)', border: v === 0 ? '1px solid var(--border)' : 'none',
          }}>{v || ''}</div>
      ))}
    </>
  )
}

function WeekdayCancels({ weekdays }: { weekdays: Weekday[] }) {
  if (!weekdays.some(w => w.cancelled > 0)) return null
  return (
    <div style={{ marginTop: 16, fontSize: 12, color: 'var(--text-muted)' }}>
      <div style={{ marginBottom: 6, fontWeight: 600 }}>Отмены по дням недели</div>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {weekdays.map(w => (
          <span key={w.name} style={{ padding: '3px 8px', borderRadius: 6, background: 'var(--field)', border: '1px solid var(--border)' }}>
            {w.name}: {w.total ? pct(w.cancelled / w.total) : '—'}
          </span>
        ))}
      </div>
    </div>
  )
}

function ServiceBars({ rows }: { rows: ServiceRow[] }) {
  if (rows.length === 0) return <div className="text-muted">Нет завершённых сеансов.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {rows.map(r => (
        <div key={r.name}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, fontSize: 13, marginBottom: 4 }}>
            <span>{r.name}</span>
            <span style={{ color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>{money(r.revenue)} · {r.count} шт · {pct(r.share)}</span>
          </div>
          <div style={{ height: 8, background: 'var(--border)', borderRadius: 4, overflow: 'hidden' }}>
            <div style={{ width: `${r.share * 100}%`, height: '100%', background: 'var(--brand)' }} />
          </div>
        </div>
      ))}
    </div>
  )
}

function ClientsBlock({ c }: { c: Insights['clients'] }) {
  const total = c.newClients + c.returningClients
  return (
    <div>
      {total === 0 ? <div className="text-muted">Нет завершённых визитов.</div> : (
        <>
          <div style={{ display: 'flex', height: 14, borderRadius: 7, overflow: 'hidden', marginBottom: 8 }}>
            <div style={{ flex: c.returningClients, background: 'var(--brand)' }} title="Вернувшиеся" />
            <div style={{ flex: c.newClients, background: 'var(--warning)' }} title="Новые" />
          </div>
          <div style={{ display: 'flex', gap: 16, fontSize: 13, marginBottom: 16, flexWrap: 'wrap' }}>
            <span><Dot color="var(--brand)" />Вернулись: <strong>{c.returningClients}</strong> ({pct(c.repeatRate)})</span>
            <span><Dot color="var(--warning)" />Новые: <strong>{c.newClients}</strong></span>
          </div>
        </>
      )}
      <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 6 }}>Лучшие клиенты за всё время</div>
      <ClientTable rows={c.top} empty="Пока нет данных." />
    </div>
  )
}

function Dot({ color }: { color: string }) {
  return <span style={{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', background: color, marginRight: 6 }} />
}

function ClientTable({ rows, empty, showSince }: { rows: ClientRow[]; empty: string; showSince?: boolean }) {
  if (rows.length === 0) return <div className="text-muted" style={{ fontSize: 13 }}>{empty}</div>
  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', fontSize: 13, borderCollapse: 'collapse' }}>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} style={{ borderTop: i ? '1px solid var(--border)' : 'none' }}>
              <td style={{ padding: '6px 0' }}>{r.name}{r.phone && <div style={{ fontSize: 11, color: 'var(--text-faint)' }}>{r.phone}</div>}</td>
              <td style={{ textAlign: 'right', color: 'var(--text-muted)' }}>{r.visits} виз.</td>
              <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>{money(r.spent)}</td>
              {showSince && <td style={{ textAlign: 'right', color: 'var(--danger)', whiteSpace: 'nowrap' }}>{r.daysSince} дн. назад</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function EmployeesTable({ rows }: { rows: EmployeeRow[] }) {
  if (rows.length === 0) return <div className="text-muted">Нет записей за период.</div>
  const max = Math.max(1, ...rows.map(r => r.revenue))
  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', fontSize: 13, borderCollapse: 'collapse', minWidth: 520 }}>
        <thead>
          <tr style={{ textAlign: 'left', color: 'var(--text-muted)' }}>
            <th style={{ padding: '4px 0' }}>Мастер</th><th>Выручка</th><th>Сеансов</th><th>Отмен</th><th>Оценка</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(r => (
            <tr key={r.id} style={{ borderTop: '1px solid var(--border)' }}>
              <td style={{ padding: '8px 0' }}>{r.name}{r.specialization && <div style={{ fontSize: 11, color: 'var(--text-faint)' }}>{r.specialization}</div>}</td>
              <td style={{ minWidth: 160 }}>
                <div style={{ height: 6, background: 'var(--border)', borderRadius: 3, overflow: 'hidden', marginBottom: 3 }}>
                  <div style={{ width: `${(r.revenue / max) * 100}%`, height: '100%', background: 'var(--brand)' }} />
                </div>
                {money(r.revenue)}
              </td>
              <td>{r.completed}</td>
              <td style={{ color: r.total && r.cancelled / r.total > 0.15 ? 'var(--danger)' : undefined }}>
                {r.cancelled}{r.total ? ` (${pct(r.cancelled / r.total)})` : ''}
              </td>
              <td>{r.avgRating !== null ? <>★ {r.avgRating.toFixed(1)} <span style={{ color: 'var(--text-faint)' }}>({r.reviewCount})</span></> : <span style={{ color: 'var(--text-faint)' }}>нет</span>}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function RatingBlock({ r }: { r: Insights['reviews'] }) {
  if (r.count === 0) return <div className="text-muted">За период нет отзывов.</div>
  const max = Math.max(1, ...r.distribution)
  return (
    <div style={{ display: 'flex', gap: 24, alignItems: 'center', flexWrap: 'wrap' }}>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 40, fontWeight: 700, lineHeight: 1 }}>{r.average.toFixed(1)}</div>
        <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>{r.count} отзывов</div>
        <div style={{ fontSize: 12, color: 'var(--text-faint)' }}>оставляют {pct(r.reviewRate)} гостей</div>
      </div>
      <div style={{ flex: 1, minWidth: 180 }}>
        {[5, 4, 3, 2, 1].map(n => (
          <div key={n} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, marginBottom: 3 }}>
            <span style={{ width: 24 }}>{n} ★</span>
            <div style={{ flex: 1, height: 8, background: 'var(--border)', borderRadius: 4, overflow: 'hidden' }}>
              <div style={{ width: `${(r.distribution[n - 1] / max) * 100}%`, height: '100%', background: n <= 3 ? 'var(--danger)' : 'var(--success)' }} />
            </div>
            <span style={{ width: 20, textAlign: 'right', color: 'var(--text-muted)' }}>{r.distribution[n - 1]}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function SubsBlock({ s, lead }: { s: Insights['subscriptions']; lead: number }) {
  const items = [
    { label: 'Активных абонементов', value: String(s.active) },
    { label: 'Истекают за 14 дней', value: String(s.expiringSoon), warn: s.expiringSoon > 0 },
    { label: 'Визитов по абонементу', value: pct(s.subscriptionShare) },
    { label: 'Записываются за', value: lead > 0 ? `${lead} дн.` : '—' },
  ]
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
      {items.map(i => (
        <div key={i.label} style={{ background: 'var(--field)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 14px' }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: i.warn ? 'var(--danger)' : undefined }}>{i.value}</div>
          <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{i.label}</div>
        </div>
      ))}
    </div>
  )
}
