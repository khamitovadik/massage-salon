import { useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import { MapPin, Phone, Clock, UserCog } from 'lucide-react'
import {
  closeBranch, createBranch, getAllUsers, getManagedBranches, setBranchManagers, updateBranch,
  type BranchInput,
} from '../api/branches'
import { getAllEmployees } from '../api/employees'
import { getAuth } from '../store/auth'
import { invalidateBranches } from '../store/useBranches'
import type { Branch, Employee, User } from '../types'

const EMPTY: BranchInput = { name: '', address: '', phone: '', mapUrl: '', workingHours: 'Ежедневно 11:00–23:00' }

export default function BranchesPage() {
  const { user } = getAuth()
  const owner = user?.role === 'OWNER'

  const [list, setList] = useState<Branch[]>([])
  const [employees, setEmployees] = useState<Employee[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  // null — форма закрыта, 0 — новый филиал, id — редактирование
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<BranchInput>(EMPTY)
  const [saving, setSaving] = useState(false)
  const [managersFor, setManagersFor] = useState<number | null>(null)
  const [pickedManagers, setPickedManagers] = useState<number[]>([])
  const [managerQuery, setManagerQuery] = useState('')

  useEffect(() => {
    Promise.all([
      getManagedBranches(),
      getAllEmployees().catch(() => []),
      owner ? getAllUsers().catch(() => []) : Promise.resolve([]),
    ]).then(([b, e, u]) => { setList(b); setEmployees(e); setUsers(u) })
      .finally(() => setLoading(false))
  }, [])

  const staffCount = useMemo(() => {
    const m = new Map<number, number>()
    employees.filter(e => e.active).forEach(e => e.branchIds?.forEach(id => m.set(id, (m.get(id) ?? 0) + 1)))
    return m
  }, [employees])

  function replace(b: Branch) {
    invalidateBranches()
    setList(l => l.some(x => x.id === b.id) ? l.map(x => x.id === b.id ? b : x) : [...l, b])
  }

  function openForm(b?: Branch) {
    setManagersFor(null)
    setEditId(b ? b.id : 0)
    setForm(b ? { name: b.name, address: b.address ?? '', phone: b.phone ?? '', mapUrl: b.mapUrl ?? '', workingHours: b.workingHours ?? '' } : EMPTY)
  }

  async function save(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const saved = editId ? await updateBranch(editId, form) : await createBranch(form)
      replace(saved)
      setEditId(null)
      toast.success(editId ? 'Филиал сохранён' : 'Новая точка открыта')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    } finally {
      setSaving(false)
    }
  }

  async function toggleActive(b: Branch) {
    if (b.active && !confirm(`Закрыть «${b.name}»? Записи и история сохранятся, но на сайте и в записи точка пропадёт.`)) return
    try {
      if (b.active) {
        await closeBranch(b.id)
        replace({ ...b, active: false })
        toast.success('Филиал закрыт')
      } else {
        replace(await updateBranch(b.id, { name: b.name, active: true }))
        toast.success('Филиал снова работает')
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  function openManagers(b: Branch) {
    setEditId(null)
    setManagersFor(b.id)
    setPickedManagers(b.managers.map(m => m.id))
    setManagerQuery('')
  }

  async function saveManagers(b: Branch) {
    try {
      replace(await setBranchManagers(b.id, pickedManagers))
      setManagersFor(null)
      toast.success('Руководители назначены')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  // Руководителем можно назначить любого, кроме владельца; клиент или мастер получит роль руководителя
  const candidates = users
    .filter(u => u.role !== 'OWNER')
    .filter(u => !managerQuery || `${u.name} ${u.email} ${u.phone}`.toLowerCase().includes(managerQuery.toLowerCase()))
    .sort((a, b) => Number(pickedManagers.includes(b.id)) - Number(pickedManagers.includes(a.id)) || a.name.localeCompare(b.name))
    .slice(0, 30)

  if (loading) return <div className="spinner" />

  const form_ = editId !== null && (
    <div className="card mb-4">
      <h2 style={{ fontWeight: 600, marginBottom: 16 }}>{editId ? 'Изменить филиал' : 'Новая точка'}</h2>
      <form onSubmit={save}>
        <div className="field-row">
          <div className="field">
            <label className="label">Название</label>
            <input className="input" value={form.name ?? ''} required placeholder="Например, «На Абая»"
              onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="field">
            <label className="label">Адрес</label>
            <input className="input" value={form.address ?? ''} placeholder="Город, улица, дом"
              onChange={e => setForm(f => ({ ...f, address: e.target.value }))} />
          </div>
          <div className="field">
            <label className="label">Телефон</label>
            <input className="input" value={form.phone ?? ''} placeholder="+7 7__ ___ __ __"
              onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
          </div>
          <div className="field">
            <label className="label">Часы работы</label>
            <input className="input" value={form.workingHours ?? ''}
              onChange={e => setForm(f => ({ ...f, workingHours: e.target.value }))} />
          </div>
          <div className="field">
            <label className="label">Ссылка на 2ГИС или карты</label>
            <input className="input" value={form.mapUrl ?? ''} placeholder="https://2gis.kz/..."
              onChange={e => setForm(f => ({ ...f, mapUrl: e.target.value }))} />
          </div>
        </div>
        <div className="flex gap-3">
          <button className="btn btn-primary" type="submit" disabled={saving}>{saving ? 'Сохранение...' : 'Сохранить'}</button>
          <button className="btn btn-ghost" type="button" onClick={() => setEditId(null)}>Отмена</button>
        </div>
      </form>
    </div>
  )

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Филиалы</h1>
          <p className="page-subtitle">
            {owner
              ? 'Открывайте новые точки, назначайте руководителей. Мастеров к филиалам привязывают в разделе «Сотрудники».'
              : 'Филиалы, которыми вы руководите.'}
          </p>
        </div>
        {owner && editId === null && (
          <button className="btn btn-primary" onClick={() => openForm()}>+ Новая точка</button>
        )}
      </div>

      {form_}

      {list.length === 0 ? (
        <div className="card"><div className="empty-state">
          {owner ? 'Филиалов пока нет' : 'Вас ещё не назначили руководителем филиала — обратитесь к владельцу'}
        </div></div>
      ) : (
        <div className="branch-grid">
          {list.map(b => (
            <div key={b.id} className={`card branch-card${b.active ? '' : ' is-closed'}`}>
              <div className="branch-card-head">
                <div className="branch-card-title">{b.name}</div>
                <span className={`badge ${b.active ? 'badge-green' : 'badge-gray'}`}>{b.active ? 'Работает' : 'Закрыт'}</span>
              </div>
              <div className="branch-meta">
                {b.address && <span className="flex items-center gap-2"><MapPin size={14} />{b.mapUrl
                  ? <a href={b.mapUrl} target="_blank" rel="noreferrer">{b.address}</a> : b.address}</span>}
                {b.phone && <span className="flex items-center gap-2"><Phone size={14} />{b.phone}</span>}
                {b.workingHours && <span className="flex items-center gap-2"><Clock size={14} />{b.workingHours}</span>}
                <span className="flex items-center gap-2"><UserCog size={14} />
                  {b.managers.length ? b.managers.map(m => m.name).join(', ') : 'Руководитель не назначен'}
                </span>
                <span>Мастеров: {staffCount.get(b.id) ?? 0}</span>
              </div>

              {owner && managersFor === b.id && (
                <div>
                  <input className="input" placeholder="Поиск по имени, email, телефону" value={managerQuery}
                    onChange={e => setManagerQuery(e.target.value)} style={{ marginBottom: 8 }} />
                  <div style={{ maxHeight: 220, overflowY: 'auto', display: 'grid', gap: 4 }}>
                    {candidates.map(u => (
                      <label key={u.id} className="flex items-center gap-2 text-sm" style={{ cursor: 'pointer' }}>
                        <input type="checkbox" checked={pickedManagers.includes(u.id)}
                          onChange={() => setPickedManagers(p => p.includes(u.id) ? p.filter(x => x !== u.id) : [...p, u.id])} />
                        <span>{u.name} <span className="text-muted">· {u.email}</span></span>
                      </label>
                    ))}
                    {candidates.length === 0 && <span className="text-muted text-sm">Никого не нашли</span>}
                  </div>
                  <p className="text-muted text-sm mt-2">Клиент или мастер станет руководителем филиала (карточка мастера сохранится).</p>
                  <div className="flex gap-2 mt-2">
                    <button className="btn btn-sm btn-primary" onClick={() => saveManagers(b)}>Сохранить</button>
                    <button className="btn btn-sm btn-ghost" onClick={() => setManagersFor(null)}>Отмена</button>
                  </div>
                </div>
              )}

              {owner && managersFor !== b.id && (
                <div className="flex gap-2" style={{ flexWrap: 'wrap', marginTop: 'auto' }}>
                  <button className="btn btn-sm btn-ghost" onClick={() => openForm(b)}>Изменить</button>
                  <button className="btn btn-sm btn-ghost" onClick={() => openManagers(b)}>Руководители</button>
                  <button className={`btn btn-sm ${b.active ? 'btn-danger' : 'btn-ghost'}`} onClick={() => toggleActive(b)}>
                    {b.active ? 'Закрыть' : 'Открыть снова'}
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
