import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { getEmployees, createEmployee, deactivateEmployee } from '../api/employees'
import type { Employee } from '../types'

export default function EmployeesPage() {
  const [list, setList] = useState<Employee[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    name: '', email: '', phone: '', password: '', specialization: '', description: ''
  })

  useEffect(() => {
    getEmployees().then(setList).finally(() => setLoading(false))
  }, [])

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    try {
      const emp = await createEmployee({
        name: form.name,
        email: form.email,
        phone: form.phone,
        password: form.password,
        specialization: form.specialization,
        description: form.description || undefined,
      })
      setList(l => [...l, emp])
      setForm({ name: '', email: '', phone: '', password: '', specialization: '', description: '' })
      setShowForm(false)
      toast.success('Сотрудник добавлен')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    } finally {
      setSaving(false)
    }
  }

  async function handleDeactivate(id: number) {
    if (!confirm('Деактивировать сотрудника?')) return
    try {
      await deactivateEmployee(id)
      setList(l => l.map(e => e.id === id ? { ...e, active: false } : e))
      toast.success('Сотрудник деактивирован')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка')
    }
  }

  if (loading) return <div className="spinner" />

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">👥 Сотрудники</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(v => !v)}>
          {showForm ? 'Отмена' : '+ Добавить сотрудника'}
        </button>
      </div>

      {showForm && (
        <div className="card mb-4">
          <h2 style={{ fontWeight: 600, marginBottom: 16 }}>Новый сотрудник</h2>
          <form onSubmit={handleCreate}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="field">
                <label className="label">Имя</label>
                <input className="input" value={form.name} onChange={set('name')} required />
              </div>
              <div className="field">
                <label className="label">Email</label>
                <input className="input" type="email" value={form.email} onChange={set('email')} required />
              </div>
              <div className="field">
                <label className="label">Телефон</label>
                <input className="input" value={form.phone} onChange={set('phone')} required />
              </div>
              <div className="field">
                <label className="label">Пароль</label>
                <input className="input" type="password" value={form.password} onChange={set('password')} required minLength={6} />
              </div>
              <div className="field">
                <label className="label">Специализация</label>
                <input className="input" value={form.specialization} onChange={set('specialization')} placeholder="Классический массаж" required />
              </div>
              <div className="field">
                <label className="label">Описание</label>
                <input className="input" value={form.description} onChange={set('description')} />
              </div>
            </div>
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? 'Сохранение...' : 'Сохранить'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {list.length === 0 ? (
          <div className="empty-state">Сотрудников пока нет</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Имя</th>
                  <th>Email</th>
                  <th>Телефон</th>
                  <th>Специализация</th>
                  <th>Статус</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {list.map(em => (
                  <tr key={em.id}>
                    <td style={{ fontWeight: 500 }}>{em.name}</td>
                    <td>{em.email}</td>
                    <td>{em.phone}</td>
                    <td>{em.specialization}</td>
                    <td>
                      <span className={`badge ${em.active ? 'badge-green' : 'badge-gray'}`}>
                        {em.active ? 'Активен' : 'Неактивен'}
                      </span>
                    </td>
                    <td>
                      {em.active && (
                        <button className="btn btn-sm btn-danger" onClick={() => handleDeactivate(em.id)}>
                          Деактивировать
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
