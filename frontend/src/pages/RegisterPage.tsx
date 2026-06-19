import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { register } from '../api/auth'
import { saveAuth } from '../store/auth'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '' })
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    try {
      const data = await register(form)
      saveAuth(data)
      navigate('/')
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Ошибка регистрации')
    } finally {
      setLoading(false)
    }
  }

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: 'var(--bg)'
    }}>
      <div className="card" style={{ width: 380 }}>
        <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 8, color: 'var(--brand)' }}>
          Регистрация
        </h1>
        <p style={{ color: 'var(--text-muted)', marginBottom: 24 }}>Создайте аккаунт клиента</p>

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label className="label">Имя</label>
            <input className="input" value={form.name} onChange={set('name')} placeholder="Иван Иванов" required />
          </div>
          <div className="field">
            <label className="label">Email</label>
            <input className="input" type="email" value={form.email} onChange={set('email')} placeholder="ivan@example.com" required />
          </div>
          <div className="field">
            <label className="label">Телефон</label>
            <input className="input" value={form.phone} onChange={set('phone')} placeholder="+7 (999) 123-45-67" required />
          </div>
          <div className="field">
            <label className="label">Пароль</label>
            <input className="input" type="password" value={form.password} onChange={set('password')} placeholder="Минимум 6 символов" required minLength={6} />
          </div>
          <button className="btn btn-primary w-full" type="submit" disabled={loading}>
            {loading ? 'Создаём аккаунт...' : 'Зарегистрироваться'}
          </button>
        </form>

        <p style={{ marginTop: 16, textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
          Уже есть аккаунт? <Link to="/login">Войти</Link>
        </p>
      </div>
    </div>
  )
}
