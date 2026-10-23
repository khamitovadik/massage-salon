import { Link } from 'react-router-dom'
import { ArrowLeft, CalendarCheck, Leaf, Ticket } from 'lucide-react'
import Logo from './Logo'
import ThemeToggle from './ThemeToggle'

// Общая обёртка для входа и регистрации: фирменная панель слева, форма справа
export default function AuthLayout({ title, subtitle, children }: {
  title: string
  subtitle: string
  children: React.ReactNode
}) {
  return (
    <div className="auth">
      <aside className="auth-aside">
        <Link to="/welcome" className="auth-aside-brand"><Logo size={36} /> Em.Del.Relaxx</Link>
        <div style={{ position: 'relative', zIndex: 1 }}>
          <h2>Ваше время <em>для себя</em> — в пару кликов</h2>
          <ul>
            <li><CalendarCheck size={18} /> Онлайн-запись на свободные окошки</li>
            <li><Ticket size={18} /> Абонементы и история визитов</li>
            <li><Leaf size={18} /> Напоминания и забота о вас</li>
          </ul>
        </div>
        <span className="auth-aside-foot">© {new Date().getFullYear()} Em.Del.Relaxx</span>
      </aside>

      <main className="auth-main">
        <div className="auth-card">
          <div className="auth-top">
            <Link to="/welcome" className="auth-back"><ArrowLeft size={14} /> О салоне</Link>
            <span className="auth-top-right">
              <span className="auth-mobile-brand"><Logo size={32} /> Em.Del.Relaxx</span>
              <ThemeToggle />
            </span>
          </div>
          <h1>{title}</h1>
          <p className="auth-sub">{subtitle}</p>
          {children}
        </div>
      </main>
    </div>
  )
}
