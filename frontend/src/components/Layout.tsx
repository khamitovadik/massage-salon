import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  Bell, CalendarClock, CalendarDays, ChartLine, ChevronDown, ChevronRight, ChevronsUpDown,
  CircleHelp, ContactRound, Home, LogOut, Menu, Send, SlidersHorizontal, Sparkles, Star,
  Ticket, Users, X, type LucideIcon,
} from 'lucide-react'
import { clearAuth, getAuth, isAdmin } from '../store/auth'
import type { Role } from '../types'
import Logo from './Logo'

interface NavItem { to: string; label: string; icon: LucideIcon; exact?: boolean; adminOnly?: boolean }

const NAV: NavItem[] = [
  { to: '/', label: 'Главная', icon: Home, exact: true },
  { to: '/appointments', label: 'Записи', icon: CalendarDays },
  { to: '/subscriptions', label: 'Абонементы', icon: Ticket },
  { to: '/services', label: 'Услуги', icon: Sparkles },
  { to: '/schedule', label: 'Расписание', icon: CalendarClock },
  { to: '/reviews', label: 'Отзывы', icon: Star },
  { to: '/employees', label: 'Сотрудники', icon: Users, adminOnly: true },
  { to: '/users', label: 'Пользователи', icon: ContactRound, adminOnly: true },
  { to: '/analytics', label: 'Аналитика', icon: ChartLine, adminOnly: true },
  { to: '/broadcast', label: 'Рассылка', icon: Send, adminOnly: true },
  { to: '/settings', label: 'Настройки', icon: SlidersHorizontal, adminOnly: true },
]

const ROLE_LABEL: Record<Role, string> = {
  OWNER: 'Владелец салона',
  ADMIN: 'Администратор',
  EMPLOYEE: 'Сотрудник',
  CLIENT: 'Клиент',
}

function initials(name?: string) {
  if (!name) return '?'
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0]).join('').toUpperCase()
}

export default function Layout() {
  const { user } = getAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)
  const [menu, setMenu] = useState<'top' | 'side' | null>(null)

  useEffect(() => { setOpen(false); setMenu(null) }, [pathname])

  useEffect(() => {
    if (!menu) return
    const close = (e: MouseEvent) => {
      if (!(e.target as Element).closest('.user-menu-anchor')) setMenu(null)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [menu])

  function logout() {
    clearAuth()
    navigate('/login')
  }

  const navItems = NAV.filter(n => !n.adminOnly || isAdmin(user?.role))
  const current = [...NAV].sort((a, b) => b.to.length - a.to.length)
    .find(n => n.exact ? pathname === n.to : pathname.startsWith(n.to))
  const roleLabel = user?.role ? ROLE_LABEL[user.role] : ''

  const dropdown = (
    <div className="user-menu">
      <div className="user-menu-head">
        <div className="user-menu-name">{user?.name}</div>
        <div className="user-menu-email">{user?.email}</div>
      </div>
      <button className="user-menu-item" onClick={logout}>
        <LogOut size={16} /> Выйти
      </button>
    </div>
  )

  return (
    <div className="app-shell">
      {open && <div className="sidebar-backdrop" onClick={() => setOpen(false)} />}

      <aside className={`sidebar${open ? ' is-open' : ''}`}>
        <div className="sidebar-brand">
          <Logo size={36} />
          <div>
            <div className="sidebar-brand-name">Em.Del.Relaxx</div>
            <div className="sidebar-brand-sub">Управление салоном</div>
          </div>
          <button className="icon-btn sidebar-close" onClick={() => setOpen(false)} aria-label="Закрыть меню">
            <X size={18} />
          </button>
        </div>

        <div className="sidebar-section">Рабочее пространство</div>
        <nav className="sidebar-nav">
          {navItems.map(({ to, label, icon: Icon, exact }) => (
            <NavLink key={to} to={to} end={exact} className="nav-item">
              <Icon size={17} strokeWidth={1.75} />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <button className="nav-item" onClick={() => toast('По вопросам работы системы обратитесь к администратору салона')}>
            <CircleHelp size={17} strokeWidth={1.75} />
            Помощь и поддержка
          </button>
          <div className="user-menu-anchor">
            <button className="sidebar-user" onClick={() => setMenu(m => m === 'side' ? null : 'side')}>
              <span className="avatar">{initials(user?.name)}</span>
              <span className="sidebar-user-text">
                <span className="sidebar-user-name">{user?.name}</span>
                <span className="sidebar-user-role">{roleLabel}</span>
              </span>
              <ChevronsUpDown size={15} color="var(--text-muted)" />
            </button>
            {menu === 'side' && <div className="user-menu-pos-up">{dropdown}</div>}
          </div>
        </div>
      </aside>

      <div className="main-col">
        <header className="topbar">
          <button className="icon-btn burger" onClick={() => setOpen(true)} aria-label="Открыть меню">
            <Menu size={20} />
          </button>
          <div className="breadcrumbs">
            <span>Управление салоном</span>
            <ChevronRight size={14} />
            <span className="breadcrumbs-current">{current?.label ?? 'Главная'}</span>
          </div>
          <div className="topbar-right">
            <button className="icon-btn" aria-label="Уведомления"><Bell size={18} strokeWidth={1.75} /></button>
            <span className="topbar-divider" />
            <div className="user-menu-anchor">
              <button className="topbar-user" onClick={() => setMenu(m => m === 'top' ? null : 'top')}>
                <span className="avatar">{initials(user?.name)}</span>
                <span className="topbar-user-name">{roleLabel || user?.name}</span>
                <ChevronDown size={15} color="var(--text-muted)" />
              </button>
              {menu === 'top' && <div className="user-menu-pos-down">{dropdown}</div>}
            </div>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
