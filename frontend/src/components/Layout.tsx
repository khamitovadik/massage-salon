import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { clearAuth, getAuth, isAdmin } from '../store/auth'

const NAV = [
  { to: '/', label: '🏠 Главная', exact: true },
  { to: '/appointments', label: '📅 Записи' },
  { to: '/subscriptions', label: '🎫 Абонементы' },
  { to: '/services', label: '💆 Услуги' },
  { to: '/schedule', label: '🗓️ Расписание' },
  { to: '/reviews', label: '⭐ Отзывы' },
  { to: '/employees', label: '👥 Сотрудники', adminOnly: true },
  { to: '/analytics', label: '📊 Аналитика', adminOnly: true },
  { to: '/broadcast', label: '📢 Рассылка', adminOnly: true },
  { to: '/settings', label: '⚙️ Настройки', adminOnly: true },
]

export default function Layout() {
  const { user } = getAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  function logout() {
    clearAuth()
    navigate('/login')
  }

  const navItems = NAV.filter(n => !n.adminOnly || isAdmin(user?.role))

  const navStyle = ({ isActive }: { isActive: boolean }) => ({
    display: 'block',
    padding: '12px 20px',
    color: isActive ? 'var(--brand)' : 'var(--text)',
    background: isActive ? 'var(--brand-light)' : 'transparent',
    fontWeight: isActive ? 600 : 400,
    borderRight: isActive ? '3px solid var(--brand)' : '3px solid transparent',
    textDecoration: 'none',
    fontSize: 14,
  })

  const sidebar = (
    <nav style={{
      width: 220, background: '#fff', borderRight: '1px solid var(--border)',
      display: 'flex', flexDirection: 'column', padding: '24px 0',
      height: '100%',
    }}>
      <div style={{ padding: '0 20px 24px', fontWeight: 700, fontSize: 16, color: 'var(--brand)' }}>
        💆 Салон массажа
      </div>

      {navItems.map(n => (
        <NavLink key={n.to} to={n.to} end={n.exact} style={navStyle} onClick={() => setOpen(false)}>
          {n.label}
        </NavLink>
      ))}

      <div style={{ marginTop: 'auto', padding: '16px 20px', borderTop: '1px solid var(--border)' }}>
        <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 4 }}>{user?.name}</div>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 12 }}>{user?.role}</div>
        <button className="btn btn-ghost btn-sm w-full" onClick={logout}>Выйти</button>
      </div>
    </nav>
  )

  return (
    <>
      <style>{`
        @media (max-width: 640px) {
          .desktop-nav { display: none !important; }
          .mobile-header { display: flex !important; }
          .mobile-overlay { display: block !important; }
        }
        @media (min-width: 641px) {
          .mobile-header { display: none !important; }
          .mobile-overlay { display: none !important; }
        }
      `}</style>

      {/* Мобильный хедер */}
      <div className="mobile-header" style={{
        display: 'none', alignItems: 'center', justifyContent: 'space-between',
        padding: '12px 16px', background: '#fff', borderBottom: '1px solid var(--border)',
        position: 'sticky', top: 0, zIndex: 100,
      }}>
        <span style={{ fontWeight: 700, color: 'var(--brand)', fontSize: 15 }}>💆 Салон массажа</span>
        <button onClick={() => setOpen(o => !o)} style={{
          background: 'none', border: 'none', fontSize: 22, cursor: 'pointer', padding: 4
        }}>
          {open ? '✕' : '☰'}
        </button>
      </div>

      {/* Мобильное меню — оверлей */}
      {open && (
        <div className="mobile-overlay" style={{
          display: 'none', position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, zIndex: 200
        }}>
          <div style={{ position: 'absolute', top: 0, left: 0, bottom: 0, width: 260, background: '#fff', boxShadow: '2px 0 12px rgba(0,0,0,0.15)' }}>
            {sidebar}
          </div>
          <div onClick={() => setOpen(false)} style={{ position: 'absolute', top: 0, left: 260, right: 0, bottom: 0, background: 'rgba(0,0,0,0.4)' }} />
        </div>
      )}

      <div style={{ display: 'flex', minHeight: '100vh' }}>
        {/* Десктопная боковая панель */}
        <div className="desktop-nav" style={{ flexShrink: 0 }}>
          {sidebar}
        </div>

        {/* Контент */}
        <main style={{ flex: 1, padding: '24px', overflowY: 'auto', minWidth: 0 }}>
          <Outlet />
        </main>
      </div>
    </>
  )
}
