import { useEffect, useState } from 'react'
import api from '../api/client'

type Role = 'CLIENT' | 'EMPLOYEE' | 'ADMIN' | 'OWNER'

interface UserItem {
  id: number
  name: string
  email: string
  phone: string
  role: Role
  active: boolean
}

const ROLES: Role[] = ['CLIENT', 'EMPLOYEE', 'ADMIN', 'OWNER']

const ROLE_LABELS: Record<Role, string> = {
  CLIENT: 'Клиент',
  EMPLOYEE: 'Сотрудник',
  ADMIN: 'Администратор',
  OWNER: 'Руководитель',
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserItem[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/users')
      .then(r => setUsers(r.data))
      .catch(e => setError(e?.response?.data?.error || 'Не удалось загрузить пользователей'))
      .finally(() => setLoading(false))
  }, [])

  async function changeRole(id: number, role: Role) {
    setSaving(id)
    try {
      await api.patch(`/users/${id}/role`, { role })
      setUsers(users.map(u => u.id === id ? { ...u, role } : u))
    } finally {
      setSaving(null)
    }
  }

  async function remove(u: UserItem) {
    if (!confirm(`Удалить ${u.name} (${u.email})?
Вместе с ним удалятся его записи, абонементы и отзывы. Это нельзя отменить.`)) return
    setSaving(u.id)
    try {
      await api.delete(`/users/${u.id}`)
      setUsers(users => users.filter(x => x.id !== u.id))
    } catch (e: any) {
      alert(e?.response?.data?.error || 'Не удалось удалить пользователя')
    } finally {
      setSaving(null)
    }
  }

  if (loading) return <div className="card">Загрузка...</div>

  return (
    <div>
      <h1 className="page-title" style={{ marginBottom: 24 }}>Пользователи</h1>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: 'var(--bg)', borderBottom: '1px solid var(--border)' }}>
              <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13 }}>Имя</th>
              <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13 }}>Email</th>
              <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13 }}>Телефон</th>
              <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13 }}>Роль</th>
              <th style={{ padding: '12px 16px' }} />
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{ padding: '12px 16px', fontSize: 14 }}>
                  {u.name}
                  {!u.active && <span style={{ marginLeft: 8, fontSize: 12, color: 'var(--text-muted)' }}>(деактивирован)</span>}
                </td>
                <td style={{ padding: '12px 16px', fontSize: 14, color: 'var(--text-muted)' }}>{u.email}</td>
                <td style={{ padding: '12px 16px', fontSize: 14, color: 'var(--text-muted)' }}>{u.phone || '—'}</td>
                <td style={{ padding: '12px 16px' }}>
                  <select
                    value={u.role}
                    disabled={saving === u.id}
                    onChange={e => changeRole(u.id, e.target.value as Role)}
                    style={{
                      padding: '4px 8px', borderRadius: 6, border: '1px solid var(--border)',
                      background: 'var(--bg)', fontSize: 13, cursor: 'pointer'
                    }}
                  >
                    {ROLES.map(r => (
                      <option key={r} value={r}>{ROLE_LABELS[r]}</option>
                    ))}
                  </select>
                </td>
                <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                  <button className="btn btn-sm btn-danger" title="Удалить пользователя" disabled={saving === u.id} onClick={() => remove(u)}>✕</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {users.length === 0 && (
          <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)' }}>
            {error || 'Нет пользователей'}
          </div>
        )}
      </div>
    </div>
  )
}
