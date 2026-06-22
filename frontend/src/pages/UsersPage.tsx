import { useEffect, useState } from 'react'
import api from '../api/client'

type Role = 'CLIENT' | 'EMPLOYEE' | 'ADMIN' | 'OWNER'

interface UserItem {
  id: number
  name: string
  email: string
  phone: string
  role: Role
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

  useEffect(() => {
    api.get('/users').then(r => setUsers(r.data)).finally(() => setLoading(false))
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

  if (loading) return <div className="card">Загрузка...</div>

  return (
    <div>
      <h1 style={{ marginBottom: 24 }}>👤 Пользователи</h1>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: 'var(--bg)', borderBottom: '1px solid var(--border)' }}>
              <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13 }}>Имя</th>
              <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13 }}>Email</th>
              <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13 }}>Телефон</th>
              <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: 13 }}>Роль</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id} style={{ borderBottom: '1px solid var(--border)' }}>
                <td style={{ padding: '12px 16px', fontSize: 14 }}>{u.name}</td>
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
              </tr>
            ))}
          </tbody>
        </table>
        {users.length === 0 && (
          <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)' }}>
            Нет пользователей
          </div>
        )}
      </div>
    </div>
  )
}
